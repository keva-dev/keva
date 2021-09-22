package sentinel

import (
	"fmt"
	"net"
	"strings"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/spf13/viper"
	kevago "github.com/tuhuynh27/keva/go-client"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

func newLogger() *zap.SugaredLogger {
	config := zap.NewDevelopmentConfig()
	config.EncoderConfig.EncodeLevel = zapcore.CapitalColorLevelEncoder
	logger, err := config.Build()
	if err != nil {
		panic(err)
	}
	return logger.Sugar()
}

type Config struct {
	MyID         string          `mapstructure:"my_id"`
	Binds        []string        `mapstructure:"binds"` //TODO: understand how to bind multiple network interfaces
	Port         string          `mapstructure:"port"`
	Masters      []MasterMonitor `mapstructure:"masters"`
	CurrentEpoch int             `mapstructure:"current_epoch"`
}

type MasterMonitor struct {
	Name            string          `mapstructure:"name"`
	Addr            string          `mapstructure:"addr"`
	Quorum          int             `mapstructure:"quorum"`
	DownAfter       time.Duration   `mapstructure:"down_after"`
	FailoverTimeout time.Duration   `mapstructure:"failover_timeout"`
	ConfigEpoch     int             `mapstructure:"config_epoch"` //epoch of master received from hello message
	LeaderEpoch     int             `mapstructure:"config_epoch"` //last leader epoch
	KnownReplicas   []KnownReplica  `mapstructure:"known_replicas"`
	KnownSentinels  []KnownSentinel `mapstructure:"known_sentinels`
}
type KnownSentinel struct {
	ID   string
	Addr string
}

type KnownReplica struct {
	Addr string
}

type Sentinel struct {
	mu              *sync.Mutex
	quorum          int
	conf            Config
	masterInstances map[string]*masterInstance //key by address ip:port
	currentEpoch    int
	runID           string

	//given a preassigned slaveInstance struct, assign missing fields to make it complete
	// - create client from given address, for example
	slaveFactory func(*slaveInstance) error

	clientFactory func(string) (internalClient, error)
	listener      net.Listener
	logger        *zap.SugaredLogger
}

func defaultSlaveFactory(sl *slaveInstance) error {
	client, err := newInternalClient(sl.addr)
	if err != nil {
		return err
	}
	sl.client = client
	return nil
}

func newInternalClient(addr string) (internalClient, error) {
	cl, err := kevago.NewInternalClient(addr)
	if err != nil {
		return nil, err
	}
	return &internalClientImpl{cl}, nil
}

func NewFromConfig(conf Config) (*Sentinel, error) {
	if conf.MyID == "" {
		conf.MyID = uuid.NewString()
	}
	return &Sentinel{
		runID:           conf.MyID,
		conf:            conf,
		mu:              &sync.Mutex{},
		clientFactory:   newInternalClient,
		masterInstances: map[string]*masterInstance{},
		logger:          newLogger(),
	}, nil
}

func NewFromConfigFile(filepath string) (*Sentinel, error) {
	viper.SetConfigType("yaml")
	viper.SetConfigFile(filepath)
	err := viper.ReadInConfig()
	if err != nil {
		return nil, err
	}
	var conf Config
	err = viper.Unmarshal(&conf)
	if err != nil {
		return nil, err
	}
	return NewFromConfig(conf)
}

func (s *Sentinel) Start() error {
	if len(s.conf.Masters) != 1 {
		return fmt.Errorf("only support monitoring 1 master for now")
	}
	m := s.conf.Masters[0]
	parts := strings.Split(m.Addr, ":")
	masterIP, masterPort := parts[0], parts[1]
	cl, err := s.clientFactory(m.Addr)
	if err != nil {
		return err
	}

	// cl2, err := kevago.NewInternalClient(m.Addr)
	if err != nil {
		return err
	}

	//read master from config, contact that master to get its slave, then contact it slave and sta
	infoStr, err := cl.Info()
	if err != nil {
		return err
	}
	s.mu.Lock()
	master := &masterInstance{
		sentinelConf:       m,
		name:               m.Name,
		ip:                 masterIP,
		port:               masterPort,
		configEpoch:        m.ConfigEpoch,
		mu:                 sync.Mutex{},
		client:             cl,
		slaves:             map[string]*slaveInstance{},
		sentinels:          map[string]*sentinelInstance{},
		state:              masterStateUp,
		lastSuccessfulPing: time.Now(),
		subjDownNotify:     make(chan struct{}),
	}
	s.masterInstances[m.Addr] = master
	s.mu.Unlock()
	switchedRole, err := s.parseInfoMaster(m.Addr, infoStr)
	if err != nil {
		return err
	}
	if switchedRole {
		return fmt.Errorf("reported address of master %s is not currently in master role", m.Name)
	}

	go s.serveRPC()
	go s.masterRoutine(master)
	return nil
}

func (s *Sentinel) Shutdown() {
	s.listener.Close()
}

type internalClient interface {
	Info() (string, error)
	Ping() (string, error)
	SubscribeHelloChan() HelloChan
}

type HelloChan interface {
	Close() error
	Publish(string) error
	Receive() (string, error)
}

type internalClientImpl struct {
	*kevago.InternalClient
}

func (s *internalClientImpl) SubscribeHelloChan() HelloChan {
	panic("unimplemented")
	return nil
}

type sentinelInstance struct {
	runID string
	mu    sync.Mutex
	// masterDown          bool
	client              sentinelClient
	lastMasterDownReply time.Time

	// these 2 must alwasy change together
	leaderEpoch int
	leaderID    string

	sdown bool
}

type masterInstanceState int

var (
	masterStateUp       masterInstanceState = 1
	masterStateSubjDown masterInstanceState = 2
	masterStateObjDown  masterInstanceState = 3
)

type slaveInstance struct {
	runID              string
	masterName         string
	killed             bool
	mu                 sync.Mutex
	masterDownSinceSec time.Duration
	masterHost         string
	masterPort         string
	masterUp           bool
	addr               string
	slavePriority      int //TODO
	replOffset         int
	reportedRole       instanceRole
	reportedMaster     *masterInstance
	sDown              bool

	lastSucessfulPingAt time.Time
	lastSucessfulInfoAt time.Time

	//each slave has a goroutine that ping by interval
	pingShutdownChan chan struct{}
	//each slave has a goroutine that check info every 10s
	infoShutdownChan chan struct{}
	//notify goroutines that master is down, to change info interval from 10 to 1s like Redis
	masterDownNotify chan struct{}
	client           internalClient
}

type instanceRole int

var (
	instanceRoleMaster instanceRole = 1
	instanceRoleSlave  instanceRole = 2
)

type sentinelClient interface {
	IsMasterDownByAddr(IsMasterDownByAddrArgs) (IsMasterDownByAddrReply, error)
}

type IsMasterDownByAddrArgs struct {
	Name         string
	IP           string
	Port         string
	CurrentEpoch int
	SelfID       string
}
type IsMasterDownByAddrReply struct {
	MasterDown    bool
	VotedLeaderID string
	LeaderEpoch   int
}

const (
	SENTINEL_MAX_DESYNC = 1000
)
