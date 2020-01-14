package man.sab.configuration

import org.junit.Test

class ConfigurationTest {

    @Test
    void testLoadFromString() {
        def yaml = """
application:
    configurationFilePath: '/a/b/c.yaml'
tomcat:
    port: 9876
    contextPath: '/a/b/c'
"""
        def configuration = Configuration.load(yaml)
        assert configuration
        assert configuration.application
        assert configuration.application.configurationFilePath == '/a/b/c.yaml'
        assert configuration.tomcat
        assert configuration.tomcat.port == 9876
        assert configuration.tomcat.contextPath == '/a/b/c'
    }

    @Test
    void testLoadFromEmptyString() {
        assert Configuration.load('') == Configuration.getDefault()
    }

    @Test
    void testLoadFromEmptyFileAsInputStream() {
        def is = ConfigurationTest.class.getResourceAsStream('emptyConfiguration.yaml')
        assert is
        assert Configuration.load(is) == Configuration.getDefault()
    }
}
