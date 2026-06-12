package org.apache.click.showcase.fisio.infra;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class QueryLoader {

    private static final Properties properties = new Properties();

    static {
        // Carrega o arquivo unificado no mapeamento estático da classe no boot
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("queries.properties")) {
            if (input == null) {
                throw new IllegalArgumentException("Arquivo queries.properties não foi encontrado no classpath.");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Falha fatal ao ler o catálogo de consultas externas.", ex);
        }
    }

    // Método utilitário estático acessado diretamente pelas camadas Service
    public static String get(String queryKey) {
        String sql = properties.getProperty(queryKey);
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalStateException("Chave SQL não mapeada no arquivo de propriedades: " + queryKey);
        }
        return sql;
    }
}
