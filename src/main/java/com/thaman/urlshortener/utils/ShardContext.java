package com.thaman.urlshortener.utils;

public class ShardContext {

  private static final ThreadLocal<Integer> CURRENT_SHARD = new ThreadLocal<>();

  private ShardContext() {}

  public static void setShard(int shard) {
    CURRENT_SHARD.set(shard);
  }

  public static Integer getShard() {
    return CURRENT_SHARD.get();
  }

  public static void clear() {
    CURRENT_SHARD.remove();
  }
}
