package io.ceremo.ceremobackend.tenant.infrastructure.primary.rest;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.tenant.application.port.in.TenantAccounts;
import io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.request.RestTenantStatusRequest;
import io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.response.RestTenantResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tenants")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminTenantResource {
  private final TenantAccounts tenants;

  public AdminTenantResource(TenantAccounts tenants) { this.tenants = tenants; }

  @PostMapping("/{tenantId}/suspend")
  public ResponseEntity<RestTenantResponse> suspend(@PathVariable UUID tenantId, @Valid @RequestBody RestTenantStatusRequest request) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(RestTenantResponse.from(tenants.suspend(TenantId.of(tenantId), request.reason())));
  }

  @PostMapping("/{tenantId}/reactivate")
  public ResponseEntity<RestTenantResponse> reactivate(@PathVariable UUID tenantId, @Valid @RequestBody RestTenantStatusRequest request) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(RestTenantResponse.from(tenants.reactivate(TenantId.of(tenantId), request.reason())));
  }
}
