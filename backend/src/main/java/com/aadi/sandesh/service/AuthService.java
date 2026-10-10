package com.aadi.sandesh.service;

import com.aadi.sandesh.api.dto.AuthResponse;
import com.aadi.sandesh.api.dto.LoginRequest;
import com.aadi.sandesh.api.dto.RegisterRequest;
import com.aadi.sandesh.domain.UserAccount;
import com.aadi.sandesh.exception.ConflictException;
import com.aadi.sandesh.exception.ForbiddenException;
import com.aadi.sandesh.repository.UserAccountRepository;
import com.aadi.sandesh.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserAccountRepository userAccountRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuditService auditService;

    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if(userAccountRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists.");
        }

        var user = userAccountRepository.save(
                new UserAccount(
                       request.displayName(),
                          request.email(),
                            passwordEncoder.encode(request.password())
                )
        );

        auditService.record(user.getId(), "User registered with email: " + user.getEmail(),
                "USER",user.getId().toString(), Map.of());




        return response(user);
    }


    @Transactional(readOnly = true)
   public AuthResponse login(LoginRequest request){
        var user = userAccountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ForbiddenException("Invalid email or password."));

        if(!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ForbiddenException("Invalid email or password.");
        }
        return response(user);
    }



    private AuthResponse response(UserAccount user) {
        var token = jwtService.issue(user);
        return new AuthResponse(token.value(),"Bearer", token.expiresAt(),
                user.getId(), user.getDisplayName(), user.getEmail());
    }
}
