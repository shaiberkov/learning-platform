package org.example.learningprojectserver.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.learningprojectserver.security.IssuedTokens;

/**
 * Result of OTP verification. The tokens are never serialized: the controller writes them
 * as httpOnly cookies, so the JSON body carries only success / errorCode.
 */
public class LoginResponse extends BasicResponse {
    @JsonIgnore
    private IssuedTokens tokens;


    public LoginResponse() {

    }

    public LoginResponse(boolean success, String errorCode, IssuedTokens tokens) {
        super(success, errorCode);
        this.tokens = tokens;
    }


    @JsonIgnore
    public IssuedTokens getTokens() {
        return tokens;
    }

    public void setTokens(IssuedTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public String toString() {
        return "LoginResponse{" + super.toString() + '}';
    }
}
