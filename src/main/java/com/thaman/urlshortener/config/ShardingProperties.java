package com.thaman.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.sharding")
public class ShardingProperties {

  private List<Shard> shards;

  public List<Shard> getShards() {
    return shards;
  }

  public void setShards(List<Shard> shards) {
    this.shards = shards;
  }

  public int shardCount() {
    return shards.size();
  }

  public static class Shard {
    private String url;
    private String username;
    private String password;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
