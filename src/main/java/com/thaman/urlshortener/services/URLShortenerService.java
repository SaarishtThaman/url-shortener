package com.thaman.urlshortener.services;

import com.thaman.urlshortener.entities.URLMapping;
import com.thaman.urlshortener.repositories.URLMappingRepository;
import com.thaman.urlshortener.utils.ShardContext;
import com.thaman.urlshortener.utils.ShardRouter;
import com.thaman.urlshortener.utils.ShortCodeCodec;
import com.thaman.urlshortener.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class URLShortenerService {

  @Autowired URLMappingRepository repository;
  @Autowired SnowflakeIdGenerator idGenerator;
  @Autowired ShortCodeCodec shortCodeCodec;
  @Autowired ShardRouter shardRouter;

  public String registerUrl(String url) {
    long id = idGenerator.nextId();
    ShardContext.setShard(shardRouter.shardFor(id));
    try {
      URLMapping saved = repository.save(new URLMapping(id, url));
      return shortCodeCodec.encode(saved.getId());
    } finally {
      ShardContext.clear();
    }
  }

    @Cacheable(value = "urlCache", key = "#shortCode", sync = true)
    public String lookupByShortCode(String shortCode) {
        long id = shortCodeCodec.decode(shortCode);
        ShardContext.setShard(shardRouter.shardFor(id));
        try {
            return repository.findById(id)
                    .map(URLMapping::getOriginalUrl)
                    .orElse(null);
        } finally {
            ShardContext.clear();
        }
    }
}
