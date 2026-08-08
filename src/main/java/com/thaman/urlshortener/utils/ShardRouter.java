package com.thaman.urlshortener.utils;

import com.thaman.urlshortener.config.ShardingProperties;
import org.springframework.stereotype.Component;

@Component
public class ShardRouter {

  private final int shardCount;

  public ShardRouter(ShardingProperties shardingProperties) {
    int shardCount = shardingProperties.shardCount();
    if (shardCount < 1) {
      throw new IllegalArgumentException("app.sharding.shards must have at least 1 entry, got " + shardCount);
    }
    this.shardCount = shardCount;
  }

  public int shardFor(long id) {
    long mixed = fmix64(id);
    return (int) ((mixed & Long.MAX_VALUE) % shardCount);
  }

  // MurmurHash3's 64-bit finalizer: spreads entropy from every input bit across
  // every output bit, so a structured value like our Snowflake id (mostly-zero
  // low bits under normal load) still hashes to a uniformly distributed result.
  private static long fmix64(long k) {
    k ^= (k >>> 33);
    k *= 0xff51afd7ed558ccdL;
    k ^= (k >>> 33);
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= (k >>> 33);
    return k;
  }
}
