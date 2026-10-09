package com.aadi.sandesh.security;

import com.aadi.sandesh.domain.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration accessTokenTtl;

    public JwtService(@Value("${sandesh.security.jwt-secret}") String jwtSecret,@Value("${sandesh.security.access-token-ttl}") Duration accessTokenTtl) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
    }

    public IssuedToken issue(UserAccount user){
        var now = Instant.now();
        var expiresAt = now.plus(accessTokenTtl);

        var token = Jwts.builder().subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, expiresAt);
    }

    public ChatPrincipal parse(String token){
       Claims claims = Jwts.parser()
               .verifyWith(signingKey)
               .build()
               .parseSignedClaims(token).getPayload();


        return new ChatPrincipal(
                UUID.fromString(claims.getSubject()), claims.get("email", String.class));
    }
}
