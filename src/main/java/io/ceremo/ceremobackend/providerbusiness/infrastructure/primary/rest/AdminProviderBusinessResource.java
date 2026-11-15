package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest;

import io.ceremo.ceremobackend.providerbusiness.application.port.in.ProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.request.RestProviderBusinessStatusRequest;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response.RestProviderBusinessResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/admin/provider-businesses")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Platform admin", description = "Staff operations; require ROLE_ADMIN, MFA and a recent sign-in")
public class AdminProviderBusinessResource {
  private final ProviderBusinesses businesses;

  public AdminProviderBusinessResource(ProviderBusinesses businesses) { this.businesses = businesses; }

  @PostMapping("/{businessId}/suspend")
  public ResponseEntity<RestProviderBusinessResponse> suspend(@PathVariable UUID businessId,
      @Valid @RequestBody RestProviderBusinessStatusRequest request) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RestProviderBusinessResponse.from(
        businesses.suspend(ProviderBusinessId.of(businessId), request.reason())));
  }

  @PostMapping("/{businessId}/reactivate")
  public ResponseEntity<RestProviderBusinessResponse> reactivate(@PathVariable UUID businessId,
      @Valid @RequestBody RestProviderBusinessStatusRequest request) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RestProviderBusinessResponse.from(
        businesses.reactivate(ProviderBusinessId.of(businessId), request.reason())));
  }
}
