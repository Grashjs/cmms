package com.grash.repository;

import com.grash.model.User;
import com.grash.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    VerificationToken findVerificationTokenEntityByToken(String token);

    ArrayList<VerificationToken> findAllVerificationTokenEntityByUser(User user);
}
