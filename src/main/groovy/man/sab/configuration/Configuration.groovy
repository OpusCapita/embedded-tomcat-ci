package man.sab.configuration

import java.nio.file.Files

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

/*
 * ToDo
 * - add ajp connector support
 * - tomcatJvmRoute
 */
class Configuration {

    static Map getDefault() {
        [
            application: [
                // application configuration.properties file path
                configurationFilePath: null
            ],
            tomcat: [
                port: 8080,
                httpConnector: [
                    URIEncoding: 'UTF-8',
                    useBodyEncodingForURI: true,
                    maxHttpHeaderSize: 32768,
                    relaxedQueryChars: "[,],{,},|",
                    enableCompression: true,
                    compressableMimeType: "text/html,application/xhtml+xml,application/json,text/json",
                ],
                // add RemoteIpValve
                enableProxySupport: true,
                // application context path ("" - means root)
                contextPath: '',
            ],
            // working directory path that will be used for storing extracted application war file and etc.
            workDirPath: Files.createTempDirectory("${new Date().format("YYYY-MM-dd")}-tomcat-").toFile().absolutePath
        ]
    }

    // if some part of configuration is not defuned in passed yaml then default values from default
    // configuration will be used
    static Map load(def yaml) {
        assert yaml != null, "'yaml' is null"

        merge(getDefault(), new Yaml().load(yaml))
    }

    static dumpToString(def configuration) {
        new Yaml().dump(configuration)
    }

    static Map merge(Map[] sources) {
        if (sources.length == 0) return [:]
        if (sources.length == 1) return sources[0]

        sources.inject([:]) { result, source ->
            source.each { k, v ->
                result[k] = result[k] instanceof Map ? merge(result[k], v) : v
            }
            result
        }
    }
}
