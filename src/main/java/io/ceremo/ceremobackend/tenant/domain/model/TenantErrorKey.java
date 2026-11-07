package io.ceremo.ceremobackend.tenant.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.ErrorKey;

public enum TenantErrorKey implements ErrorKey {
  TENANT_NOT_FOUND("tenant-not-found"),
  TENANT_OWNER_REQUIRED("tenant-owner-required"),
  TENANT_MEMBER_EXISTS("tenant-member-exists"),
  TENANT_MEMBER_NOT_FOUND("tenant-member-not-found"),
  TENANT_OWNER_PROTECTED("tenant-owner-protected");

  private final String key;
  TenantErrorKey(String key) { this.key = key; }
  @Override public String get() { return key; }
}
