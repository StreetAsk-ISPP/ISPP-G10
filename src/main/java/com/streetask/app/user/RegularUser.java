package com.streetask.app.user;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streetask.app.model.Answer;
import com.streetask.app.model.CoinTransaction;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.model.Question;
import com.streetask.app.model.Report;
import com.streetask.app.model.Strike;
import com.streetask.app.functionalities.notifications.model.Notification;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "regular_users")
@Getter
@Setter
public class RegularUser extends User {

    private String phone;

    private String profilePhoto;

    private Integer coinBalance;

    private Float rating;

    private Integer totalLikesReceived;

    private Integer totalDislikesReceived;

    private Float visibilityRadiusKm;

    private Boolean premiumActive;

    private Boolean verified;

    @JsonIgnore
    @OneToMany(mappedBy = "creator")
    private List<Question> questions;

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<Answer> answers;

    @OneToMany(mappedBy = "regularUser")
    private List<EventAttendance> eventAttendances;

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "user")
    private List<CoinTransaction> coinTransactions;

    @OneToMany(mappedBy = "reporter")
    private List<Report> reports;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<com.streetask.app.functionalities.notifications.push.model.PushDevice> pushDevices;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<Strike> strikes;
}
