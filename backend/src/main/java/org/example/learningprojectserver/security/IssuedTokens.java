package org.example.learningprojectserver.security;

/** A fresh access JWT and the raw (unhashed) refresh token, to be written as httpOnly cookies. */
public record IssuedTokens(String accessToken, String refreshToken) {
}
