package io.ceremo.ceremobackend.providerbusiness.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.ErrorKey;

public enum ProviderBusinessErrorKey implements ErrorKey {
  PROVIDER_BUSINESS_NOT_FOUND("provider-business-not-found"),
  PROVIDER_BUSINESS_SUSPENDED("provider-business-suspended"),
  PROVIDER_BUSINESS_OWNER_REQUIRED("provider-business-owner-required"),
  PROVIDER_BUSINESS_OWNER_PROTECTED("provider-business-owner-protected"),
  PROVIDER_BUSINESS_MEMBER_EXISTS("provider-business-member-exists"),
  PROVIDER_BUSINESS_MEMBER_NOT_FOUND("provider-business-member-not-found"),
  PROVIDER_BUSINESS_INVITATION_NOT_FOUND("provider-business-invitation-not-found");

  private final String key;
  ProviderBusinessErrorKey(String key) { this.key = key; }
  @Override public String get() { return key; }
}
