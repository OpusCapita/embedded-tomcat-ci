package com.opuscapita.tomcat.embedded

import groovy.xml.*

import javax.security.auth.login.AppConfigurationEntry
import javax.security.auth.login.Configuration

class Josso {
    static def configure(tomcat, workDir, configuration) {
        if (!configuration.josso.enabled) {
            // josso is not used for this installation
            println "Application will be started without JoSSO"
            return;
        }
        // validating configuration
        if (!configuration.josso.publicUrl) {
            throw new RuntimeException("'josso.publicUrl' configuration property is not defined");
        }
        if (!configuration.josso.serviceUrl) {
            throw new RuntimeException("'josso.serviceUrl' configuration property is not defined");
        }
        if (!configuration.josso.applicationId) {
            throw new RuntimeException("'josso.applicationId' configuration property is not defined");
        }
        if (!configuration.josso.applicationPublicUrl) {
            throw new RuntimeException("'josso.applicationPublicUrl' configuration property is not defined");
        }

        setupJossoJaas(tomcat, workDir, configuration)
        setupJossoAgentConfig(tomcat, workDir, configuration)
        updateServerConfig(tomcat, workDir, configuration)
    }

    static def setupJossoAgentConfig(tomcat, workDir, configuration) {
        def serviceUri = new URI(configuration.josso.serviceUrl)
        // calculate valued to use in config
        def endpoint = (configuration.josso.serviceUrl - "${serviceUri.scheme}://")
        def transportSecurity = ('https' == serviceUri.scheme)?"confidential":"node"

        // def ssoPartnerApplicationId = "sp-${configuration.josso.applicationName}-${configuration.josso.applicationPublicUrl.toString().hashCode().abs()}"
        def ssoPartnerApplicationId = "sp-${configuration.josso.applicationId}"

        def markupBuilder = new StreamingMarkupBuilder()
        def jossoAgentConfig = markupBuilder.bind { builder ->
            mkp.xmlDeclaration(standalone: 'yes')
            mkp.declareNamespace(xsi:'http://www.w3.org/2001/XMLSchema-instance')
            beans('xmlns': 'http://www.springframework.org/schema/beans',
                'xsi:schemaLocation': 'http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd') {
                bean(name: 'josso-tc70-agent', class: 'org.josso.tc70.agent.CatalinaSSOAgent') {
                    property(name: "sessionAccessMinInterval", value:"1000")
                    property(name:"gatewayLoginUrl", value: "${configuration.josso.publicUrl}/IDBUS/TC/JOSSO/SSO/REDIR")
                    property(name:"gatewayLogoutUrl", value: "${configuration.josso.publicUrl}/IDBUS/TC/JOSSO/SLO/REDIR")
                    property(name:'gatewayServiceLocator') {
                        builder.bean(class: "org.josso.gateway.jaxws.JAXWSWebserviceGatewayServiceLocator") {
                            property(name: "wsdlLocation", value: "classpath:org/josso/gateway/ws/_1_2/wsdl/josso-1.2.wsdl")
                            property(name:"endpoint", value:"${endpoint}")
                            property(name: "transportSecurity", value:"${transportSecurity}")
                            property(name:"sessionManagerServicePath", value: "IDBUS/TC/JOSSO/SSOSessionManager/SOAP")
                            property(name:"identityManagerServicePath", value: "IDBUS/TC/JOSSO/SSOIdentityManager/SOAP")
                            property(name: "identityProviderServicePath", value: "IDBUS/TC/JOSSO/SSOIdentityProvider/SOAP")
                        }
                    }
                    property(name:"uriEncoding", value:"UTF-8")
                    property(name:'parametersBuilders') {
                        list {
                            bean(class:"org.josso.agent.http.AppIdParametersBuilder")
                            bean(class:"org.josso.agent.http.HttpRequestParametersBuilder") {
                                property(name: "includeRequestParameters") {
                                    list {
                                        value('josso_.*')
                                    }
                                }
                            }
                        }
                    }

                    property(name: "automaticLoginStrategies") {
                        list {
                            bean(class: "org.josso.agent.http.DefaultAutomaticLoginStrategy") {
                                property(name: "mode", value: "OPTIONAL")
                                property(name: "ignoredReferrers") {
                                    list {
                                        value(configuration.josso.publicUrl)
                                    }
                                }
                            }
                        }
                    }

                    property(name: "configuration") {
                        bean(class: "org.josso.agent.SSOAgentConfigurationImpl") {
                            property(name: "ssoPartnerApps") {
                                list {
                                    bean(class: "org.josso.agent.SSOPartnerAppConfig") {
                                        property(name: "ignoredWebResources") {
                                            list {
                                              value('unprotected-resources')
                                            }
                                        }
                                        property(name: "id", value: "${ssoPartnerApplicationId}")
                                        property(name: "context", value: "${configuration.contextPath}")
                                    }
                                }
                            }
                        }
                    }

                    property(name: "singlePointOfAccess", value: configuration.josso.applicationPublicUrl)
                }
            }
        }
        // saving to 'josso-agent-config.xml' file in work directory
        def jossoAgentConfigContent = XmlUtil.serialize(jossoAgentConfig)
        def jossoAgentConfigFile = new File(workDir, 'josso-agent-config.xml')
        jossoAgentConfigFile.text = jossoAgentConfigContent

        println "\nDEBUG: path to 'josso-agent-config.xml'\n${jossoAgentConfigFile.absolutePath}\n"
        println "\nDEBUG: 'josso-agent-config.xml' content\n${jossoAgentConfigContent}\n"

        System.properties['org.josso.agent.config.ComponentKeeperFactory'] = 'org.josso.agent.config.FileBasedComponentKeeperFactory'
        System.properties['org.josso.agent.config.FileBasedComponentKeeper.JOSSO_AGENT_CONFIG_XML_FILE_PATH'] = jossoAgentConfigFile.absolutePath
    }

    static def updateServerConfig(tomcat, workDir, configuration) {
        // replace realm
        // def realm = Class.forName("org.josso.tc70.agent.jaas.CatalinaJAASRealm").newInstance()
        def realm = Class.forName("org.josso.tc70.agent.jaas.CatalinaJAASRealm").newInstance()
        realm.with {
            appName = 'josso'
            roleClassNames = 'org.josso.gateway.identity.service.BaseRoleImpl'
            userClassNames = 'org.josso.gateway.identity.service.BaseUserImpl'
        }
        tomcat.engine.realm = realm
        // add valve
        def valve = Class.forName("org.josso.tc70.agent.CharacterEncodingPreserveSSOAgentValve").newInstance()
        // valve.appName = 'josso'
        valve.debug = 1
        tomcat.host.pipeline.addValve(valve)
    }

    static def setupJossoJaas(tomcat, workDir, configuration) {
        Configuration.setConfiguration(new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return [new AppConfigurationEntry(
                    "org.josso.tc70.agent.jaas.SSOGatewayLoginModule",
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    ["debug": "true"]
                )] as AppConfigurationEntry[];
            }
        });
    }
}
