package com.quezon.cmms.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class OracleDataSourceConfig {

    @Value("${ORACLE_USE_WALLET:false}")
    private boolean useWallet;

    @Value("${ORACLE_WALLET_PATH:/opt/oracle/wallet}")
    private String walletPath;

    @Value("${ORACLE_SERVICE:}")
    private String oracleService;

    @Value("${ORACLE_HOST:}")
    private String oracleHost;

    @Value("${ORACLE_PORT:1521}")
    private String oraclePort;

    @Value("${ORACLE_USER:}")
    private String oracleUser;

    @Value("${ORACLE_PASSWORD:}")
    private String oraclePassword;

    @PostConstruct
    public void init() {
        if (useWallet) {
            System.setProperty("oracle.net.tns_admin", walletPath);
        }
    }

    @Bean
    public DataSource dataSource() {
        String jdbcUrl;
        if (useWallet) {
            jdbcUrl = "jdbc:oracle:thin:@" + oracleService;
        } else {
            jdbcUrl = String.format("jdbc:oracle:thin:@//%s:%s/%s", oracleHost, oraclePort, oracleService);
        }

        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("oracle.jdbc.driver.OracleDriver")
                .url(jdbcUrl)
                .username(oracleUser)
                .password(oraclePassword)
                .build();

        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(10);
        ds.setPoolName("cmms-oracle-pool");

        return ds;
    }
}
