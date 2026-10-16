package com.gilbertoca.fisio.service;

/**
 * Demo authentication service.
 * Fixed credentials: admin / admin.
 *
 * WARNING: for teaching purposes only.
 */
public class AuthService {
    private static final String DEMO_USER = "admin";
    private static final String DEMO_PASS = "admin";

    public boolean authenticate(String username, String password) {
        return DEMO_USER.equals(username) && DEMO_PASS.equals(password);
    }
}
