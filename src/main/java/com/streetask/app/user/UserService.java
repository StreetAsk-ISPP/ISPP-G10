package com.streetask.app.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.answer.AnswerRepository;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Question;
import com.streetask.app.question.QuestionRepository;

import jakarta.validation.Valid;

@Service
public class UserService {

    private UserRepository userRepository;
    private AnswerRepository answerRepository;
    private QuestionRepository questionRepository;
    private PasswordEncoder passwordEncoder;

    private static final int LIKE_WEIGHT = 2;
    private static final int DISLIKE_WEIGHT = 1;

    @Autowired
    public UserService(UserRepository userRepository, AnswerRepository answerRepository,
            QuestionRepository questionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private PasswordEncoder getPasswordEncoder() {
        return passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    @Transactional
    public User saveUser(User user) throws DataAccessException {
        userRepository.save(user);
        return enrichReputation(user);
    }

    @Transactional(readOnly = true)
    public User findUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return enrichReputation(user);
    }

    @Transactional(readOnly = true)
    public User findUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return enrichReputation(user);
    }

    @Transactional(readOnly = true)
    public User findCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            throw new ResourceNotFoundException("Nobody authenticated!");
        else {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "Email", auth.getName()));
            return enrichReputation(user);
        }
    }

    public Boolean existsUser(String email) {
        return userRepository.existsByEmail(email);
    }

    public Boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Transactional(readOnly = true)
    public Iterable<User> findAll() {
        return enrichReputation(userRepository.findAll());
    }

    public Iterable<User> findAllByAuthority(String auth) {
        return enrichReputation(userRepository.findAllByAuthority(auth));
    }

    @Transactional
    public User updateUser(@Valid User user, UUID idToUpdate) {
        User toUpdate = findUser(idToUpdate);

        String previousPassword = toUpdate.getPassword();

        BeanUtils.copyProperties(user, toUpdate, "id", "authority", "accountType", "createdAt", "lastLogin", "active");

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            toUpdate.setPassword(previousPassword);
        } else {
            toUpdate.setPassword(getPasswordEncoder().encode(user.getPassword()));
        }

        userRepository.save(toUpdate);
        return enrichReputation(toUpdate);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User toDelete = findUser(id);
        this.userRepository.delete(toDelete);
    }

    private User enrichReputation(User user) {
        Map<UUID, Integer> reputationByUserId = calculateReputationByUserIds(List.of(user.getId()));
        user.setReputation(reputationByUserId.getOrDefault(user.getId(), 0));
        return user;
    }

    private Iterable<User> enrichReputation(Iterable<User> users) {
        List<User> userList = StreamSupport.stream(users.spliterator(), false).toList();
        if (userList.isEmpty()) {
            return userList;
        }

        List<UUID> userIds = new ArrayList<>(userList.size());
        for (User user : userList) {
            userIds.add(user.getId());
        }

        Map<UUID, Integer> reputationByUserId = calculateReputationByUserIds(userIds);

        for (User user : userList) {
            user.setReputation(reputationByUserId.getOrDefault(user.getId(), 0));
        }

        return userList;
    }

    private Map<UUID, Integer> calculateReputationByUserIds(List<UUID> userIds) {
        Map<UUID, Integer> reputationByUserId = new HashMap<>();
        List<Object[]> aggregates = answerRepository.aggregateVotesByUserIds(userIds);
        for (Object[] row : aggregates) {
            UUID userId = (UUID) row[0];
            int likes = ((Number) row[1]).intValue();
            int dislikes = ((Number) row[2]).intValue();
            int reputation = (likes * LIKE_WEIGHT) - (dislikes * DISLIKE_WEIGHT);
            reputationByUserId.put(userId, reputation);
        }
        return reputationByUserId;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(UUID userId) {
        User user = findUser(userId);

        long questionsCount = questionRepository.countByCreatorId(userId);
        long answersCount = answerRepository.countByUserId(userId);

        int likesCount = 0;
        int dislikesCount = 0;
        int coinBalance = 0;
        if (user instanceof RegularUser regularUser) {
            likesCount = regularUser.getTotalLikesReceived() == null ? 0 : regularUser.getTotalLikesReceived();
            dislikesCount = regularUser.getTotalDislikesReceived() == null ? 0 : regularUser.getTotalDislikesReceived();
            coinBalance = regularUser.getCoinBalance() == null ? 0 : regularUser.getCoinBalance();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("questionsCount", questionsCount);
        stats.put("answersCount", answersCount);
        stats.put("username", user.getUserName());

        stats.put("bio", user.getBio());
        stats.put("profilePictureUrl", user.getProfilePictureUrl());

        stats.put("role", user.getAuthority().getAuthority());
        stats.put("likesCount", likesCount);
        stats.put("dislikesCount", dislikesCount);
        stats.put("coinBalance", coinBalance);
        int reputation = (likesCount * LIKE_WEIGHT) - (dislikesCount * DISLIKE_WEIGHT);
        stats.put("reputation", reputation);

        // Calculate rating on a 0-5 scale from vote ratio.
        // Formula: likes / (likes + dislikes) * 5
        int totalInteractions = likesCount + dislikesCount;
        double rating = 0.0;
        if (totalInteractions > 0) {
            rating = ((double) likesCount / (double) totalInteractions) * 5.0;
            if (rating > 5.0) {
                rating = 5.0;
            }
            rating = Math.round(rating * 10.0) / 10.0;
        }

        stats.put("rating", rating);

        return stats;
    }

    @Transactional(readOnly = true)
    public Iterable<Question> findQuestionsByUserId(UUID userId) {
        return questionRepository.findByCreatorId(userId);
    }

    @Transactional(readOnly = true)
    public Iterable<com.streetask.app.model.Answer> findAnswersByUserId(UUID userId) {
        return answerRepository.findByUserId(userId);
    }
}