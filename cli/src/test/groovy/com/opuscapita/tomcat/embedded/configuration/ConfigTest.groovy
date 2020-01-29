package com.opuscapita.tomcat.embedded.configuration

import org.junit.Test

class ConfigTest {

    @Test
    void testLoadFromString() {
        def yaml = """
application:
    configurationProperties:
        url: 'https://its.is.me'
contextPath: '/a/b/c'
tomcat:
    port: 9876
"""
        def configuration = Config.load(yaml)
        assert configuration
        assert configuration.application
        assert configuration.application.configurationProperties?.url == 'https://its.is.me'
        assert configuration.contextPath == '/a/b/c'
        assert configuration.tomcat
        assert configuration.tomcat.port == 9876
    }

    @Test
    void testLoadFromEmptyString() {
        assert Config.load('') == Config.default
    }

    @Test
    void testLoadFromEmptyFileAsInputStream() {
        def is = this.class.getResourceAsStream('emptyConfiguration.yaml')
        assert is
        assert Config.load(is) == Config.default
    }
}
