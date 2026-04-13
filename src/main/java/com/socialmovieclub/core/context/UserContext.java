package com.socialmovieclub.core.context;

import lombok.Data;

import java.util.Locale;

@Data
public class UserContext {
    private String region;
    private String language;
    private Locale locale;
}
