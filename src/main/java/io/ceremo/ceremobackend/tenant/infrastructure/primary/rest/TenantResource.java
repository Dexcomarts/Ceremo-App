package io.ceremo.ceremobackend.tenant.infrastructure.primary.rest;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.pagination.infrastructure.primary.RestCeremoPage;
import io.ceremo.ceremobackend.shared.pagination.infrastructure.primary.RestCeremoPageable;
import io.ceremo.ceremobackend.tenant.application.port.in.TenantAccounts;
import io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.request.RestCreateTenantRequest;
import io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.request.RestInviteMemberRequest;
import io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.response.RestTenantMemberResponse;
import io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.response.RestTenantResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantResource {
  private final TenantAccounts tenants;

  public TenantResource(TenantAccounts tenants) { this.tenants = tenants; }

  @PostMapping
  public ResponseEntity<RestTenantResponse> create(@Valid @RequestBody RestCreateTenantRequest request) {
    var response = RestTenantResponse.from(tenants.create(request.name()));
    return ResponseEntity.created(URI.create("/api/v1/tenants/" + response.id()))
        .cacheControl(CacheControl.noStore()).body(response);
  }

  @GetMapping
  public ResponseEntity<RestCeremoPage<RestTenantResponse>> mine(@Valid RestCeremoPageable pageable) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
        RestCeremoPage.from(tenants.mine(pageable.toPageable()), RestTenantResponse::from));
  }

  @GetMapping("/{tenantId}")
  public ResponseEntity<RestTenantResponse> get(@PathVariable UUID tenantId) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(RestTenantResponse.from(tenants.get(TenantId.of(tenantId))));
  }

  @GetMapping("/{tenantId}/members")
  public ResponseEntity<RestCeremoPage<RestTenantMemberResponse>> members(
      @PathVariable UUID tenantId, @Valid RestCeremoPageable pageable) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
        RestCeremoPage.from(tenants.members(TenantId.of(tenantId), pageable.toPageable()), RestTenantMemberResponse::from));
  }

  @PostMapping("/{tenantId}/members")
  public ResponseEntity<Void> invite(@PathVariable UUID tenantId, @Valid @RequestBody RestInviteMemberRequest request) {
    tenants.invite(TenantId.of(tenantId), request.userId(), request.role());
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @PostMapping("/{tenantId}/membership/accept")
  public ResponseEntity<RestTenantResponse> accept(@PathVariable UUID tenantId) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(RestTenantResponse.from(tenants.accept(TenantId.of(tenantId))));
  }

  @DeleteMapping("/{tenantId}/members/{userId}")
  public ResponseEntity<Void> revoke(@PathVariable UUID tenantId, @PathVariable UUID userId) {
    tenants.revoke(TenantId.of(tenantId), userId);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
}
