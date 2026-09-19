package org.nidis.national_identity_document_issuing_system.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        boolean isAdmin = authorities.stream()
                .anyMatch(auth -> "SUPER_ADMIN".equals(auth.getAuthority()));
        boolean isOfficer = authorities.stream()
                .anyMatch(auth -> "VERIFICATION_OFFICER".equals(auth.getAuthority()));

        if (isAdmin) {
            response.sendRedirect("/admin/dashboard");
        } else if (isOfficer) {
            response.sendRedirect("/verification/queue");
        } else {
            response.sendRedirect("/dashboard");
        }
    }
}

