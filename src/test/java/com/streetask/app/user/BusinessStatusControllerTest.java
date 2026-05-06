package com.streetask.app.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.streetask.app.moderation.BusinessVerificationDTO;
import com.streetask.app.moderation.BusinessVerificationService;

@WebMvcTest(BusinessStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class BusinessStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusinessVerificationService businessVerificationService;

    @Test
    @WithMockUser
    @DisplayName("GET /me/verification should return business status")
    void getMyVerificationStatus_shouldReturnStatus() throws Exception {
        BusinessVerificationDTO dto = mock(BusinessVerificationDTO.class);

        when(businessVerificationService.getMyVerificationStatus()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/businesses/me/verification")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}