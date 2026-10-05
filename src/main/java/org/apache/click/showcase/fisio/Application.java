package org.apache.click.showcase.fisio;

import java.io.InputStream;
import java.util.Properties;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.apache.click.showcase.fisio.infra.DataSourceManager;

public class Application {

    public static void main(String[] args) throws Exception {
        Properties mavenDefaults = new Properties();

        // 1. Load Maven filtered defaults from the classpath
        try (InputStream input = Application.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                mavenDefaults.load(input);
            }
        } catch (Exception e) {
            System.err.println("Warning: application.properties could not be loaded. Falling back to code defaults.");
        }

        // 2. Cascade Port Settings (System Environment -> Java Property -> Maven POM Fallback)
        String envPort = System.getenv("PORT");
        String sysPort = System.getProperty("server.port");
        String pomPort = mavenDefaults.getProperty("default.server.port", "8080");

        int port = Integer.parseInt( envPort != null ? envPort : (sysPort != null ? sysPort : pomPort) );

        // 3. Cascade Database Settings (System Environment -> Maven POM Fallbacks)
        String jdbcUrl = System.getenv().getOrDefault("DB_URL", mavenDefaults.getProperty("default.db.url"));
        String username = System.getenv().getOrDefault("DB_USER", mavenDefaults.getProperty("default.db.user", "sa"));
        String password = System.getenv().getOrDefault("DB_PASS", mavenDefaults.getProperty("default.db.password", "sa"));
        String driver = System.getenv().getOrDefault("DB_DRIVER", mavenDefaults.getProperty("default.db.driver", "org.h2.Driver"));

        System.out.println("--------------------------------------------------");
        System.out.println("Starting Application Server Core...");
        System.out.println("HTTP Port Context:   " + port);
        System.out.println("Database URL:        " + jdbcUrl);
        System.out.println("Database Driver:     " + driver);
        System.out.println("--------------------------------------------------");

        // 4. Initialize Infrastructure safely before turning on the socket listener
        DataSourceManager.initialize(jdbcUrl, username, password, driver);
        DataSourceManager.runMigrations();

        // 5. Build and launch Jetty 12
        Server server = new Server(port);
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase("./src/main/webapp");
        context.setParentLoaderPriority(true);

        server.setHandler(context);

        server.setStopTimeout(30000);
        server.setStopAtShutdown(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Container terminating. Closing pool streams...");
            DataSourceManager.shutdown();
        }));

        server.start();
        server.join();
    }
}
