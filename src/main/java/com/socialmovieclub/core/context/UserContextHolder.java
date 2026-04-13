package com.socialmovieclub.core.context;

import java.util.Locale;

public class UserContextHolder {
    private static final ThreadLocal<UserContext> userContext = new ThreadLocal<>();

    public static void setContext(UserContext context) {
        userContext.set(context);
    }

    public static UserContext getContext() {
        return userContext.get();
    }

    public static void clear() {
        userContext.remove();
    }
}