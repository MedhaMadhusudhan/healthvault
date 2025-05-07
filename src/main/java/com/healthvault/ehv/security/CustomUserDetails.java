package com.healthvault.ehv.security;

import com.healthvault.ehv.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Add ROLE_USER as a base role for all users
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Add role-specific authority
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toString()));
        }
        
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    // Custom getters to expose User properties
    public String getName() {
        return user.getName();
    }

    public String getBloodGroup() {
        return user.getBloodGroup();
    }

    public String getPhoneNumber() {
        return user.getPhoneNumber();
    }

    public String getEmailAddress() {
        return user.getEmailAddress();
    }

    // Getter for the underlying user entity
    public User getUser() {
        return user;
    }
}