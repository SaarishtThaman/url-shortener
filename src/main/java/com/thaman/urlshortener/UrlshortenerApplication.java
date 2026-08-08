package com.thaman.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableCaching
@ConfigurationPropertiesScan
public class UrlshortenerApplication {

  public static void main(String[] args) {
    SpringApplication.run(UrlshortenerApplication.class, args);
  }
}
