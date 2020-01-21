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
gradle clean shadowJar -Dapplication.warFilePath=[path to your war file] -Dapplication.groupId=[group id] -Dapplication.artifactId=[artifact id] -Dapplication.version=[version]
```
After successful command execution you can find result jar file undex path ```./build/libs/[artifact id]-[version]-executable.jar```

If you would like to deploy result artufact into local Maven repository type the following in you terminal
```
gradle clean shadowJar pubish -Dapplication.warFilePath=[path to your war file] -Dapplication.groupId=[group id] -Dapplication.artifactId=[artifact id] -Dapplication.version=[version]
```

## Run application: howto

Before junning application/service set up necessary env variables

```sh
# Java options necessary to start and run application
export _JAVA_OPTIONS=...
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
  configutation-file-path: <path to configuration>    # (String )path to configuration.properties file,                                                            # examples:
                                                      #  /a/b/c/configuration.properties
                                                      #  ./configuration.properties
tomcat:
  port:                                               # (Integer) 8080 by default
  contextPath: <application context path>             # "" (root) by default. Examples:
                                                      #  '/prov'
                                                      #  '/procurement/prov
  httpConnector:
      URIEncoding:                                    # 'UTF-8' by default
      useBodyEncodingForURI:                          # true by default
      maxHttpHeaderSize:                              # 32768 by default
      relaxedQueryChars:                              # "[,],{,},|" by default
      enableCompression:                              # true by default
      compressableMimeType:                           # "text/html,application/xhtml+xml,application/json,text/json" by default

```

## References
- [Apache Tomcat Maven plugin](https://github.com/apache/tomcat-maven-plugin)
- [Gradle plugin supporting deployment of your web application to an embedded Tomcat web container](https://github.com/bmuschko/gradle-tomcat-plugin)
- [Grails standalone](https://github.com/grails-plugins/grails-standalone)


