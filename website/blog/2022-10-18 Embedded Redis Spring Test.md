---
slug: keva-as-redis-embedded-server-spring-boot-test
title: Keva as Embedded Redis Server for Spring Boot Test
authors: [tu]
tags: [test]
---

Spring Data Redis provides an easy way to integrate with Redis instances.

However, in some cases, it's more convenient to use an embedded server than to create an environment with a real server.

In this article, we will introduce how to use Keva as an embedded Redis server for Spring Boot test.

## Install Keva as a dependency

Keva is a Java library, so we can use it as a dependency in our project.

`build.gradle`

```groovy
dependencies {
    implementation 'dev.keva:kevadb:1.0.0-rc2'
}
```

or:

`pom.xml`

```xml
<dependency>
    <groupId>dev.keva</groupId>
    <artifactId>kevadb</artifactId>
    <version>1.0.0-rc2</version>
</dependency>
```

## Setup

After adding the dependencies, we should define the connection settings between the Redis server and our application.

Let's begin by creating a class that will hold our properties:

```java
@Configuration
public class RedisProperties {
    private int redisPort;
    private String redisHost;

    public RedisProperties(
      @Value("${spring.redis.port}") int redisPort, 
      @Value("${spring.redis.host}") String redisHost) {
        this.redisPort = redisPort;
        this.redisHost = redisHost;
    }

    // getters
}
```

Next, we should create a configuration class that defines the connection and uses our properties:

```java
@Configuration
@EnableRedisRepositories
public class RedisConfiguration {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
      RedisProperties redisProperties) {
        return new LettuceConnectionFactory(
          redisProperties.getRedisHost(), 
          redisProperties.getRedisPort());
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
```

The configuration is quite simple. Additionally, it allows us to run the embedded server on a different port.

Check out our Introduction to Spring Data Redis article to learn more about the Redis with Spring Boot.

## Keva as Embedded Redis Server

Now, we'll configure the embedded server and use it in one of our tests.

```properties
spring.redis.host=localhost
spring.redis.port=6370
```

After that, we'll create a @TestConfiguration-annotated class:

```java
@TestConfiguration
public class TestRedisConfiguration {

    private KevaServer redisServer;

    public TestRedisConfiguration(RedisProperties redisProperties) {
        KevaConfig kevaConfig = KevaConfig.builder()
                .hostname(redisProperties.getRedisHost())
                .port(redisProperties.getRedisPort())
                .persistence(false)
                .aof(false)
                .workDirectory("./")
                .build();
        this.redisServer = KevaServer.of(kevaConfig);
    }

    @PostConstruct
    public void postConstruct() {
        redisServer.run();
    }

    @PreDestroy
    public void preDestroy() {
        redisServer.shutdown();
    }
}
```

The server will start once the context is up. It'll start on our machine on the port that we've defined in our properties. For instance, we can now run the test without stopping the actual Redis server.

Additionally, the server will stop once the context is destroyed.

Finally, let's create a test that'll use our TestRedisConfiguration class:

```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void shouldSaveUser_toRedis() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "name");

        User saved = userRepository.save(user);

        assertNotNull(saved);
    }
}
```

The user has been saved to our embedded Keva server.

Additionally, we had to manually add TestRedisConfiguration to SpringBootTest. As we said earlier, the server has started before the test and stopped after.

The code for examples is [available over on GitHub](https://github.com/tuhuynh27/keva-embedded-redis-spring-test).

[Reference](https://www.baeldung.com/spring-embedded-redis)
