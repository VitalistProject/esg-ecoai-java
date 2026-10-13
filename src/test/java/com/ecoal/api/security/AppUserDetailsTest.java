package com.ecoal.api.security;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserDetailsTest {

    @Test
    void exposesUserIdentityForAuthentication() {
        AppUserDetails details = new AppUserDetails(user("admin"));

        assertThat(details.getId()).isEqualTo(5L);
        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(details.getEmail()).isEqualTo("alice@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.getCompanyId()).isEqualTo(9L);
        assertThat(details.getRole()).isEqualTo("admin");
    }

    @Test
    void mapsRoleToUppercaseSpringAuthority() {
        AppUserDetails details = new AppUserDetails(user("Admin"));

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void neverLocksOrExpiresAccounts() {
        AppUserDetails details = new AppUserDetails(user("user"));

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    private static UserAccount user(String role) {
        Company company = new Company();
        company.setId(9L);
        UserAccount user = new UserAccount();
        user.setId(5L);
        user.setEmail("alice@example.com");
        user.setPassword("hashed-password");
        user.setRole(role);
        user.setCompany(company);
        return user;
    }
}
