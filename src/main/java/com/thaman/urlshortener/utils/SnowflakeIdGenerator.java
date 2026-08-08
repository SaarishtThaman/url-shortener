package com.thaman.urlshortener.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

  // 2025-01-01T00:00:00Z — keeps the 41-bit timestamp field smaller for longer.
  private static final long CUSTOM_EPOCH = 1735689600000L;

  private static final long MACHINE_ID_BITS = 10L;
  private static final long SEQUENCE_BITS = 12L;

  private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
  private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

  private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

  private final long machineId;

  private long lastTimestamp = -1L;
  private long sequence = 0L;

  public SnowflakeIdGenerator(@Value("${app.snowflake.machine-id}") long machineId) {
    if (machineId < 0 || machineId > MAX_MACHINE_ID) {
      throw new IllegalArgumentException(
          "app.snowflake.machine-id must be between 0 and " + MAX_MACHINE_ID + ", got " + machineId);
    }
    this.machineId = machineId;
  }

  public synchronized long nextId() {
    long timestamp = System.currentTimeMillis();

    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      if (sequence == 0) {
        timestamp = waitForNextMillis(lastTimestamp);
      }
    } else {
      sequence = 0L;
    }

    lastTimestamp = timestamp;

    return ((timestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
        | (machineId << MACHINE_ID_SHIFT)
        | sequence;
  }

  private long waitForNextMillis(long currentTimestamp) {
    long timestamp = System.currentTimeMillis();
    while (timestamp <= currentTimestamp) {
      timestamp = System.currentTimeMillis();
    }
    return timestamp;
  }
}
