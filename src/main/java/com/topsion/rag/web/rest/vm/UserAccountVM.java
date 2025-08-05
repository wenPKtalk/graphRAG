package com.topsion.rag.web.rest.vm;

import java.util.Set;

/**
 * View Model representing a user account.
 */
public class UserAccountVM {

    private String login;
    private Set<String> authorities;
    private boolean activated;

    public UserAccountVM() {
        // Empty constructor needed for Jackson.
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public String toString() {
        return "UserAccountVM{" +
            "login='" + login + '\'' +
            ", authorities=" + authorities +
            ", activated=" + activated +
            '}';
    }
}