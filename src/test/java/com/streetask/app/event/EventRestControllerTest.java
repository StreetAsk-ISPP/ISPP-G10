package com.streetask.app.event;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.streetask.app.model.Event;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.BusinessAccount;
import com.streetask.app.user.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Event event1;

    @BeforeEach
    void setUp() {
        Authorities businessAuthority = entityManager.find(Authorities.class,
                java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"));

        BusinessAccount creator = new BusinessAccount();
        creator.setEmail("eventcreator@streetask.com");
        creator.setUserName("eventcreator");
        creator.setFirstName("Event");
        creator.setLastName("Creator");
        creator.setCompanyName("Events Corp");
        creator.setTaxId("B11223344");
        creator.setAuthority(businessAuthority);
        creator = (BusinessAccount) userRepository.save(creator);

        event1 = new Event();
        event1.setTitle("Event Endpoint Test 1");
        event1.setDescription("First event for endpoint tests");
        event1.setCreator(creator);
        event1 = eventRepository.save(event1);

        Event event2 = new Event();
        event2.setTitle("Event Endpoint Test 2");
        event2.setDescription("Second event for endpoint tests");
        event2.setCreator(creator);
        eventRepository.save(event2);
    }

    @Test
    @WithMockUser
    void findAll_shouldReturnAllEvents() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Event Endpoint Test 1")))
                .andExpect(jsonPath("$[*].title", hasItem("Event Endpoint Test 2")));
    }

    @Test
    @WithMockUser
    void findById_shouldReturnEventWhenExists() throws Exception {
        mockMvc.perform(get("/api/v1/events/{id}", event1.getId())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event1.getId().toString()))
                .andExpect(jsonPath("$.title").value("Event Endpoint Test 1"))
                .andExpect(jsonPath("$.description").value("First event for endpoint tests"));
    }

    @Test
    @WithMockUser
    void findById_shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/events/{id}", nonExistentId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
