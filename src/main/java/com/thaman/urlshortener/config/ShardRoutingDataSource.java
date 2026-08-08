package com.thaman.urlshortener.config;

import com.thaman.urlshortener.utils.ShardContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class ShardRoutingDataSource extends AbstractRoutingDataSource {

  @Override
  protected Object determineCurrentLookupKey() {
    return ShardContext.getShard();
  }
}
