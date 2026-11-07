package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.primary;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantInfo;

@Component
@ConditionalOnProperty(name = "application.multitenancy.enabled", havingValue = "true")
public class TenantFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

  private static final String ORGANIZATION_CLAIM = "organization";
  private static final String TENANT_ID_ATTRIBUTE = "tenant_id";
  private static final String SUBSCRIPTION_TIER_ATTRIBUTE = "subscription_tier";
  private static final String MAX_LOCATIONS_ATTRIBUTE = "max_locations";
  private static final String DEFAULT_SUBSCRIPTION_TIER = "FREE";
  private static final int DEFAULT_MAX_LOCATIONS = 1;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {
    try {
      TenantContext.clear();
      extractTenantFromAuthentication().ifPresent(TenantContext::setCurrentTenant);
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private Optional<TenantInfo> extractTenantFromAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    return extractAttributesFromAuthentication(authentication)
        .flatMap(this::extractTenantFromAttributes);
  }

  private Optional<Map<String, Object>> extractAttributesFromAuthentication(
      Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtToken) {
      return Optional.of(jwtToken.getTokenAttributes());
    }
    if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
      OAuth2User principal = oauthToken.getPrincipal();
      if (principal == null) {
        return Optional.empty();
      }
      return Optional.of(principal.getAttributes());
    }
    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  private Optional<TenantInfo> extractTenantFromAttributes(Map<String, Object> attributes) {
    Object organizationClaim = attributes.get(ORGANIZATION_CLAIM);
    if (organizationClaim == null) {
      log.debug("No organization claim found in token");
      return Optional.empty();
    }

    try {
      Map<String, Object> orgMap = (Map<String, Object>) organizationClaim;
      if (orgMap.size() != 1) {
        log.debug("Exactly one organization is required for tenant context");
        return Optional.empty();
      }
      // Keycloak format: key is org name, value is org data
      Map.Entry<String, Object> firstOrg = orgMap.entrySet().iterator().next();
      String orgName = firstOrg.getKey();

      if (!(firstOrg.getValue() instanceof Map)) {
        log.debug("Organization data is not a map");
        return Optional.empty();
      }

      Map<String, Object> orgData = (Map<String, Object>) firstOrg.getValue();
      String tenantIdStr = extractTenantId(orgData);

      if (tenantIdStr == null) {
        log.debug("No tenant_id found in organization data");
        return Optional.empty();
      }

      TenantId tenantId = TenantId.of(UUID.fromString(tenantIdStr));
      TenantInfo tenantInfo = createTenantInfo(tenantId, orgName, orgData);

      log.debug("Extracted tenant context: {}", tenantId);
      return Optional.of(tenantInfo);
    } catch (ClassCastException | IllegalArgumentException e) {
      log.warn("Invalid organization claim; tenant context was not set");
      return Optional.empty();
    }
  }

  private String extractTenantId(Map<String, Object> orgData) {
    Object tenantIdValue = orgData.get(TENANT_ID_ATTRIBUTE);
    if (tenantIdValue instanceof List<?> list && !list.isEmpty()) {
      return String.valueOf(list.getFirst());
    }
    if (tenantIdValue != null) {
      return tenantIdValue.toString();
    }
    return null;
  }

  private TenantInfo createTenantInfo(
      TenantId tenantId, String organizationName, Map<String, Object> orgData) {
    String subscriptionTier =
        extractStringAttribute(orgData, SUBSCRIPTION_TIER_ATTRIBUTE, DEFAULT_SUBSCRIPTION_TIER);
    int maxLocations = extractIntAttribute(orgData, MAX_LOCATIONS_ATTRIBUTE, DEFAULT_MAX_LOCATIONS);

    return new TenantInfo(tenantId, organizationName, subscriptionTier, maxLocations);
  }

  private String extractStringAttribute(
      Map<String, Object> attributes, String key, String defaultValue) {
    if (attributes == null) {
      return defaultValue;
    }
    Object value = attributes.get(key);
    if (value instanceof List<?> list && !list.isEmpty()) {
      return String.valueOf(list.getFirst());
    }
    return value != null ? String.valueOf(value) : defaultValue;
  }

  private int extractIntAttribute(Map<String, Object> attributes, String key, int defaultValue) {
    String strValue = extractStringAttribute(attributes, key, String.valueOf(defaultValue));
    try {
      return Integer.parseInt(strValue);
    } catch (NumberFormatException _) {
      return defaultValue;
    }
  }
}
