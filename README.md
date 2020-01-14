# Convert your WAR to executable JAR!

## Prerequisites

JDK 8 to build artifact
JRE 8 to run application

## Application (executable jar) build: howto

Before junning application/service set up necessary env variables

```sh
# Java options necessary to compile/build project
export JAVA_OPTS=...
```

```sh
# build executable jar file
mvn clean package -Dapp.warFilePath=[path to your war file] -Dapp.groupId=[group id] -Dapp.artifactId=[artifact id] -Dapp.version=[version]
```
After successful command execution you can find result jar file undex path ```./target/[artifact id]-[version]-executable.jar```

If you would like to install result artufact into local Maven repository type the following in you terminal
```
mvn clean install -Dapp.warFilePath=[path to your war file] -Dapp.groupId=[group id] -Dapp.artifactId=[artifact id] -Dapp.version=[version]
```

If you would like to deploy result artufact into local Maven repository type the following in you terminal
```
mvn clean deploy -Dapp.warFilePath=[path to your war file] -Dapp.groupId=[group id] -Dapp.artifactId=[artifact id] -Dapp.version=[version]
```

## Run application: howto

Before junning application/service set up necessary env variables

```sh
# Java options necessary to start and run application
export JAVA_OPTS=...
```

Start applocation with default configuration
```sh
java -jar <path to you executable jar file>
```

Passing configuration to application via system variable `config.files`
```sh
java -jar <path to you executable jar file> -Dconfig.files=config.yaml
```



## Configuration reference
```yaml
application:
  configutation-file-path: <path to configuration>    # (String )path to configuration.properties file, examples
                                                      # /a/b/c/configuration.properties
                                                      # ./configuration.properties
tomcat:
  port:                                               # (Integer) 8080 by default
  contextPath: <application context path>            # (String) /prov or /eprocurement/prov or / or ...
```



