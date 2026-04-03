package com.thaman.urlshortener.repositories;

import com.thaman.urlshortener.entities.URLMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface URLMappingRepository extends JpaRepository<URLMapping, Long> {
    Optional<URLMapping> findByOriginalUrl(String originalUrl);
}
