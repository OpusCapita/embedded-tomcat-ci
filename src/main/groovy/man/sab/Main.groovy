package man.sab;

import com.google.devtools.common.options.OptionsParser

import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import man.sab.configuration.Configuration

import org.apache.catalina.LifecycleEvent
import org.apache.catalina.LifecycleException
import org.apache.catalina.LifecycleListener
import org.apache.catalina.LifecycleState
import org.apache.catalina.core.StandardContext
import org.apache.catalina.core.StandardServer
import org.apache.catalina.startup.Tomcat
import org.apache.catalina.valves.RemoteIpValve

class Main {
    static void main(String[] args)  {
        println "Parsing configuration"

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

        def configuration = Configuration.default;
        if (options.configurationFilePath) {
            def configurationFile = new File(options.configurationFilePath)
            if (!configurationFile.exists()) {
                println "Configuration file '${configurationFile}' is not found";
                System.exit(1);
            }
            configurationFile.withInputStream {def is ->
                configuration = Configuration.load(is)
            }
        }

        println "Configration that will be used\n${Configuration.dumpToString(configuration)}\n"

        def workDirPath = configuration.workDirPath
        if (!workDirPath) {
            throw new RuntimeException("'workDirPath' is not defined!");
        }
        def workDir = new File(workDirPath);
        if (!new File(workDirPath).exists()) {
            throw new RuntimeException("Work dorectory '${workDir}' does not exists!");
        }
        // extract appllucation war file
        def applicationTempDir = extractWarFile(workDir)
        // create tomcat instance
        def tomcat = new Tomcat()
        // configure application context
        def context = (StandardContext) tomcat.addWebapp(configuration.tomcat.contextPath, applicationTempDir.absolutePath);
        println "Сonfiguring app with basedir '${applicationTempDir.absolutePath}'"
        // set up port
        tomcat.setPort(configuration.tomcat.port)
        // set up base tomcat directory
        File tomcatBaseDir = new File(workDir, "tomcat-tmp")
		tomcatBaseDir.deleteDir();
        tomcat.setBaseDir(tomcatBaseDir.absolutePath)
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

        try {
            println("Start tomcat");
            tomcat.start();
            // tomcat.server.await();
        } catch(LifecycleException e) {
            e.printStackTrace();
            System.err.println("Tomcat start failed '${e.message}'");
            System.exit(1);
        }
    }

    // returns directory were applicatino was extracted
    static File extractWarFile(File workDirectory) {
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

        System.out.println("Extracting application war file into '${applicationTempDir.absolutePath}' directory");
        unzip(applicationWarFileUrl, applicationTempDir);

        return applicationTempDir
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
