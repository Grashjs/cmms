package com.grash.service;

import com.grash.dto.AuthResponse;
import com.grash.model.OwnUser;
import com.grash.model.VerificationToken;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

@Service
@AllArgsConstructor
public class VerificationTokenService {

    private final UserService userService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;


    public VerificationToken getVerificationTokenEntity(String token) {
        return verificationTokenRepository.findVerificationTokenEntityByToken(token);
    }

    public void deleteVerificationTokenEntity(OwnUser user) {
        ArrayList<VerificationToken> verificationToken =
                verificationTokenRepository.findAllVerificationTokenEntityByUser(user);
        verificationTokenRepository.deleteAll(verificationToken);
    }


    private VerificationToken verifyToken(String token) throws Exception {
        VerificationToken verificationToken = getVerificationTokenEntity(token);

        //invalid token
        if (verificationToken == null) {
            String message = "Invalid activation link";
            throw new Exception(message);
        }

        //expired token
        Calendar calendar = Calendar.getInstance();
        if ((verificationToken.getExpiryDate().getTime() - calendar.getTime().getTime()) <= 0) {
            String message = "Expired activation link!";
            throw new Exception(message);
        }
        return verificationToken;
    }

    public AuthResponse confirmMail(String token) throws Exception {

        OwnUser user = verifyToken(token).getUser();
        //valid token
        userService.enableUser(user.getEmail());
        String message = "Account successfully activated !";

        return new AuthResponse(jwtTokenProvider.createToken(user.getEmail(),
                Arrays.asList(user.getRole().getRoleType())));
    }

    public OwnUser confirmResetPassword(String token) throws Exception {
        VerificationToken verificationToken = verifyToken(token);
        OwnUser user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(verificationToken.getPayload()));
        return userRepository.save(user);
    }
}
