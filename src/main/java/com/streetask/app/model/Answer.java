package com.streetask.app.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streetask.app.user.User;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "answers")
@Getter
@Setter
public class Answer extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    @JsonBackReference("question-answers")
    private Question question;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({ "password", "authority", "questions", "answers", "eventAttendances", "notifications",
            "coinTransactions", "reports", "reputation", "active", "createdAt", "lastLogin" })
    private User user;

    @NotBlank(message = "Answer content is required")
    private String content;

    private Boolean isVerified;

    private OffsetDateTime verifiedAt;

    @PositiveOrZero(message = "Coins earned must be zero or positive")
    private Integer coinsEarned;

    private Boolean rewardClaimed;

    @Embedded
    @Valid
    private GeoPoint userLocation;

    private OffsetDateTime createdAt;

    @PositiveOrZero(message = "Upvotes must be zero or positive")
    private Integer upvotes;

    @PositiveOrZero(message = "Downvotes must be zero or positive")
    private Integer downvotes;

    @JsonIgnore
    @OneToMany(mappedBy = "answer")
    private List<AnswerVote> votes;
}
