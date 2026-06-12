package org.apache.click.showcase.fisio.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DataSourceManager {

    private static HikariDataSource dataSource;
    private static Sql2o sql2o;

    private DataSourceManager() {}

    /**
     * Inicializa apenas o pool de conexões (HikariCP) e o barramento de dados (Sql2o).
     * Roda estritamente sob privilégios DML (sem comandos de DDL estruturais automáticos).
     */
    public static synchronized void initialize(String url, String username, String password, String driverClassName) {
        if (dataSource != null) return; 

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        
        dataSource = new HikariDataSource(config);

        // Conversores estáveis para java.time do JDK 17
        Map<Class, Converter> converters = new HashMap<>();
        
        converters.put(LocalDate.class, new Converter<LocalDate>() {
            @Override public LocalDate convert(Object val) {
                if (val == null) return null;
                if (val instanceof java.sql.Date) return ((java.sql.Date) val).toLocalDate();
                return LocalDate.parse(val.toString());
            }
            @Override public Object toDatabaseParam(LocalDate val) { return val; }
        });

        converters.put(LocalDateTime.class, new Converter<LocalDateTime>() {
            @Override public LocalDateTime convert(Object val) {
                if (val == null) return null;
                if (val instanceof java.sql.Timestamp) return ((java.sql.Timestamp) val).toLocalDateTime();
                return LocalDateTime.parse(val.toString());
            }
            @Override public Object toDatabaseParam(LocalDateTime val) { return val; }
        });

        sql2o = new Sql2o(dataSource, new NoQuirks(converters));
    }

    /**
     * MÉTODO SIMPLIFICADO: Depende do DataSource ativo.
     * Consome diretamente o pool HikariCP sem exigir novas strings de credenciais.
     */
    public static void runMigrations() {
        if (dataSource == null) {
            throw new IllegalStateException("Impossível rodar as migrações: O pool de conexões não foi inicializado.");
        }
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource) // Utiliza o pool ativo diretamente
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    public static Sql2o getSql2o() {
        if (sql2o == null) {
            throw new IllegalStateException("DataSourceManager não foi inicializado.");
        }
        return sql2o;
    }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
