package com.streetask.app.answer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.model.Answer;
import com.streetask.app.model.AnswerVote;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.Question;
import com.streetask.app.model.enums.VoteType;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.RegularUser;

@DataJpaTest
class AnswerVoteRepositoryTest {

    private static final UUID REGULAR_AUTHORITY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BUSINESS_AUTHORITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnswerVoteRepository answerVoteRepository;

    private BusinessAccount businessVoter;
    private Answer answer;

    @BeforeEach
    void setUp() {
        Authorities regularAuthority = entityManager.find(Authorities.class, REGULAR_AUTHORITY_ID);
        Authorities businessAuthority = entityManager.find(Authorities.class, BUSINESS_AUTHORITY_ID);

        RegularUser questionCreator = new RegularUser();
        questionCreator.setEmail("creator@test.com");
        questionCreator.setUserName("creator");
        questionCreator.setFirstName("Question");
        questionCreator.setLastName("Creator");
        questionCreator.setPassword("secret");
        questionCreator.setAuthority(regularAuthority);
        questionCreator.setActive(true);
        questionCreator = entityManager.persistAndFlush(questionCreator);

        Question question = new Question();
        question.setCreator(questionCreator);
        question.setTitle("Question title");
        question.setContent("Question content");
        question.setActive(true);
        question.setCreatedAt(Instant.now());
        question = entityManager.persistAndFlush(question);

        RegularUser answerAuthor = new RegularUser();
        answerAuthor.setEmail("author@test.com");
        answerAuthor.setUserName("author");
        answerAuthor.setFirstName("Answer");
        answerAuthor.setLastName("Author");
        answerAuthor.setPassword("secret");
        answerAuthor.setAuthority(regularAuthority);
        answerAuthor.setActive(true);
        answerAuthor = entityManager.persistAndFlush(answerAuthor);

        answer = new Answer();
        answer.setQuestion(question);
        answer.setUser(answerAuthor);
        answer.setContent("This is a persisted answer");
        answer.setCreatedAt(null);
        answer.setUpvotes(0);
        answer.setDownvotes(0);
        answer.setCoinsEarned(0);
        answer.setRewardClaimed(false);
        answer.setUserLocation(new GeoPoint());
        answer.getUserLocation().setLatitude(40.4168);
        answer.getUserLocation().setLongitude(-3.7038);
        answer = entityManager.persistAndFlush(answer);

        businessVoter = new BusinessAccount();
        businessVoter.setEmail("business-voter@test.com");
        businessVoter.setUserName("businessvoter");
        businessVoter.setFirstName("Business");
        businessVoter.setLastName("Voter");
        businessVoter.setPassword("secret");
        businessVoter.setAuthority(businessAuthority);
        businessVoter.setCompanyName("Business Voter SL");
        businessVoter.setTaxId("B12345679");
        businessVoter.setActive(true);
        businessVoter = entityManager.persistAndFlush(businessVoter);

        entityManager.clear();
    }

    @Test
    void saveAndReloadBusinessVote_shouldPersistAgainstAppUsersFk() {
        Answer reloadedAnswer = entityManager.find(Answer.class, answer.getId());
        BusinessAccount reloadedBusinessVoter = entityManager.find(BusinessAccount.class, businessVoter.getId());

        AnswerVote vote = new AnswerVote();
        vote.setAnswer(reloadedAnswer);
        vote.setUser(reloadedBusinessVoter);
        vote.setVoteType(VoteType.LIKE);
        vote.setVotedAt(java.time.LocalDateTime.now());

        AnswerVote savedVote = answerVoteRepository.save(vote);
        entityManager.flush();
        entityManager.clear();

        AnswerVote foundVote = answerVoteRepository.findById(savedVote.getId()).orElseThrow();

        assertThat(foundVote.getUser()).isInstanceOf(BusinessAccount.class);
        assertThat(foundVote.getVoteType()).isEqualTo(VoteType.LIKE);
        assertThat(answerVoteRepository.findByUserIdAndAnswerId(businessVoter.getId(), answer.getId())).isPresent();
    }
}