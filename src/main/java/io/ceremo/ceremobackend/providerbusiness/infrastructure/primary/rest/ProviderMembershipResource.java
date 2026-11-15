package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest;

import io.ceremo.ceremobackend.providerbusiness.application.port.in.ProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response.RestProviderBusinessResponse;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response.RestProviderMembershipResponse;
import io.ceremo.ceremobackend.shared.pagination.infrastructure.primary.RestCeremoPage;
import io.ceremo.ceremobackend.shared.pagination.infrastructure.primary.RestCeremoPageable;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in person's own invitations and team memberships; the person is always the caller. */
@RestController
@RequestMapping("/api/v1/users/me/provider-memberships")
@Tag(name = "My account", description = "The signed-in person's ceremo account, session and team invitations")
public class ProviderMembershipResource {
  private final ProviderBusinesses businesses;

  public ProviderMembershipResource(ProviderBusinesses businesses) { this.businesses = businesses; }

  @GetMapping
  public ResponseEntity<RestCeremoPage<RestProviderMembershipResponse>> mine(@Valid RestCeremoPageable pageable) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
        RestCeremoPage.from(businesses.myMemberships(pageable.toPageable()), RestProviderMembershipResponse::from));
  }

  @PostMapping("/{businessId}/accept")
  public ResponseEntity<RestProviderBusinessResponse> accept(@PathVariable UUID businessId) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(RestProviderBusinessResponse.from(businesses.accept(ProviderBusinessId.of(businessId))));
  }
}
