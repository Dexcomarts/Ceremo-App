package io.ceremo.ceremobackend.tenant.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.multitenancy.application.TenantSuspendedException;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import io.ceremo.ceremobackend.tenant.domain.model.*;
import io.ceremo.ceremobackend.tenant.domain.repository.TenantRepository;
import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import io.ceremo.ceremobackend.account.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantServiceTest {
  private static final UUID OWNER = UUID.fromString("01940d11-7390-7000-8000-000000000001");
  private static final UUID MEMBER = UUID.fromString("01940d11-7390-7000-8000-000000000002");
  private static final TenantId ID = TenantId.of(UUID.fromString("01940d11-7390-7000-8000-000000000003"));
  private final Accounts accounts = mock(Accounts.class);
  private final UserDirectory users = mock(UserDirectory.class);
  private final TenantRepository repository = mock(TenantRepository.class);
  private final TenantService service = new TenantService(accounts, users, repository);
  private final Tenant tenant = Tenant.create(ID, "Ama Catering", OWNER);

  @BeforeEach
  void caller() {
    User user = User.register(OWNER, "Ama", "terms-v1", Instant.EPOCH).activate();
    when(accounts.requireActiveAccount()).thenReturn(user);
    when(accounts.requireAdministrator()).thenReturn(user);
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.of(tenant));
    when(repository.findById(ID)).thenReturn(Optional.of(tenant));
  }

  @Test
  void createsAWorkspaceWithoutGrantingCommercialOrStaffEntitlements() {
    Tenant created = service.create("New business");
    assertThat(created.ownerId()).isEqualTo(OWNER);
    assertThat(created.id().get().version()).isEqualTo(7);
    assertThat(created.status()).isEqualTo(TenantStatus.ACTIVE);
    verify(repository).create(created);
    verify(repository).recordChange(created.id(), OWNER, "CREATED", "Provider workspace created");
    assertThatThrownBy(() -> service.create(" ")).isInstanceOf(AssertionException.class);
  }

  @Test
  void grantsOnlyAnInvitationUntilTheAuthenticatedRecipientAccepts() {
    when(users.isRegistered(MEMBER)).thenReturn(true);
    service.invite(ID, MEMBER, MemberRole.MANAGER);
    verify(repository).invite(ID, new TenantMember(MEMBER, MemberRole.MANAGER, MembershipStatus.INVITED));
    verify(repository).recordChange(ID, OWNER, "MEMBER_INVITED", MEMBER.toString());
    when(repository.findForInvitee(ID, OWNER)).thenReturn(Optional.of(tenant));
    assertThat(service.accept(ID)).isSameAs(tenant);
    verify(repository).accept(ID, OWNER);
    when(repository.findForInvitee(ID, OWNER)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.accept(ID)).isInstanceOf(CeremoException.class);
    when(users.isRegistered(MEMBER)).thenReturn(false);
    assertThatThrownBy(() -> service.invite(ID, MEMBER, MemberRole.FINANCE)).isInstanceOf(CeremoException.class);
    assertThatThrownBy(() -> service.invite(ID, OWNER, MemberRole.MANAGER)).isInstanceOf(CeremoException.class);
    assertThatThrownBy(() -> service.invite(ID, null, MemberRole.MANAGER)).isInstanceOf(AssertionException.class);
    when(users.isRegistered(MEMBER)).thenReturn(true);
    assertThatThrownBy(() -> service.invite(ID, MEMBER, null)).isInstanceOf(AssertionException.class);
  }

  @Test
  void authorisesEachCallAndRetainsTheOwnerDuringSuspension() {
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.of(tenant.suspend()));
    assertThat(service.get(ID).status()).isEqualTo(TenantStatus.SUSPENDED);
    assertThatThrownBy(() -> service.invite(ID, MEMBER, MemberRole.MANAGER)).isInstanceOf(TenantSuspendedException.class);
    when(repository.findForInvitee(ID, OWNER)).thenReturn(Optional.of(tenant.suspend()));
    assertThatThrownBy(() -> service.accept(ID)).isInstanceOf(TenantSuspendedException.class);
    service.revoke(ID, MEMBER);
    verify(repository).revoke(ID, MEMBER);
    assertThatThrownBy(() -> service.revoke(ID, OWNER)).isInstanceOf(CeremoException.class);
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.of(Tenant.create(ID, "Other owner", MEMBER)));
    assertThatThrownBy(() -> service.revoke(ID, MEMBER)).isInstanceOf(CeremoException.class);
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(ID)).isInstanceOf(CeremoException.class);
    when(accounts.requireActiveAccount()).thenThrow(new IllegalStateException("Account denied"));
    clearInvocations(repository);
    assertThatThrownBy(() -> service.create("Business")).isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void returnsOnlyTheCallersPaginatedWorkspacesAndOwnerAuthorisedTeam() {
    CeremoPageable page = new CeremoPageable(0, 10);
    when(repository.findForUser(OWNER, page)).thenReturn(CeremoPage.singlePage(List.of(tenant)));
    when(repository.members(ID, page)).thenReturn(CeremoPage.singlePage(List.of()));
    assertThat(service.mine(page).content()).containsExactly(tenant);
    assertThat(service.members(ID, page).content()).isEmpty();
  }

  @Test
  void requiresStaffAccessAndAReasonForStatusChangesIncludingDirectCalls() {
    assertThat(service.suspend(ID, "Investigation").status()).isEqualTo(TenantStatus.SUSPENDED);
    assertThat(service.reactivate(ID, "Review complete").status()).isEqualTo(TenantStatus.ACTIVE);
    verify(repository).recordChange(ID, OWNER, "SUSPENDED", "Investigation");
    verify(repository).recordChange(ID, OWNER, "REACTIVATED", "Review complete");
    assertThatThrownBy(() -> service.suspend(ID, " ")).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> service.reactivate(ID, "x".repeat(501))).isInstanceOf(AssertionException.class);
    when(repository.findById(ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.suspend(ID, "Review")).isInstanceOf(CeremoException.class);
    assertThatThrownBy(() -> service.reactivate(ID, "Review")).isInstanceOf(CeremoException.class);
    when(accounts.requireAdministrator()).thenThrow(new IllegalStateException("Staff denied"));
    clearInvocations(repository);
    assertThatThrownBy(() -> service.suspend(ID, "Review")).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> service.reactivate(ID, "Review")).isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void validatesBoundariesAndKeepsErrorKeysStable() {
    assertThatThrownBy(() -> new TenantService(null, users, repository)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new TenantService(accounts, null, repository)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new TenantService(accounts, users, null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Tenant(ID, "x".repeat(161), OWNER, TenantStatus.ACTIVE)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new Tenant(null, "Name", OWNER, TenantStatus.ACTIVE)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new Tenant(ID, "Name", null, TenantStatus.ACTIVE)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new Tenant(ID, "Name", OWNER, null)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new TenantMember(MEMBER, MemberRole.MANAGER, null)).isInstanceOf(AssertionException.class);
    assertThat(java.util.Arrays.stream(TenantErrorKey.values()).map(TenantErrorKey::get).toList())
        .doesNotHaveDuplicates().allMatch(key -> key.startsWith("tenant-"));
  }
}
