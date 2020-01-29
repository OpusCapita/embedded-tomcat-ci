package com.opuscapita.tomcat.embedded;

import com.google.devtools.common.options.OptionsParser

import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import com.opuscapita.tomcat.embedded.configuration.Config

import org.apache.catalina.LifecycleEvent
import org.apache.catalina.LifecycleException
import org.apache.catalina.LifecycleListener
import org.apache.catalina.LifecycleState
import org.apache.catalina.core.StandardServer
import org.apache.catalina.startup.Tomcat
import org.apache.catalina.valves.RemoteIpValve

class Main {
    static void main(String[] args)  {
        println "Parsing arguments"

        // Options
        OptionsParser parser = OptionsParser.newOptionsParser(Options);
        parser.parseAndExitUponError(args);
        Options options = parser.getOptions(Options);
        // print all the parsed options
        println "\nDEBUG: passed options"
        options.asMap().each {key, value ->
            println "  ${key}: ${value}"
        }
        println ""

        def configuration = Config.default;
        if (options.configuration) {
            def configurationFile = new File(options.configuration)
            if (!configurationFile.exists()) {
                println "Configuration file '${configurationFile}' is not found";
                System.exit(1);
            }
            configurationFile.withInputStream {def is ->
                configuration = Config.load(is)
            }
        }

        println "Configration that will be used\n\n${Config.dumpToString(configuration)}\n"

        def workDir;
        if (!configuration.workDir) {
            workDir = Files.createTempDirectory("${new Date().format("YYYY-MM-dd")}-tomcat-").toFile()
            println "Configuration 'workDir' was not specified, instead temp folder '${workDir.absolutePath}' will be used"
        } else {
            workDir = new File(configuration.workDir);
            println "Configured 'workDir' is '${workDir.absolutePath}'"
        }
        // make sure that work directory exists
        workDir.mkdirs();

        // extract appllucation war file
        def applicationDir = extractWarFileAndConfigure(workDir, configuration)
        // create tomcat instance
        def tomcat = new Tomcat()

        configureTomcat(tomcat, workDir, applicationDir, configuration)
        Josso.configure(tomcat, workDir, configuration)

        try {
            println("Start tomcat");
            tomcat.start();
            tomcat.server.await();
        } catch(LifecycleException e) {
            e.printStackTrace();
            System.err.println("Tomcat start failed '${e.message}'");
            System.exit(1);
        }
    }

    // returns directory were applicatino was extracted
    static File extractWarFileAndConfigure(File workDirectory, configuration) {
        URL applicationWarFileUrl = Main.class.classLoader.getResource("META-INF/application.war");
        // resource is not found?!
        if (applicationWarFileUrl == null) {
            throw new RuntimeException("File 'META-INF/app.war' is not found inside jar archive");
        }
        // folder where application will be extracted
        File applicationTempDir = new File(workDirectory, "application");
        // delete old dir if it exists
        applicationTempDir.deleteDir()
        // create it again
        applicationTempDir.mkdirs();
        // on JVM shutdown delete this dir
        applicationTempDir.deleteOnExit();

        System.out.println("DEBUG: extracting application war file into '${applicationTempDir.absolutePath}' directory");
        unzip(applicationWarFileUrl, applicationTempDir);

        if (configuration.configurationProperties) {
            Properties configurationProperties = new Properties();
            configurationProperties.putAll(configuration.configurationProperties);
            // create new file or replace content of exiting application configuration file
            def configurationPropertiesFile = new File(applicationTempDir, 'WEB-INF/conf/configuration.properties')
            if (!configurationPropertiesFile.exists()) {
                configurationPropertiesFile.parentFile.mkdirs()
                configurationPropertiesFile.createNewFile()
            }
            configurationPropertiesFile.withWriter {writer ->
                configurationProperties.store(writer, null)
            }
            println "\nDEBUG: configuration.properties file content\n\n${configurationPropertiesFile.text}\n"
        } else {
            println "DEBUG: 'configurationProperties' is not defined"
        }

        return applicationTempDir
    }

    static configureTomcat(tomcat, workDir, applicationDir, configuration) {
        // todo: tomcat creates temp dir for webapp like "tomcat.8080", not clear how to change its location
        // configure application context
        def context = tomcat.addWebapp(configuration.contextPath, applicationDir.absolutePath);
        // File applicationWorkDir = new File(workDir, "application-work")
		// applicationWorkDir.deleteDir()
        // applicationWorkDir.mkdirs()
        // context.workDir = applicationWorkDir
        // println "DEBUG: application  work dir: '${applicationWorkDir.absolutePath}'"
        // set up port
        tomcat.port = configuration.tomcat.port
        // set up base tomcat directory
        File tomcatBaseDir = new File(workDir, "tomcat-tmp")
		tomcatBaseDir.deleteDir()
        tomcatBaseDir.mkdirs()
        tomcat.baseDir = tomcatBaseDir.absolutePath
        // configure http connector
        def connector = tomcat.connector;
        // base set up
        connector.URIEncoding = configuration.tomcat.httpConnector.URIEncoding
        connector.useBodyEncodingForURI = configuration.tomcat.httpConnector.useBodyEncodingForURI
        if (configuration.tomcat.httpConnector.maxHttpHeaderSize) {
            connector.setProperty('maxHttpHeaderSize', (String)configuration.tomcat.httpConnector.maxHttpHeaderSize)
        }
        if (configuration.tomcat.httpConnector.relaxedQueryChars) {
            connector.setProperty('relaxedQueryChars', configuration.tomcat.httpConnector.relaxedQueryChars)
        }

        // compression
        if (configuration.tomcat.httpConnector.enableCompression) {
			connector.setProperty("compression", "on")
			connector.setProperty("compressableMimeType", configuration.tomcat.httpConnector.compressableMimeType)
        }
        // proxy support
        if (configuration.tomcat.enableProxySupport) {
            tomcat.engine.pipeline.addValve(new RemoteIpValve());
        }

        // add shutdown hook
        addShutdownHook {
            try {
                tomcat?.server.stop();
            } catch (LifecycleException e) {
                throw new RuntimeException("WARNING: Cannot Stop Tomcat '${e.message()}'", e);
            }
		}

        // allow Tomcat to shutdown if a context failure is detected
		context.addLifecycleListener(new LifecycleListener() {
			public void lifecycleEvent(LifecycleEvent event) {
				if (event.lifecycle.state == LifecycleState.FAILED) {
					def server = tomcat.server;
					if (server instanceof StandardServer) {
						System.err.println("SEVERE: Context [${configuration.tomcat.contextPath}] failed in" +
								" [${event.lifecycle.class.name}] lifecycle. Allowing Tomcat to shutdown.");
						server.stopAwait();
					}
				}
			}
		});
    }

    static void unzip(URL urlToZipFile, File targetDir) throws IOException {
        ZipInputStream zip = new ZipInputStream(urlToZipFile.openStream());
        try {
            ZipEntry entry;

            while ((entry = zip.nextEntry) != null) {
                File file = new File(targetDir, entry.name);

                if (!file.toPath().normalize().startsWith(targetDir.toPath())) {
                    throw new IOException("Bad zip entry");
                }

                if (entry.isDirectory()) {
                    file.mkdirs();
                    continue;
                }

                byte[] buffer = new byte[4096];
                file.parentFile.mkdirs();
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.close();
            }
        } finally {
            zip.close();
        }
    }


}
