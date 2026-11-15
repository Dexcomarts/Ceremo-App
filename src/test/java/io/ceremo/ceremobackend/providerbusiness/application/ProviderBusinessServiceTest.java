package io.ceremo.ceremobackend.providerbusiness.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.providerbusiness.domain.model.*;
import io.ceremo.ceremobackend.providerbusiness.domain.repository.ProviderBusinessRepository;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Adapted from the former TenantServiceTest; behaviour for members versus invitations is new. */
class ProviderBusinessServiceTest {
  private static final UUID OWNER = UUID.fromString("01940d11-7390-7000-8000-000000000001");
  private static final UUID MEMBER = UUID.fromString("01940d11-7390-7000-8000-000000000002");
  private static final ProviderBusinessId ID = ProviderBusinessId.of(UUID.fromString("01940d11-7390-7000-8000-000000000003"));
  private final Accounts accounts = mock(Accounts.class);
  private final UserDirectory users = mock(UserDirectory.class);
  private final ProviderBusinessRepository repository = mock(ProviderBusinessRepository.class);
  private final ProviderBusinessService service = new ProviderBusinessService(accounts, users, repository);
  private final ProviderBusiness business = ProviderBusiness.create(ID, "Ama Catering", OWNER);

  @BeforeEach
  void caller() {
    User user = User.register(OWNER, "Ama", "terms-v1", Instant.EPOCH).activate();
    when(accounts.requireActiveAccount()).thenReturn(user);
    when(accounts.requireAdministrator()).thenReturn(user);
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.of(business));
    when(repository.findById(ID)).thenReturn(Optional.of(business));
  }

  @Test
  void createsABusinessWithoutGrantingCommercialOrStaffEntitlements() {
    ProviderBusiness created = service.create("New business");
    assertThat(created.ownerId()).isEqualTo(OWNER);
    assertThat(created.id().value().version()).isEqualTo(7);
    assertThat(created.status()).isEqualTo(ProviderBusinessStatus.ACTIVE);
    assertThat(created.ownerMember())
        .isEqualTo(new ProviderBusinessMember(OWNER, MemberRole.OWNER, MembershipStatus.ACTIVE));
    verify(repository).create(created);
    verify(repository).recordChange(created.id(), OWNER, "CREATED", "Provider business created");
    assertThatThrownBy(() -> service.create(" ")).isInstanceOf(AssertionException.class);
  }

  @Test
  void grantsOnlyAnInvitationUntilTheAuthenticatedRecipientAccepts() {
    when(users.isRegistered(MEMBER)).thenReturn(true);
    service.invite(ID, MEMBER, MemberRole.MANAGER);
    verify(repository).invite(ID, new ProviderBusinessMember(MEMBER, MemberRole.MANAGER, MembershipStatus.INVITED));
    verify(repository).recordChange(ID, OWNER, "MEMBER_INVITED", MEMBER.toString());
    when(repository.findForInvitee(ID, OWNER)).thenReturn(Optional.of(business));
    assertThat(service.accept(ID)).isSameAs(business);
    verify(repository, never()).accept(ID, OWNER);
    verify(repository, never()).recordChange(ID, OWNER, "INVITATION_ACCEPTED", OWNER.toString());
    ProviderBusiness invitedTo = ProviderBusiness.create(ID, "Kojo Photos", MEMBER);
    when(repository.findForInvitee(ID, OWNER)).thenReturn(Optional.of(invitedTo));
    assertThat(service.accept(ID)).isSameAs(invitedTo);
    verify(repository).accept(ID, OWNER);
    verify(repository).recordChange(ID, OWNER, "INVITATION_ACCEPTED", OWNER.toString());
    when(repository.findForInvitee(ID, OWNER)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.accept(ID)).isInstanceOf(CeremoException.class);
    when(users.isRegistered(MEMBER)).thenReturn(false);
    assertThatThrownBy(() -> service.invite(ID, MEMBER, MemberRole.FINANCE)).isInstanceOf(CeremoException.class);
    assertThatThrownBy(() -> service.invite(ID, OWNER, MemberRole.MANAGER)).isInstanceOf(CeremoException.class);
    assertThatThrownBy(() -> service.invite(ID, null, MemberRole.MANAGER)).isInstanceOf(AssertionException.class);
    when(users.isRegistered(MEMBER)).thenReturn(true);
    assertThatThrownBy(() -> service.invite(ID, MEMBER, null)).isInstanceOf(AssertionException.class);
    clearInvocations(repository);
    assertThatThrownBy(() -> service.invite(ID, MEMBER, MemberRole.OWNER))
        .isInstanceOfSatisfying(CeremoException.class,
            error -> assertThat(error.key()).isEqualTo(ProviderBusinessErrorKey.PROVIDER_BUSINESS_OWNER_PROTECTED));
    verify(repository, never()).invite(any(), any());
  }

  @Test
  void authorisesEachCallAndRetainsTheOwnerDuringSuspension() {
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.of(business.suspend()));
    assertThat(service.get(ID).status()).isEqualTo(ProviderBusinessStatus.SUSPENDED);
    assertThatThrownBy(() -> service.invite(ID, MEMBER, MemberRole.MANAGER))
        .isInstanceOfSatisfying(CeremoException.class,
            error -> assertThat(error.key()).isEqualTo(ProviderBusinessErrorKey.PROVIDER_BUSINESS_SUSPENDED));
    when(repository.findForInvitee(ID, OWNER)).thenReturn(Optional.of(business.suspend()));
    assertThatThrownBy(() -> service.accept(ID)).isInstanceOf(CeremoException.class);
    service.removeMember(ID, MEMBER);
    verify(repository).remove(ID, MEMBER, MembershipStatus.ACTIVE);
    verify(repository).recordChange(ID, OWNER, "MEMBER_REVOKED", MEMBER.toString());
    service.cancelInvitation(ID, MEMBER);
    verify(repository).remove(ID, MEMBER, MembershipStatus.INVITED);
    verify(repository).recordChange(ID, OWNER, "INVITATION_CANCELLED", MEMBER.toString());
    assertThatThrownBy(() -> service.removeMember(ID, OWNER)).isInstanceOf(CeremoException.class);
    assertThatThrownBy(() -> service.cancelInvitation(ID, OWNER)).isInstanceOf(CeremoException.class);
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.of(ProviderBusiness.create(ID, "Other owner", MEMBER)));
    assertThatThrownBy(() -> service.removeMember(ID, MEMBER))
        .isInstanceOfSatisfying(CeremoException.class,
            error -> assertThat(error.key()).isEqualTo(ProviderBusinessErrorKey.PROVIDER_BUSINESS_OWNER_REQUIRED));
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(ID)).isInstanceOf(CeremoException.class);
    when(accounts.requireActiveAccount()).thenThrow(new IllegalStateException("Account denied"));
    clearInvocations(repository);
    assertThatThrownBy(() -> service.create("Business")).isInstanceOf(IllegalStateException.class);
    CeremoPageable page = new CeremoPageable(0, 10);
    assertThatThrownBy(() -> service.myMemberships(page)).isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void separatesActiveMembersFromPendingInvitationsAndListsOnlyTheCallersOwnMemberships() {
    CeremoPageable page = new CeremoPageable(0, 10);
    ProviderBusinessMember active = new ProviderBusinessMember(MEMBER, MemberRole.FINANCE, MembershipStatus.ACTIVE);
    ProviderBusinessMember invited = new ProviderBusinessMember(MEMBER, MemberRole.MANAGER, MembershipStatus.INVITED);
    ProviderMembership membership = new ProviderMembership(ID, "Ama Catering", ProviderBusinessStatus.ACTIVE,
        MemberRole.MANAGER, MembershipStatus.INVITED);
    when(repository.findForUser(OWNER, page)).thenReturn(CeremoPage.singlePage(List.of(business)));
    when(repository.members(ID, MembershipStatus.ACTIVE, page)).thenReturn(CeremoPage.singlePage(List.of(active)));
    when(repository.members(ID, MembershipStatus.INVITED, page)).thenReturn(CeremoPage.singlePage(List.of(invited)));
    when(repository.memberships(OWNER, page)).thenReturn(CeremoPage.singlePage(List.of(membership)));
    assertThat(service.mine(page).content()).containsExactly(business);
    assertThat(service.members(ID, page).content()).containsExactly(active);
    assertThat(service.invitations(ID, page).content()).containsExactly(invited);
    assertThat(service.myMemberships(page).content()).containsExactly(membership);
    when(repository.findForMember(ID, OWNER)).thenReturn(Optional.of(ProviderBusiness.create(ID, "Other owner", MEMBER)));
    assertThatThrownBy(() -> service.invitations(ID, page)).isInstanceOf(CeremoException.class);
  }

  @Test
  void requiresStaffAccessAndAReasonForStatusChangesIncludingDirectCalls() {
    assertThat(service.suspend(ID, "Investigation").status()).isEqualTo(ProviderBusinessStatus.SUSPENDED);
    assertThat(service.reactivate(ID, "Review complete").status()).isEqualTo(ProviderBusinessStatus.ACTIVE);
    verify(repository).recordChange(ID, OWNER, "SUSPENDED", "Investigation");
    verify(repository).recordChange(ID, OWNER, "REACTIVATED", "Review complete");
    assertThatThrownBy(() -> service.suspend(ID, " ")).isInstanceOf(AssertionException.class);
    String tooLongReason = "x".repeat(501);
    assertThatThrownBy(() -> service.reactivate(ID, tooLongReason)).isInstanceOf(AssertionException.class);
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
    assertThatThrownBy(() -> new ProviderBusinessService(null, users, repository)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ProviderBusinessService(accounts, null, repository)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ProviderBusinessService(accounts, users, null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ProviderBusinessId.of(null)).isInstanceOf(AssertionException.class);
    String tooLongName = "x".repeat(161);
    assertThatThrownBy(() -> new ProviderBusiness(ID, tooLongName, OWNER, ProviderBusinessStatus.ACTIVE))
        .isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new ProviderBusiness(null, "Name", OWNER, ProviderBusinessStatus.ACTIVE))
        .isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new ProviderBusiness(ID, "Name", null, ProviderBusinessStatus.ACTIVE))
        .isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new ProviderBusiness(ID, "Name", OWNER, null)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new ProviderBusinessMember(MEMBER, MemberRole.MANAGER, null)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new ProviderMembership(ID, " ", ProviderBusinessStatus.ACTIVE, MemberRole.MANAGER,
        MembershipStatus.ACTIVE)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new ProviderMembership(null, "Name", ProviderBusinessStatus.ACTIVE, MemberRole.MANAGER,
        MembershipStatus.ACTIVE)).isInstanceOf(AssertionException.class);
    assertThat(java.util.Arrays.stream(ProviderBusinessErrorKey.values()).map(ProviderBusinessErrorKey::get).toList())
        .doesNotHaveDuplicates().allMatch(key -> key.startsWith("provider-business-"));
  }
}
