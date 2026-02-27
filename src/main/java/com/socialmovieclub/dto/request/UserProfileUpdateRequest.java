package com.socialmovieclub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String firstName;
    private String lastName;

    @Size(max = 250, message = "{user.bio.length}")
    private String bio;

    private String profilePictureUrl;
}
