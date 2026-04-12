package com.streetask.app.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.answer.AnswerRepository;
import com.streetask.app.answer.AnswerService;
import com.streetask.app.answer.AnswerVoteRepository;
import com.streetask.app.model.Answer;
import com.streetask.app.model.AnswerVote;
import com.streetask.app.model.Question;
import com.streetask.app.model.enums.VoteType;
import com.streetask.app.question.QuestionRepository;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.AuthoritiesRepository;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.User;
import com.streetask.app.user.UserService;

@SpringBootTest
@Transactional
class UserReputationVotesIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private AnswerVoteRepository answerVoteRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private RegularUserRepository regularUserRepository;

    @Autowired
    private AuthoritiesRepository authoritiesRepository;

    @Test
    void upvote_shouldIncreaseOwnerReputation_andPersistVoteAndCounters() {
        Authorities authority = createAndSaveAuthority();
        RegularUser owner = createAndSaveUser("owner", authority);
        RegularUser voter = createAndSaveUser("voter", authority);
        Question question = createAndSaveQuestion(owner);
        Answer answer = createAndSaveAnswer(question, owner, 0, 0);

        answerService.updateVotes(answer.getId(), voter.getId(), VoteType.LIKE);

        Answer persistedAnswer = answerRepository.findById(answer.getId()).orElseThrow();
        assertEquals(1, persistedAnswer.getUpvotes());
        assertEquals(0, persistedAnswer.getDownvotes());

        Optional<AnswerVote> persistedVote = answerVoteRepository.findByUserIdAndAnswerId(voter.getId(),
                answer.getId());
        assertTrue(persistedVote.isPresent());
        assertEquals(VoteType.LIKE, persistedVote.get().getVoteType());

        User ownerWithReputation = userService.findUser(owner.getId());
        User voterWithReputation = userService.findUser(voter.getId());
        assertEquals(2, ownerWithReputation.getReputation());
        assertEquals(0, voterWithReputation.getReputation());

        List<Object[]> aggregates = answerRepository.aggregateVotesByUserIds(List.of(owner.getId()));
        assertEquals(1, aggregates.size());
        int likes = ((Number) aggregates.get(0)[1]).intValue();
        int dislikes = ((Number) aggregates.get(0)[2]).intValue();
        assertEquals((likes * 2) - dislikes, ownerWithReputation.getReputation());
    }

    @Test
    void downvote_shouldDecreaseOwnerReputation_andNotAffectVoterReputation() {
        Authorities authority = createAndSaveAuthority();
        RegularUser owner = createAndSaveUser("ownerDown", authority);
        RegularUser voter = createAndSaveUser("voterDown", authority);
        Question question = createAndSaveQuestion(owner);
        Answer answer = createAndSaveAnswer(question, owner, 0, 0);

        answerService.updateVotes(answer.getId(), voter.getId(), VoteType.DISLIKE);

        Answer persistedAnswer = answerRepository.findById(answer.getId()).orElseThrow();
        assertEquals(0, persistedAnswer.getUpvotes());
        assertEquals(1, persistedAnswer.getDownvotes());

        User ownerWithReputation = userService.findUser(owner.getId());
        User voterWithReputation = userService.findUser(voter.getId());
        assertEquals(-1, ownerWithReputation.getReputation());
        assertEquals(0, voterWithReputation.getReputation());
    }

    @Test
    void changingVoteType_shouldRecalculateReputationConsistently() {
        Authorities authority = createAndSaveAuthority();
        RegularUser owner = createAndSaveUser("ownerSwap", authority);
        RegularUser voter = createAndSaveUser("voterSwap", authority);
        Question question = createAndSaveQuestion(owner);
        Answer answer = createAndSaveAnswer(question, owner, 0, 0);

        answerService.updateVotes(answer.getId(), voter.getId(), VoteType.LIKE);
        assertEquals(2, userService.findUser(owner.getId()).getReputation());

        answerService.updateVotes(answer.getId(), voter.getId(), VoteType.DISLIKE);

        Answer persistedAnswer = answerRepository.findById(answer.getId()).orElseThrow();
        assertEquals(0, persistedAnswer.getUpvotes());
        assertEquals(1, persistedAnswer.getDownvotes());
        assertEquals(-1, userService.findUser(owner.getId()).getReputation());
    }

    @Test
    void usersWithNoAnswers_orZeroVotesAnswers_shouldHaveZeroReputation() {
        Authorities authority = createAndSaveAuthority();
        RegularUser userWithNoAnswers = createAndSaveUser("noAnswers", authority);
        RegularUser userWithZeroVoteAnswer = createAndSaveUser("zeroVotes", authority);

        Question question = createAndSaveQuestion(userWithZeroVoteAnswer);
        createAndSaveAnswer(question, userWithZeroVoteAnswer, 0, 0);

        User noAnswers = userService.findUser(userWithNoAnswers.getId());
        User zeroVotes = userService.findUser(userWithZeroVoteAnswer.getId());

        assertEquals(0, noAnswers.getReputation());
        assertEquals(0, zeroVotes.getReputation());
    }

    private Authorities createAndSaveAuthority() {
        Authorities authority = new Authorities();
        authority.setAuthority("USER_" + UUID.randomUUID().toString().substring(0, 8));
        return authoritiesRepository.save(authority);
    }

    private RegularUser createAndSaveUser(String prefix, Authorities authority) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegularUser user = new RegularUser();
        user.setEmail(prefix + "_" + suffix + "@test.com");
        user.setUserName(prefix + "_" + suffix);
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAuthority(authority);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setVerified(true);
        user.setCoinBalance(0);
        return regularUserRepository.save(user);
    }

    private Question createAndSaveQuestion(RegularUser creator) {
        Question question = new Question();
        question.setCreator(creator);
        question.setTitle("Question " + UUID.randomUUID());
        question.setContent("Question content");
        question.setCreatedAt(Instant.now());
        question.setActive(true);
        question.setAnswerCount(0);
        return questionRepository.save(question);
    }

    private Answer createAndSaveAnswer(Question question, RegularUser owner, int upvotes, int downvotes) {
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setUser(owner);
        answer.setContent("Answer content");
        answer.setCreatedAt(OffsetDateTime.now());
        answer.setUpvotes(upvotes);
        answer.setDownvotes(downvotes);
        Answer saved = answerRepository.save(answer);
        assertNotNull(saved.getId());
        return saved;
    }
}
