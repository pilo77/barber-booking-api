package com.villamil.barberbooking.support;
import java.util.UUID;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
/** Dedicated test database: an explicit isolated local instance or a disposable Docker container. */
public final class QaPostgres {
    private static PostgreSQLContainer<?> container;
    private QaPostgres() { }
    public static boolean available() {
        return "true".equals(System.getenv("GHS_QA_POSTGRES")) || DockerClientFactory.instance().isDockerAvailable();
    }
    public static synchronized DriverManagerDataSource dataSource(String database) {
        if ("true".equals(System.getenv("GHS_QA_POSTGRES")))
            return new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:55438/" + database, "ghs_qa", System.getenv("GHS_QA_PASSWORD"));
        if (container == null) {
            container = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("postgres")
                .withUsername("ghs_qa").withPassword(UUID.randomUUID().toString());
            container.start(); Runtime.getRuntime().addShutdownHook(new Thread(() -> container.stop()));
        }
        return new DriverManagerDataSource("jdbc:postgresql://" + container.getHost() + ":" + container.getMappedPort(5432) + "/" + database,
            container.getUsername(), container.getPassword());
    }
}
