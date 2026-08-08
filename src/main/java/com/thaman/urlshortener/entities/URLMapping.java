package com.thaman.urlshortener.entities;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "url_mappings")
public class URLMapping implements Persistable<Long> {
  @Id
  private Long id;

  @Column(nullable = false)
  private String originalUrl;

  public URLMapping() {}

  public URLMapping(Long id, String originalUrl) {
    this.id = id;
    this.originalUrl = originalUrl;
  }

  public Long getId() {
    return id;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  @Override
  public boolean isNew() {
    return true;
  }
}
