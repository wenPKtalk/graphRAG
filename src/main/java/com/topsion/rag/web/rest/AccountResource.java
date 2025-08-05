package com.topsion.rag.web.rest;

import com.topsion.rag.security.SecurityUtils;
import com.topsion.rag.web.rest.vm.UserAccountVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for managing user accounts.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private static final Logger log = LoggerFactory.getLogger(AccountResource.class);

    /**
     * {@code GET  /account} : get the current user account.
     *
     * @return the current user account.
     */
    @GetMapping("/account")
    public Mono<ResponseEntity<UserAccountVM>> getAccount() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(this::createUserAccountVM)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    private UserAccountVM createUserAccountVM(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String login = SecurityUtils.getCurrentUserLogin().block();
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        UserAccountVM userAccount = new UserAccountVM();
        userAccount.setLogin(login);
        userAccount.setAuthorities(authorities);
        userAccount.setActivated(true); // Assuming user is activated if authenticated
        
        return userAccount;
    }
}