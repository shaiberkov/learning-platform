package org.example.learningprojectserver.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.example.learningprojectserver.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final String SECRET = String.valueOf(Keys.secretKeyFor(SignatureAlgorithm.HS256)); // שים במשתנה סביבה

    @Value("${auth.access-token.ttl-minutes:15}")
    private long accessTokenTtlMinutes;
    private static final long IMPERSONATION_EXPIRATION_TIME = 1000 * 60 * 30;
    private static final String IMPERSONATOR_ID_CLAIM = "impersonatorId";
    private static final String IMPERSONATOR_USERNAME_CLAIM = "impersonatorUsername";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

//   @PostConstruct
//   public void init() {
//       System.out.println( "מנהל");
//       System.out.println("אדמיניסטראטור"+generateToken("325256022", String.valueOf(Role.SYSTEM_ADMIN)));
//       System.out.println( extractUserId(generateToken("325256022", String.valueOf(Role.SCHOOLMANAGER))));
//   }

    public String generateToken(String userId, String username,String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenTtlMinutes * 60_000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * A token that acts as targetUserId for support, carrying the system admin who started it.
     * Shorter lived than a login token.
     */
    public String generateImpersonationToken(String userId, String username, String role,
                                             String impersonatorId, String impersonatorUsername) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("username", username);
        claims.put(IMPERSONATOR_ID_CLAIM, impersonatorId);
        claims.put(IMPERSONATOR_USERNAME_CLAIM, impersonatorUsername);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + IMPERSONATION_EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractImpersonatorId(String token) {
        return extractAllClaims(token).get(IMPERSONATOR_ID_CLAIM, String.class);
    }

    public String extractImpersonatorUsername(String token) {
        return extractAllClaims(token).get(IMPERSONATOR_USERNAME_CLAIM, String.class);
    }

    public Claims extractAllClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }


    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

}
