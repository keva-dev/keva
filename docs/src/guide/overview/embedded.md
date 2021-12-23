# Java Embedded

You can use KevaDB as an embedded database in your Java/Kotlin/Scala project, thus can use in for example integration tests.

## Install

`build.gradle`

```groovy
dependencies {
    implementation 'dev.keva:kevadb:0.1.4'
}
```

or:

`pom.xml`

```xml
<dependency>
    <groupId>dev.keva</groupId>
    <artifactId>kevadb</artifactId>
    <version>0.1.4</version>
</dependency>
```

## Usage

Example usage:

`App.java`

```java
package test.keva;

import dev.keva.core.config.KevaConfig;
import dev.keva.core.server.KevaServer;

public class App {
    public static void main(String[] args) {
        KevaConfig kevaConfig = KevaConfig.ofDefaults();
        KevaServer server = KevaServer.of(kevaConfig);
        server.run();
    }
}
```
