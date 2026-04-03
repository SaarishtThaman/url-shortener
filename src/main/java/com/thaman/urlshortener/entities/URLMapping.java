package com.thaman.urlshortener.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "url_mappings")
public class URLMapping {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String originalUrl;

  public URLMapping() {}

  public URLMapping(String originalUrl) {
    this.originalUrl = originalUrl;
  }

  public Long getId() {
    return id;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }
}
