# Convert your WAR to executable JAR!

## Prerequisites

Installed tools:
- JDK 8
- Gradle 6.x

P.S. Be sure that Gradle dependencies and maven-publish plugin are configured configuration !

**Gradle configuration example**

```groovy
allprojects {
    apply plugin: 'maven-publish'

    repositories {
        maven {
            url "https://opuscapita.jfrog.io/opuscapita/maven-get"
            credentials {
                username "bob"
                password "kinikinik"
            }
        }

        maven {
            url 'https://repo1.maven.org/maven2/'
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url "https://central.maven.org/maven2/"
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url "https://oss.sonatype.org/content/repositories/releases"
            mavenContent {
                releasesOnly()
            }
        }
    }

    publishing {
        repositories {
            maven {
                name 'artifactory'
                url "https://opuscapita.jfrog.io/opuscapita/maven-get"
                credentials {
                    username "hannah"
                    password "madam"
                }
            }
        }
    }
}
```

## Application (executable jar) build: howto

```sh
# build executable jar file
gradle clean shadowJar \
  -Dapplication.war=<path to application war file> \
  -Dapplication.groupId=<application group id> \
  -Dapplication.artifactId=>application artifact id> \
  -Dapplication.version=<application version> \
  -Dtomcat.version=<tomcat version> \
  -Djosso.version=<inhouse customized/modified josso version)
```

Notes:
- `application.war` - you should have you application war file somewere near by on FS (may be later we could skip it and then corresponding war file will be downloaded from maven repository automatically)
- `application.groupId`, `application.artifactId`, `application.version` are used for result jar file deployment, these parametrs defines Maven artifact coordinates. Additionally to it predefined classifier **executable** and packaging **jar** will be used
- if `tomcat.version` is not specified, version hardcoded in [build.gradle](build.gradle) will be used
- if `josso.version` is not specified, version hardcoded in [build.gradle](build.gradle) will be used

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

Passing configuration to application via system property `configuration`
```sh
java -jar <path to you executable jar file> -Dconfiguration=config.yaml
```

## Configuration reference
```yaml
configurationProperties:
  # configurratio.properties content
  # key: value
contextPath:                  # Application context path. "" (root) by default.
                              # Examples:
                              #  '/prov'
                              #  '/procurement/prov
tomcat:
  port:                       # (Integer) 8080 by default
  httpConnector:
      URIEncoding:            # 'UTF-8' by default
      useBodyEncodingForURI:  # true by default
      maxHttpHeaderSize:      # 32768 by default
      relaxedQueryChars:      # "[,],{,},|" by default
      enableCompression:      # true by default
      compressableMimeType:   # "text/html,application/xhtml+xml,application/json,text/json" by default
josso:
  enabled: true                                    # whether JoSSO is enabled or not
  publicUrl: http://josso.com                      # JoSSO public URL
  serviceUrl: http://josso.local                   # JoSSO service URL
  applicationId: prov                              # installed application ID
  applicationPublicUrl: http://localhost:8080/ppp  # installed application public URL
workDir: /usr/share/prov                           # base dirtory, where application will be
                                                   # extracted and necessary configuration files
                                                   # will be placed during app execution
```

## References
- [Apache Tomcat Maven plugin](https://github.com/apache/tomcat-maven-plugin)
- [Gradle plugin supporting deployment of your web application to an embedded Tomcat web container](https://github.com/bmuschko/gradle-tomcat-plugin)
- [Grails standalone](https://github.com/grails-plugins/grails-standalone)


