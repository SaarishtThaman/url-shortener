package com.thaman.urlshortener.utils;

/**
 * Superseded by {@link ShortCodeCodec}, which scrambles IDs via Sqids instead of encoding them
 * sequentially. Kept unused for reference.
 */
@Deprecated
public class Base62Encoder {

  private static final String CHARACTERS =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  public static String encode(long id) {
    StringBuilder encoded = new StringBuilder();
    while (id > 0) {
      long rem = id % 62;
      encoded.append(CHARACTERS.charAt((int) rem));
      id = id / 62;
    }
    return encoded.reverse().toString();
  }

  public static long decode(String code) {
    if (code == null || code.isBlank()) throw new IllegalArgumentException("Invalid code");
    long result = 0;
    for (char c : code.toCharArray()) {
      int index = CHARACTERS.indexOf(c);
      if (index == -1) throw new IllegalArgumentException("Invalid character: " + c);
      result = result * 62 + index;
    }
    return result;
  }
}
