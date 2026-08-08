package com.thaman.urlshortener.entities;

import jakarta.validation.constraints.NotBlank;

public record PostBody(@NotBlank(message = "url must not be blank") String url) {}
