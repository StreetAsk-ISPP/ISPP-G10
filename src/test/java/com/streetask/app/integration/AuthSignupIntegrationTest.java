package com.streetask.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.user.BusinessAccount;
import com.streetask.app.user.RequestStatus;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import com.streetask.app.user.RegularUser;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSignupIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Test
        void signupBasicShouldCreateUserWhenPayloadIsValid() throws Exception {
                String email = "regular.success@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "regularUser1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Basic user data saved! Complete your registration."));

                assertThat(userRepository.findByEmail(email)).isPresent();
        }

        @Test
        void signupBasicShouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
                String email = "duplicated.email@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "dupUser1"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "dupUser2"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Error: Email is already registered!"));
        }

        @Test
        void signupBasicShouldReturnBadRequestWhenUserNameAlreadyExists() throws Exception {
                String duplicatedUserName = "duplicatedUser";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBasicPayload("first.user@streetask.com", duplicatedUserName))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBasicPayload("second.user@streetask.com", duplicatedUserName))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));
        }

        @Test
        void signupBasicShouldReturnBadRequestWhenRequiredFieldIsMissing() throws Exception {
                Map<String, Object> invalidPayload = new HashMap<>();
                invalidPayload.put("email", "invalid.payload@streetask.com");
                invalidPayload.put("password", "123456");
                invalidPayload.put("firstName", "Invalid");
                invalidPayload.put("lastName", "Payload");

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidPayload)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void signupRegularShouldConvertBasicUserToRegularUser() throws Exception {
                String email = "regular.convert@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(validBasicPayload(email, "regularConvertUser"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/regular")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("email", email))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Regular user registered successfully!"));

                User storedUser = userRepository.findByEmail(email).orElseThrow();
                assertThat(storedUser).isInstanceOf(RegularUser.class);
                assertThat(storedUser.getAuthority().getAuthority()).isEqualTo("USER");
                assertThat(storedUser.getActive()).isTrue();
        }

        @Test
        void signupRegularShouldReturnBadRequestWhenBasicUserDoesNotExist() throws Exception {
                mockMvc.perform(post("/api/v1/auth/signup/regular")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(Map.of("email", "missing.user@streetask.com"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Error: User not found or already completed!"));
        }

        @Test
        void signupBusinessShouldConvertBasicUserToBusinessAccount() throws Exception {
                String email = "business.convert@streetask.com";
                String taxId = "B12345678";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "businessUser1"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(validBusinessPayload(email, taxId, "Gran Via 1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Business account registered successfully! Your account is pending admin verification."));

                User storedUser = userRepository.findByEmail(email).orElseThrow();
                assertThat(storedUser).isInstanceOf(BusinessAccount.class);
                BusinessAccount businessAccount = (BusinessAccount) storedUser;

                assertThat(businessAccount.getTaxId()).isEqualTo(taxId);
                assertThat(businessAccount.getCompanyName()).isEqualTo("Test Company");
                assertThat(businessAccount.getAddress()).isEqualTo("Gran Via 1");
                assertThat(businessAccount.getAuthority().getAuthority()).isEqualTo("BUSINESS");
                assertThat(businessAccount.getActive()).isFalse();
                assertThat(businessAccount.getRequestStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        void signupBusinessShouldReturnBadRequestWhenTaxIdAlreadyExists() throws Exception {
                String duplicatedTaxId = "B87654321";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(validBasicPayload("business.one@streetask.com",
                                                                "businessOne"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBusinessPayload("business.one@streetask.com", duplicatedTaxId,
                                                                "Address 1"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(validBasicPayload("business.two@streetask.com",
                                                                "businessTwo"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBusinessPayload("business.two@streetask.com", duplicatedTaxId,
                                                                "Address 2"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Error: Tax ID is already registered!"));
        }

        @Test
        void signupBusinessShouldRejectDuplicateTaxIdIgnoringCase() throws Exception {
                String firstEmail = "business.case.first@streetask.com";
                String secondEmail = "business.case.second@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(firstEmail, "businessCase1"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBusinessPayload(firstEmail, "B76543210", "Address One"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(secondEmail, "businessCase2"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBusinessPayload(secondEmail, "b76543210", "Address Two"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Error: Tax ID is already registered!"));
        }

        @Test
        void signupBusinessShouldReturnBadRequestWhenBasicUserDoesNotExist() throws Exception {
                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBusinessPayload("missing.user@streetask.com", "B99999991",
                                                                "Address Missing"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "Error: Basic user registration not found. Please complete the basic signup first."));
        }

        @Test
        void signupBusinessShouldReturnBadRequestWhenTaxIdFormatIsInvalid() throws Exception {
                String email = "business.invalidtaxid@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "invalidTaxUser"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBusinessPayload(email, "12345678A", "Invalid Address"))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void signupBusinessShouldReturnBadRequestWhenCompanyNameIsMissing() throws Exception {
                String email = "business.nocompany@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "noCompanyUser"))))
                                .andExpect(status().isOk());

                Map<String, Object> payload = validBusinessPayload(email, "B12345679", "Address 3");
                payload.remove("companyName");

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", containsString("companyName")));
        }

        @Test
        void signupBusinessShouldReturnBadRequestWhenTaxIdIsMissing() throws Exception {
                String email = "business.notaxid@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "noTaxIdUser"))))
                                .andExpect(status().isOk());

                Map<String, Object> payload = validBusinessPayload(email, "B12345679", "Address 5");
                payload.remove("taxId");

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", containsString("taxId")));
        }

        @Test
        void signupBusinessShouldReturnBadRequestWhenEmailIsMissing() throws Exception {
                Map<String, Object> payload = validBusinessPayload("business.noemail@streetask.com", "B12345688", "Address 6");
                payload.remove("email");

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", containsString("email")));
        }

        @Test
        void signupBusinessShouldNormalizeTaxIdToUpperCaseBeforeSaving() throws Exception {
                String email = "business.normalize@streetask.com";

                mockMvc.perform(post("/api/v1/auth/signup/basic")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validBasicPayload(email, "normalizeUser"))))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/auth/signup/business")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                validBusinessPayload(email, "b12345670", "Address 4"))))
                                .andExpect(status().isOk());

                User storedUser = userRepository.findByEmail(email).orElseThrow();
                assertThat(storedUser).isInstanceOf(BusinessAccount.class);
                BusinessAccount businessAccount = (BusinessAccount) storedUser;
                assertThat(businessAccount.getTaxId()).isEqualTo("B12345670");
        }

        private Map<String, Object> validBasicPayload(String email, String userName) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("email", email);
                payload.put("userName", userName);
                payload.put("password", "123456");
                payload.put("firstName", "Test");
                payload.put("lastName", "User");
                return payload;
        }

        private Map<String, Object> validBusinessPayload(String email, String taxId, String address) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("email", email);
                payload.put("taxId", taxId);
                payload.put("companyName", "Test Company");
                payload.put("address", address);
                return payload;
        }
}
