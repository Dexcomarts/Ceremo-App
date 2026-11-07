package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import com.zaxxer.hikari.HikariDataSource;
import io.ceremo.ceremobackend.account.infrastructure.secondary.security.SpringCurrentUser;
import io.ceremo.ceremobackend.wire.account.AccountConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The datasource can only point to this test's disposable container. */
@TestConfiguration(proxyBeanMethods = false)
@EnableJpaAuditing
@EnableTransactionManagement
@Import({AccountConfiguration.class, SpringCurrentUser.class})
class PostgresUserTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18.0");
    }

    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(PostgreSQLContainer postgres) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        dataSource.setAutoCommit(false);
        dataSource.setMaximumPoolSize(4);
        dataSource.setConnectionTimeout(5000);
        dataSource.addDataSourceProperty("options", "-c statement_timeout=15000 -c lock_timeout=10000");
        return dataSource;
    }
}
