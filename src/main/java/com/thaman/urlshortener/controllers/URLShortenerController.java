package com.thaman.urlshortener.controllers;

import com.thaman.urlshortener.entities.PostBody;
import com.thaman.urlshortener.services.URLShortenerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
public class URLShortenerController {
  private static final Logger log = LoggerFactory.getLogger(URLShortenerController.class);

  @Autowired URLShortenerService urlShortenerService;

  private boolean isValidUrl(String url) {
    if (url == null || url.isBlank()) return false;
    try {
      URI uri = new URI(url);
      return uri.getScheme() != null &&
              (uri.getScheme().equals("http") || uri.getScheme().equals("https")) &&
              uri.getHost() != null;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<Resource> index() {
    return ResponseEntity.ok().body(new ClassPathResource("static/index.html"));
  }

  @PostMapping
  public ResponseEntity<String> registerUrl(@Valid @RequestBody PostBody postBody) {
    log.info("POST / - registering url={}", postBody.url());
    if (!isValidUrl(postBody.url())) {
      return ResponseEntity.badRequest().body("Invalid URL");
    }
    return ResponseEntity.ok(urlShortenerService.registerUrl(postBody.url()));
  }

  @GetMapping(path = "/{code}")
  ResponseEntity<Object> getUrl(@PathVariable("code") String code) {
    log.info("GET /{} - lookup", code);
    String url = urlShortenerService.lookupByShortCode(code);
    if (url != null) {
      return ResponseEntity.status(HttpStatus.FOUND)
              .location(URI.create(url))
              .build();
    }
    return ResponseEntity.notFound().build();
  }
}
