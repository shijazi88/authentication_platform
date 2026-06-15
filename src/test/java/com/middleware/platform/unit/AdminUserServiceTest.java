package com.middleware.platform.unit;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;
import com.middleware.platform.iam.dto.CreateAdminUserRequest;
import com.middleware.platform.iam.dto.UpdateAdminUserRequest;
import com.middleware.platform.iam.repo.AdminUserRepository;
import com.middleware.platform.iam.security.JwtService;
import com.middleware.platform.iam.security.SecurityProperties;
import com.middleware.platform.iam.service.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock AdminUserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock SecurityProperties securityProperties;

    @InjectMocks AdminUserService service;

    private static AdminUser user(AdminRole role, boolean active, String email) {
        return AdminUser.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .role(role)
                .active(active)
                .build();
    }

    @Test
    @DisplayName("create rejects a duplicate email")
    void createDuplicateEmail() {
        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);
        var req = new CreateAdminUserRequest("dup@x.com", "password1", "Dup", AdminRole.FINANCE);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("create rejects the legacy AUDITOR role")
    void createRejectsAuditor() {
        var req = new CreateAdminUserRequest("a@x.com", "password1", "A", AdminRole.AUDITOR);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("create normalizes email to lowercase and hashes the password")
    void createNormalizes() {
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        var req = new CreateAdminUserRequest("New@X.com", "password1", " Ops ", AdminRole.PLATFORM_OPS);

        var resp = service.create(req);

        assertThat(resp.email()).isEqualTo("new@x.com");
        assertThat(resp.displayName()).isEqualTo("Ops");
        assertThat(resp.role()).isEqualTo(AdminRole.PLATFORM_OPS);
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("hashed") && u.isActive()));
    }

    @Test
    @DisplayName("demoting the last active super-admin is blocked")
    void cannotDemoteLastSuperAdmin() {
        AdminUser su = user(AdminRole.SUPER_ADMIN, true, "su@x.com");
        when(userRepository.findById(su.getId())).thenReturn(Optional.of(su));
        when(userRepository.countByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(su.getId(),
                new UpdateAdminUserRequest("SU", AdminRole.FINANCE)))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("demoting a super-admin is allowed when another active one exists")
    void canDemoteWhenAnotherSuperAdminExists() {
        AdminUser su = user(AdminRole.SUPER_ADMIN, true, "su@x.com");
        when(userRepository.findById(su.getId())).thenReturn(Optional.of(su));
        when(userRepository.countByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(2L);

        var resp = service.update(su.getId(), new UpdateAdminUserRequest("SU", AdminRole.PLATFORM_OPS));

        assertThat(resp.role()).isEqualTo(AdminRole.PLATFORM_OPS);
    }

    @Test
    @DisplayName("you cannot deactivate your own account")
    void cannotDeactivateSelf() {
        AdminUser me = user(AdminRole.SUPER_ADMIN, true, "me@x.com");
        when(userRepository.findById(me.getId())).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> service.setActive(me.getId(), false, "ME@x.com"))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("deactivating the last active super-admin is blocked")
    void cannotDeactivateLastSuperAdmin() {
        AdminUser su = user(AdminRole.SUPER_ADMIN, true, "su@x.com");
        when(userRepository.findById(su.getId())).thenReturn(Optional.of(su));
        when(userRepository.countByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.setActive(su.getId(), false, "other@x.com"))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("a non-super-admin can be deactivated freely")
    void canDeactivateNonSuperAdmin() {
        AdminUser ops = user(AdminRole.PLATFORM_OPS, true, "ops@x.com");
        when(userRepository.findById(ops.getId())).thenReturn(Optional.of(ops));

        var resp = service.setActive(ops.getId(), false, "other@x.com");

        assertThat(resp.active()).isFalse();
    }
}
