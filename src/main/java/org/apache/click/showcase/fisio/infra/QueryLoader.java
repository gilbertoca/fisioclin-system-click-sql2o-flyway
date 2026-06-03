package org.apache.click.showcase.fisio.infra;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class QueryLoader {

    private final Properties properties;

    public QueryLoader(String resourcePath) {
        this.properties = new Properties();
        loadProperties(resourcePath);
    }

    private void loadProperties(String resourcePath) {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Unable to locate external properties file at: " + resourcePath);
            }
            this.properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Fatal error parsing external query catalog: " + resourcePath, ex);
        }
    }

    /**
     * Resolves a target statement by its identifier key.
     */
    public String get(String queryKey) {
        String sql = properties.getProperty(queryKey);
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalStateException("Requested query key code not mapped in configuration: " + queryKey);
        }
        return sql;
    }
}
