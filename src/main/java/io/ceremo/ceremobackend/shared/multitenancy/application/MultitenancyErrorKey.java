package io.ceremo.ceremobackend.shared.multitenancy.application;

import io.ceremo.ceremobackend.shared.error.domain.ErrorKey;

public enum MultitenancyErrorKey implements ErrorKey {
  NO_TENANT_CONTEXT("no-tenant-context"),
  TENANT_SUSPENDED("tenant-suspended");

  private final String key;

  MultitenancyErrorKey(String key) {
    this.key = key;
  }

  @Override
  public String get() {
    return key;
  }
}
