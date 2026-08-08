package com.thaman.urlshortener.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataSourceConfig {

  @Bean
  public DataSource dataSource(ShardingProperties shardingProperties) {
    List<ShardingProperties.Shard> shards = shardingProperties.getShards();
    Map<Object, Object> targetDataSources = new HashMap<>();

    for (int i = 0; i < shards.size(); i++) {
      targetDataSources.put(i, buildShardDataSource(shards.get(i)));
    }

    ShardRoutingDataSource routingDataSource = new ShardRoutingDataSource();
    routingDataSource.setTargetDataSources(targetDataSources);
    routingDataSource.setDefaultTargetDataSource(targetDataSources.get(0));
    routingDataSource.afterPropertiesSet();
    return routingDataSource;
  }

  private DataSource buildShardDataSource(ShardingProperties.Shard shard) {
    return DataSourceBuilder.create()
        .url(shard.getUrl())
        .username(shard.getUsername())
        .password(shard.getPassword())
        .build();
  }
}
