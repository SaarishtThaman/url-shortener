package com.thaman.urlshortener.utils;

import org.sqids.Sqids;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShortCodeCodec {

  private final Sqids sqids = Sqids.builder().build();

  public String encode(long id) {
    return sqids.encode(List.of(id));
  }

  public long decode(String code) {
    List<Long> numbers = sqids.decode(code);
    if (numbers.isEmpty()) {
      throw new IllegalArgumentException("Invalid code: " + code);
    }
    return numbers.get(0);
  }
}
