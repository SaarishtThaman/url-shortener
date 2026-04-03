package com.thaman.urlshortener.services;

import com.thaman.urlshortener.entities.URLMapping;
import com.thaman.urlshortener.repositories.URLMappingRepository;
import com.thaman.urlshortener.utils.Base62Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class URLShortenerService {

  @Autowired URLMappingRepository repository;

  public String registerUrl(String url) {
    return repository
        .findByOriginalUrl(url)
        .map(existing -> Base62Encoder.encode(existing.getId()))
        .orElseGet(
            () -> {
              try {
                URLMapping saved = repository.save(new URLMapping(url));
                return Base62Encoder.encode(saved.getId());
              } catch (DataIntegrityViolationException e) {
                return repository
                    .findByOriginalUrl(url)
                    .map(existing -> Base62Encoder.encode(existing.getId()))
                    .orElseThrow();
              }
            });
  }

    @Cacheable(value = "urlCache", key = "#shortCode", unless = "#result == null")
    public String lookupByShortCode(String shortCode) {
        return repository.findById(Base62Encoder.decode(shortCode))
                .map(URLMapping::getOriginalUrl)
                .orElse(null);
    }
}
