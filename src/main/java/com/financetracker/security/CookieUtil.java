package com.financetracker.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public static final String JWT_COOKIE_NAME = "jwt";

    /**
     * Set the JWT as an HTTP-only cookie.
     * @param maxAgeSeconds how long the cookie lives (should match JWT expiration)
     */
    public void setJwtCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(true);          // JavaScript cannot read it
        cookie.setSecure(false);            // Set to true in production (HTTPS only)
        cookie.setPath("/");                // Valid for the whole site
        cookie.setMaxAge((int) maxAgeSeconds);
        response.addCookie(cookie);
    }

    /**
     * Clear the JWT cookie (for logout).
     */
    public void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);                // Immediately expire
        response.addCookie(cookie);
    }

    /**
     * Read the JWT from the cookie, if present.
     */
    public String getJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}