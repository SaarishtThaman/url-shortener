package com.thaman.urlshortener.controllers;

import com.thaman.urlshortener.entities.PostBody;
import com.thaman.urlshortener.services.URLShortenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@RestController
public class URLShortenerController {
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

  @PostMapping
  public ResponseEntity<String> registerUrl(@RequestBody PostBody postBody) {
    if (!isValidUrl(postBody.getUrl())) {
      return ResponseEntity.badRequest().body("Invalid URL");
    }
    return ResponseEntity.ok(urlShortenerService.registerUrl(postBody.getUrl()));
  }

  @GetMapping(path = "/{code}")
  ResponseEntity<Object> getUrl(@PathVariable("code") String code) {
    String url = urlShortenerService.lookupByShortCode(code);
    if (url != null) {
      return ResponseEntity.status(HttpStatus.FOUND)
              .location(URI.create(url))
              .build();
    }
    return ResponseEntity.notFound().build();
  }
}
