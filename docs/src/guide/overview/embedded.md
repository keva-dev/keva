# Java Embedded

You can use KevaDB as an embedded database in your Java/Kotlin/Scala project, thus can use in for example integration tests.

## Install

Currently, only Sonatype's snapshot repository is supported.

`build.gradle`

```groovy
repositories {
    // Load snapshot repository
    maven {
        url "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    }
}

dependencies {
    implementation 'dev.keva:kevadb:0.1.0-SNAPSHOT'
}
```

or:

`pom.xml`

```xml
<repositories>
    <repository>
        <id>s01.oss.sonatype.org-snapshot</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>dev.keva</groupId>
    <artifactId>kevadb</artifactId>
    <version>0.1.0-SNAPSHOT</version>
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
