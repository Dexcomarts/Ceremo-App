package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest;

import io.ceremo.ceremobackend.providerbusiness.application.port.in.ProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.request.RestCreateProviderBusinessRequest;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.request.RestInviteMemberRequest;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response.RestProviderBusinessMemberResponse;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response.RestProviderBusinessResponse;
import io.ceremo.ceremobackend.shared.pagination.infrastructure.primary.RestCeremoPage;
import io.ceremo.ceremobackend.shared.pagination.infrastructure.primary.RestCeremoPageable;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** Owner and team operations on a provider business. Membership and ownership are checked in each use case. */
@RestController
@RequestMapping("/api/v1/provider-businesses")
@Tag(name = "Provider business", description = "Register a provider business and manage its owner-led team")
public class ProviderBusinessResource {
  private final ProviderBusinesses businesses;

  public ProviderBusinessResource(ProviderBusinesses businesses) { this.businesses = businesses; }

  @PostMapping
  public ResponseEntity<RestProviderBusinessResponse> create(@Valid @RequestBody RestCreateProviderBusinessRequest request) {
    var response = RestProviderBusinessResponse.from(businesses.create(request.name()));
    return ResponseEntity.created(URI.create("/api/v1/provider-businesses/" + response.id()))
        .cacheControl(CacheControl.noStore()).body(response);
  }

  @GetMapping
  public ResponseEntity<RestCeremoPage<RestProviderBusinessResponse>> mine(@Valid RestCeremoPageable pageable) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
        RestCeremoPage.from(businesses.mine(pageable.toPageable()), RestProviderBusinessResponse::from));
  }

  @GetMapping("/{businessId}")
  public ResponseEntity<RestProviderBusinessResponse> get(@PathVariable UUID businessId) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(RestProviderBusinessResponse.from(businesses.get(ProviderBusinessId.of(businessId))));
  }

  @GetMapping("/{businessId}/members")
  public ResponseEntity<RestCeremoPage<RestProviderBusinessMemberResponse>> members(
      @PathVariable UUID businessId, @Valid RestCeremoPageable pageable) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RestCeremoPage.from(
        businesses.members(ProviderBusinessId.of(businessId), pageable.toPageable()),
        RestProviderBusinessMemberResponse::from));
  }

  @DeleteMapping("/{businessId}/members/{userId}")
  public ResponseEntity<Void> removeMember(@PathVariable UUID businessId, @PathVariable UUID userId) {
    businesses.removeMember(ProviderBusinessId.of(businessId), userId);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @GetMapping("/{businessId}/invitations")
  public ResponseEntity<RestCeremoPage<RestProviderBusinessMemberResponse>> invitations(
      @PathVariable UUID businessId, @Valid RestCeremoPageable pageable) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RestCeremoPage.from(
        businesses.invitations(ProviderBusinessId.of(businessId), pageable.toPageable()),
        RestProviderBusinessMemberResponse::from));
  }

  @PostMapping("/{businessId}/invitations")
  public ResponseEntity<Void> invite(@PathVariable UUID businessId, @Valid @RequestBody RestInviteMemberRequest request) {
    businesses.invite(ProviderBusinessId.of(businessId), request.userId(), request.role());
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @DeleteMapping("/{businessId}/invitations/{userId}")
  public ResponseEntity<Void> cancelInvitation(@PathVariable UUID businessId, @PathVariable UUID userId) {
    businesses.cancelInvitation(ProviderBusinessId.of(businessId), userId);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
}
