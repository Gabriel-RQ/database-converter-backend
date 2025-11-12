package com.gabrielrq.database_converter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableAsync
public class DatabaseConverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseConverterApplication.class, args);
    }
}
