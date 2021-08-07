
test-go-integration:
	make start;
	sleep 2;
	cd go-client && go test . -count 1 -v -run Cmd;
	make stop
test-go-unit:
	cd go-clint && go test . -count 1 -v -run Pool

start: server.PID

server.PID:
    # cd bin && { python server.py & echo $$! > $@; }
	./gradlew server:run & echo $$! > $@

stop: server.PID
	kill `cat $<` && rm $<

run-with-file: 
	./gradlew server:run --args="-f $(realpath $(config_file))"
.PHONY: start stop