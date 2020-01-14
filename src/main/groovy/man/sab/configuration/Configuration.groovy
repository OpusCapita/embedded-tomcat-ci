package man.sab.configuration

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

class Configuration {

    static Map getDefault() {
        [
            application: [
                configurationFilePath: null
            ],
            tomcat: [
                port: 80,
                contextPath: '/'
            ]
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
