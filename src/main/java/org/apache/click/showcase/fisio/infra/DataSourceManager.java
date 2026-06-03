package org.apache.click.showcase.fisio.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DataSourceManager implements java.io.Closeable {

    private final HikariDataSource dataSource;
    private final Sql2o sql2o;

    public DataSourceManager(String url, String username, String password, String driverClassName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);

        this.dataSource = new HikariDataSource(config);

        // MAPA DE CONVERSORES PARA SUTILEZAS DO JDK 17 / java.time
        Map<Class, Converter> converters = new HashMap<>();

        converters.put(LocalDate.class, new Converter<LocalDate>() {
            @Override
            public LocalDate convert(Object val) throws ConverterException {
                if (val == null) return null;
                if (val instanceof java.sql.Date) return ((java.sql.Date) val).toLocalDate();
                if (val instanceof java.util.Date) return new java.sql.Date(((java.util.Date) val).getTime()).toLocalDate();
                return LocalDate.parse(val.toString());
            }
            @Override public Object toDatabaseParam(LocalDate val) { return val; }
        });

        converters.put(LocalDateTime.class, new Converter<LocalDateTime>() {
            @Override
            public LocalDateTime convert(Object val) throws ConverterException {
                if (val == null) return null;
                if (val instanceof java.sql.Timestamp) return ((java.sql.Timestamp) val).toLocalDateTime();
                if (val instanceof java.util.Date) return new java.sql.Timestamp(((java.util.Date) val).getTime()).toLocalDateTime();
                return LocalDateTime.parse(val.toString());
            }
            @Override public Object toDatabaseParam(LocalDateTime val) { return val; }
        });

        // Inicializa injetando os mappers de correção de reflexão
        this.sql2o = new Sql2o(this.dataSource, new NoQuirks(converters));
        runMigrations();
    }

    private void runMigrations() {
        Flyway.configure()
              .dataSource(this.dataSource)
              .locations("classpath:db/migration")
              .load()
              .migrate();
    }

    public Sql2o getSql2o() { return this.sql2o; }

    @Override
    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }
}
