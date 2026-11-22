package org.example.learningprojectserver.security;

import org.springframework.security.access.AccessDeniedException;

/** Thrown when a school has a feature turned off; the message is the Hebrew text the user sees. */
public class FeatureDisabledException extends AccessDeniedException {
    public FeatureDisabledException(String message) {
        super(message);
    }
}
