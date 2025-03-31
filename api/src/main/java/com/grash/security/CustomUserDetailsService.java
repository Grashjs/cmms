package com.grash.security;

import com.grash.model.OwnUser;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomUserDetail loadUserByUsername(String username) throws UsernameNotFoundException {
        final OwnUser user = userRepository.findByEmailIgnoreCase(username).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException("User '" + username + "' not found");
        }

        return CustomUserDetail.builder()//
                .user(user)//
                .build();
    }

}
