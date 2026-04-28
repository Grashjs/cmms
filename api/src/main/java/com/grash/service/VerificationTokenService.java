package com.grash.service;

import com.grash.factory.MailServiceFactory;
import com.grash.model.User;
import com.grash.model.VerificationToken;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;

@Service
@AllArgsConstructor
public class VerificationTokenService {

    private final UserService userService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MailServiceFactory mailServiceFactory;


    public VerificationToken getVerificationTokenEntity(String token) {
        return verificationTokenRepository.findVerificationTokenEntityByToken(token);
    }

    public void deleteVerificationTokenEntity(User user) {
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

    public String confirmMail(String token) throws Exception {

        User user = verifyToken(token).getUser();
        //valid token
        userService.enableUser(user.getEmail());
        if (!user.getCompany().isDemo()) mailServiceFactory.getMailService().addToContactList(user);
        return user.getEmail();
    }

    public User confirmResetPassword(String token) throws Exception {
        VerificationToken verificationToken = verifyToken(token);
        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(verificationToken.getPayload()));
        return userRepository.save(user);
    }
}
