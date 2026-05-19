package com.ecoal.api.security;

import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserProvider {

    private CurrentUserProvider() {}

    public static AppUserDetails get() {
        return (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
