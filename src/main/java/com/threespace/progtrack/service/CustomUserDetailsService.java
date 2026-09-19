package com.threespace.progtrack.service;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    	User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "No user found with email: " + email
                ));

    	// Check if deactivated
    	if (Boolean.FALSE.equals(user.getIsActive())) {
    	    throw new DisabledException("Account is deactivated. Please contact your administrator.");
    	}

    	// Check if pending approval
    	if (Boolean.FALSE.equals(user.getIsApproved()) || user.getIsApproved() == null) {
    	    throw new DisabledException("Your registration approval is pending. Kindly contact the admin.");
    	}

            return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // Spring Security expects "ROLE_" prefix
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Check BOTH isActive and isApproved
        boolean isAccountEnabled = Boolean.TRUE.equals(user.getIsActive()) && Boolean.TRUE.equals(user.getIsApproved());

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            isAccountEnabled,     // enabled (False here prevents login!)
            true,                 // accountNonExpired
            true,                 // credentialsNonExpired
            true,                 // accountNonLocked
            authorities
        );
    }
}
