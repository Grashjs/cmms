package com.grash.security;

import com.grash.model.OwnUser;
import com.grash.repository.UserRepository;
import com.grash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    @Lazy
    private UserService userService;

    @Override
    @Transactional(readOnly = true)
    public CustomUserDetail loadUserByUsername(String username) throws UsernameNotFoundException {
        OwnUser user = userService.whoami(username, true);
        return CustomUserDetail.builder()//
                .user(user)//
                .build();
    }

}
