package com.financetracker.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public static final String JWT_COOKIE_NAME = "jwt";

    /**
     * Whether the cookie should only be sent over HTTPS.
     */
    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    /**
     * Set the JWT as an HTTP-only cookie.
     *
     * @param response the HTTP response
     * @param token the JWT token
     * @param maxAgeSeconds how long the cookie lives
     */
    public void setJwtCookie(
            HttpServletResponse response,
            String token,
            long maxAgeSeconds
    ) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge((int) maxAgeSeconds);

        response.addCookie(cookie);
    }

    /**
     * Clear the JWT cookie during logout.
     */
    public void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    /**
     * Read the JWT from the cookie, if present.
     */
    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}