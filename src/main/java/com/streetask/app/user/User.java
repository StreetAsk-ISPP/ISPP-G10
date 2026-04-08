package com.streetask.app.user;

import com.streetask.app.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size; // Importado para la bio
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "appusers")
public class User extends BaseEntity {

    // Email for authentication
    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    @Column(unique = true)
    private String userName;

    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Size(max = 255)
    private String bio;

    @Lob
    @Column(name = "profile_picture_url", columnDefinition = "LONGTEXT")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "authority")
    Authorities authority;

    @Transient
    private Integer reputation;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private java.util.List<com.streetask.app.functionalities.feedback.FeedbackMessage> feedbackMessages;

	  @com.fasterxml.jackson.annotation.JsonIgnore
	  @OneToMany(mappedBy = "issuedBy", cascade = CascadeType.REMOVE)
	  private List<com.streetask.app.model.Strike> issuedStrikes;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private java.util.List<com.streetask.app.model.UserLocation> locations;


    public Boolean hasAuthority(String auth) {
        return authority.getAuthority().equals(auth);
    }

    public Boolean hasAnyAuthority(String... authorities) {
        Boolean cond = false;
        for (String auth : authorities) {
            if (auth.equals(authority.getAuthority()))
                cond = true;
        }
        return cond;
    }
}