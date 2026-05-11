package com.streetask.app.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streetask.app.model.enums.VoteType;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "answer_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "answer_id" })
})
@Getter
@Setter
public class AnswerVote extends BaseEntity {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "answer_id")
    private Answer answer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private VoteType voteType;

    private LocalDateTime votedAt;
}
