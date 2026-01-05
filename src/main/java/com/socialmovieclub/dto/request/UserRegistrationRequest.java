package com.socialmovieclub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationRequest {
    @NotBlank(message = "{user.username.required}")
    private String username;

    @NotBlank(message = "{user.email.required}")
    @Email(message = "{user.email.invalid}")
    private String email;

    @NotBlank(message = "{user.password.required}")
    @Size(min = 6, message = "{user.password.size}")
    private String password;

    private String firstName;
    private String lastName;
}