package com.streetask.app.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String userName;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Size(max = 255)
    private String bio;

    private String profilePictureUrl;
}
