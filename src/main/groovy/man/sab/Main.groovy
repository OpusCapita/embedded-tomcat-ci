package man.sab;

import com.google.devtools.common.options.OptionsParser;

import java.net.URL;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import man.sab.configuration.Configuration;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;


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

        def configuration = Configuration.getDefault();
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

        URL applicationWarFileUrl = Main.class.getClassLoader().getResource("META-INF/application.war");
        // resource is not found?!
        if (applicationWarFileUrl == null) {
            throw new RuntimeException("File 'META-INF/app.war' is not found inside jar archive");
        }
        // create temp folder where app will be extracted
        File applicationTempDir = Files.createTempDirectory("application").toFile();
        // on JVM shutdown delete this dir
        applicationTempDir.deleteOnExit();

        System.out.println("Extracting application war file into '" + applicationTempDir.getAbsolutePath() + "' folder");
        extract(applicationWarFileUrl, applicationTempDir);

        Tomcat tomcat = new Tomcat();

        StandardContext ctx = (StandardContext) tomcat.addWebapp("/prov", applicationTempDir.getAbsolutePath());
        System.out.println("Сonfiguring app with basedir '" + applicationTempDir.getAbsolutePath() + "'");

        System.out.println("starting tomcat");
        tomcat.start();
        tomcat.getServer().await();
    }

    private static final int BUFFER_SIZE = 4096;

    public static void extract(URL urlToZipFile, File target) throws IOException {
        ZipInputStream zip = new ZipInputStream(urlToZipFile.openStream());
        try {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                File file = new File(target, entry.getName());

                if (!file.toPath().normalize().startsWith(target.toPath())) {
                    throw new IOException("Bad zip entry");
                }

                if (entry.isDirectory()) {
                    file.mkdirs();
                    continue;
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                file.getParentFile().mkdirs();
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
