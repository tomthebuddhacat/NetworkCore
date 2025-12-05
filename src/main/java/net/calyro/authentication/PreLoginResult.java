package net.calyro.authentication;

import net.calyro.database.User;

public class PreLoginResult {

    private final PreLoginState state;
    private final String message;
    private final User user;

    public PreLoginResult(PreLoginState state, String message, User user) {
        this.state = state;
        this.message = message;
        this.user = user;
    }

    public PreLoginState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public enum PreLoginState {

        FORCE_ONLINE, FORCE_OFFLINE, DENIED

    }

}
