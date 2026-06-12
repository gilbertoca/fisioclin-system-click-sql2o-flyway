package org.apache.click.showcase.fisio.infra;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String jdbcUrl = "jdbc:h2:mem:fisioclin_prod;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        
        // Static instantiation pattern: boot and leave active on JVM runtime
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        // Safely trigger Flyway migrations immediately at deployment startup
        DataSourceManager.runMigrations();        
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceManager.shutdown();
    }
}
