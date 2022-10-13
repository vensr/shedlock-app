## ShedLock with Spring Boot

### Introduction

Typically in distributed environments, when we deploy applications sometimes we have a requirement that a task executes only once across the distributed systems. 

Spring, by default, cannot handle scheduler synchronization over multiple instances. It executes the jobs simultaneously on every single node instead.

We'll look at ShedLock, a Java library that makes sure our scheduled tasks run only once at the same time and is an alternative to Quartz. ShedLock is designed to be used in situations where you have scheduled tasks that are not ready to be executed in parallel, but can be safely executed repeatedly. Moreover, the locks are time-based and ShedLock assumes that clocks on the nodes are synchronized.

While there are plenty of schedulers which we can use, note that majority of these schedulers still try to execute jobs in parallel across the distributed nodes. 

Shedlock is not a full fledge scheduler, its just a lock. ShedLock makes sure that your scheduled tasks are executed at most once at the same time. If a task is being executed on one node, it acquires a lock which prevents execution of the same task from another node (or thread).

ShedLock uses an external store like Mongo, JDBC database, Redis, Hazelcast, ZooKeeper or others for coordination.

### Shedlock configuration

Ensure that you have a spring boot project and follow the steps along.

* Adding 2 shedlock dependencies. One shedlock-spring and another shedlock-provider-jdbc-template. Note that shedlock saves the state in the DB and uses it for deciding to either execute a task or skip it.

```xml
    <dependency>
        <groupId>net.javacrumbs.shedlock</groupId>
        <artifactId>shedlock-spring</artifactId>
        <version>4.42.0</version>
    </dependency>
    <dependency>
        <groupId>net.javacrumbs.shedlock</groupId>
        <artifactId>shedlock-provider-jdbc-template</artifactId>
        <version>4.42.0</version>
    </dependency>
```

* Adding a datasouce. By default you will have a datasource configured to your DB. Incase you are missing it, you can add the configuration in your application.yml or application.properties file. Note this example uses a h2 DB.

```yml
spring:
  datasource:
    driverClassName: org.h2.Driver
    url: jdbc:h2:mem:shedlock_DB;INIT=CREATE SCHEMA IF NOT EXISTS shedlock;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
```

* Creating a scheduler configuration to enable shedlock and configuring a lock provider. In this example we use JdbcTemplateLockProvider since we are persisting the state in the DB.

```java
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

@Configuration
public class SchedulerConfig {
    
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
    
}
```

* Creating your scheduled tasks. This shows an example scheduled task that executes on a cron trigger. Note that the time lockAtLeastFor and lockAtMostFor uses the time expressions as per [ISO8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations) formats. You can add as many scheduled tasks as you wish.

```java

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class MyTaskScheduler {
 
    @Scheduled(cron = "*/10 * * * * *")
    @SchedulerLock(name = "MyTaskScheduler", lockAtLeastFor = "PT30S", lockAtMostFor = "PT1M")
    public void run() {
        System.out.println("My Scheduled Task");
    }

}

```

* Tying everything together using your application main class.  We have to provide @EnableScheduling and @EnableSchedulerLock annotations on our Spring configuration class. This will enable shedlock scheduler.

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class ShedlockApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShedlockApplication.class, args);
	}

}
```

* In normal situations, ShedLock releases the lock directly after the task finishes. However we can decide how long the lock can be held and overide the defaultLockAtMostFor parameter to ensure that the task does not get locked infinitly. Complete source code example available at [https://github.com/vensr/shedlock-app](https://github.com/vensr/shedlock-app)

* Now you can start your application and run it. You should see that the scheduled task executes every 10 seconds on the console.

```bash
mvn spring-boot:run
```

```bash
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.minilab:shedlock >------------------------
[INFO] Building shedlock 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] >>> spring-boot-maven-plugin:2.7.4:run (default-cli) > test-compile @ shedlock >>>
[INFO] 
[INFO] --- maven-resources-plugin:3.2.0:resources (default-resources) @ shedlock ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Using 'UTF-8' encoding to copy filtered properties files.
[INFO] Copying 1 resource
[INFO] Copying 1 resource
[INFO] 
[INFO] --- maven-compiler-plugin:3.10.1:compile (default-compile) @ shedlock ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- maven-resources-plugin:3.2.0:testResources (default-testResources) @ shedlock ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Using 'UTF-8' encoding to copy filtered properties files.
[INFO] skip non existing resourceDirectory /home/venkatesh/Projects/opensource/shedlock-app/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.10.1:testCompile (default-testCompile) @ shedlock ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] <<< spring-boot-maven-plugin:2.7.4:run (default-cli) < test-compile @ shedlock <<<
[INFO] 
[INFO] 
[INFO] --- spring-boot-maven-plugin:2.7.4:run (default-cli) @ shedlock ---
[INFO] Attaching agents: []

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.7.4)

2022-10-13 13:50:31.555  INFO 70408 --- [           main] c.minilab.shedlock.ShedlockApplication   : Starting ShedlockApplication using Java 17.0.4 on venkatesh-personal with PID 70408 (/home/venkatesh/Projects/opensource/shedlock-app/target/classes started by venkatesh in /home/venkatesh/Projects/opensource/shedlock-app)
2022-10-13 13:50:31.558  INFO 70408 --- [           main] c.minilab.shedlock.ShedlockApplication   : No active profile set, falling back to 1 default profile: "default"
2022-10-13 13:50:31.983  INFO 70408 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2022-10-13 13:50:31.998  INFO 70408 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 5 ms. Found 0 JPA repository interfaces.
2022-10-13 13:50:32.420  INFO 70408 --- [           main] o.f.c.internal.license.VersionPrinter    : Flyway Community Edition 9.4.0 by Redgate
2022-10-13 13:50:32.420  INFO 70408 --- [           main] o.f.c.internal.license.VersionPrinter    : See what's new here: https://flywaydb.org/documentation/learnmore/releaseNotes#9.4.0
2022-10-13 13:50:32.420  INFO 70408 --- [           main] o.f.c.internal.license.VersionPrinter    : 
2022-10-13 13:50:32.431  INFO 70408 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2022-10-13 13:50:32.662  INFO 70408 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2022-10-13 13:50:32.679  INFO 70408 --- [           main] o.f.c.i.database.base.BaseDatabaseType   : Database: jdbc:h2:mem:shedlock_DB (H2 2.1)
2022-10-13 13:50:32.791  INFO 70408 --- [           main] o.f.core.internal.command.DbValidate     : Successfully validated 1 migration (execution time 00:00.009s)
2022-10-13 13:50:32.796  INFO 70408 --- [           main] o.f.c.i.s.JdbcTableSchemaHistory         : Creating Schema History table "PUBLIC"."flyway_schema_history" ...
2022-10-13 13:50:32.887  INFO 70408 --- [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "PUBLIC": << Empty Schema >>
2022-10-13 13:50:32.899  INFO 70408 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "1 - shedlock"
2022-10-13 13:50:32.914  INFO 70408 --- [           main] o.f.core.internal.command.DbMigrate      : Successfully applied 1 migration to schema "PUBLIC", now at version v1 (execution time 00:00.041s)
2022-10-13 13:50:33.090  INFO 70408 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2022-10-13 13:50:33.164  INFO 70408 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 5.6.11.Final
2022-10-13 13:50:33.312  INFO 70408 --- [           main] o.hibernate.annotations.common.Version   : HCANN000001: Hibernate Commons Annotations {5.1.2.Final}
2022-10-13 13:50:33.455  INFO 70408 --- [           main] org.hibernate.dialect.Dialect            : HHH000400: Using dialect: org.hibernate.dialect.H2Dialect
2022-10-13 13:50:33.610  INFO 70408 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
2022-10-13 13:50:33.618  INFO 70408 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2022-10-13 13:50:33.795  INFO 70408 --- [           main] c.minilab.shedlock.ShedlockApplication   : Started ShedlockApplication in 3.071 seconds (JVM running for 3.724)
My Scheduled Task
```


## References

* [https://github.com/lukas-krecan/ShedLock](https://github.com/lukas-krecan/ShedLock)

* [https://github.com/vensr/shedlock-app](https://github.com/vensr/shedlock-app)

