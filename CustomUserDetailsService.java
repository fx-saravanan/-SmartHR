package com.smarthr.service;

import com.smarthr.entity.AppUser;
import com.smarthr.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public CustomUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + username));
        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .disabled(!u.isEnabled())
                .build();
    }
}
