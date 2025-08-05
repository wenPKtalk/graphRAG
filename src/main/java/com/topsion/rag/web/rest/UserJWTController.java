package com.topsion.rag.web.rest;

import com.topsion.rag.security.jwt.TokenProvider;
import com.topsion.rag.web.rest.vm.LoginVM;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.Collections;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class UserJWTController {

    private final TokenProvider tokenProvider;

    public UserJWTController(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * {@code POST  /authenticate} : authenticate user credentials.
     *
     * @param loginVM the login view model.
     * @return the ResponseEntity with status 200 (OK) and with body the JWT token, or 401 (Unauthorized).
     */
    @PostMapping("/authenticate")
    public Mono<ResponseEntity<JWTToken>> authenticate(@Valid @RequestBody LoginVM loginVM) {
        // Simple authentication logic - in production, use proper user service
        if (("admin".equals(loginVM.getUsername()) && "admin".equals(loginVM.getPassword())) ||
            ("user".equals(loginVM.getUsername()) && "user".equals(loginVM.getPassword()))) {
            
            String authority = "admin".equals(loginVM.getUsername()) ? "ROLE_ADMIN" : "ROLE_USER";
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginVM.getUsername(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
            );
            
            String jwt = tokenProvider.createToken(authentication, loginVM.isRememberMe());
            return Mono.just(ResponseEntity.ok()
                .header("Authorization", "Bearer " + jwt)
                .body(new JWTToken(jwt)));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
    }

    /**
     * {@code GET  /authenticate} : check if the user is authenticated, and return its login.
     *
     * @return the login if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public Mono<ResponseEntity<Void>> isAuthenticated() {
        return Mono.just(ResponseEntity.ok().build());
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {
        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}