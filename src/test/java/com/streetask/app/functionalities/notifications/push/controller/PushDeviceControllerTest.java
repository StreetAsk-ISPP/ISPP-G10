package com.streetask.app.functionalities.notifications.push.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.functionalities.notifications.push.dto.RegisterPushDeviceRequest;
import com.streetask.app.functionalities.notifications.push.dto.UnregisterPushDeviceRequest;
import com.streetask.app.functionalities.notifications.push.dto.UpdatePushDeviceZoneRequest;
import com.streetask.app.functionalities.notifications.push.service.PushDeviceService;

@WebMvcTest(PushDeviceController.class)
@DisplayName("PushDeviceController Tests")
class PushDeviceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PushDeviceService pushDeviceService;

	private RegisterPushDeviceRequest registerRequest;
	private UpdatePushDeviceZoneRequest updateRequest;
	private UnregisterPushDeviceRequest unregisterRequest;

	@BeforeEach
	void setUp() {
		registerRequest = new RegisterPushDeviceRequest();
		registerRequest.setEndpoint("https://example.com/push");
		registerRequest.setP256dh("test-p256dh");
		registerRequest.setAuth("test-auth");
		registerRequest.setZoneKey("zone-1");
		registerRequest.setLatitude(40.4168);
		registerRequest.setLongitude(-3.7038);

		updateRequest = new UpdatePushDeviceZoneRequest();
		updateRequest.setEndpoint("https://example.com/push");
		updateRequest.setZoneKey("new-zone");
		updateRequest.setLatitude(41.0);
		updateRequest.setLongitude(-4.0);

		unregisterRequest = new UnregisterPushDeviceRequest();
		unregisterRequest.setEndpoint("https://example.com/push");
	}

	@Test
	@DisplayName("Should register device successfully")
	@WithMockUser(username = "test@example.com")
	void testRegister() throws Exception {
		doNothing().when(pushDeviceService).registerDevice(any(RegisterPushDeviceRequest.class));

		mockMvc.perform(post("/api/push-devices/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest))
				.with(csrf()))
				.andExpect(status().isOk());

		verify(pushDeviceService, times(1)).registerDevice(any(RegisterPushDeviceRequest.class));
	}

	@Test
	@DisplayName("Should return 400 when registering with invalid request")
	@WithMockUser(username = "test@example.com")
	void testRegisterInvalidRequest() throws Exception {
		doThrow(new IllegalArgumentException("Invalid request")).when(pushDeviceService)
				.registerDevice(any(RegisterPushDeviceRequest.class));

		mockMvc.perform(post("/api/push-devices/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest))
				.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should update zone successfully")
	@WithMockUser(username = "test@example.com")
	void testUpdateZone() throws Exception {
		doNothing().when(pushDeviceService).updateDeviceZone(any(UpdatePushDeviceZoneRequest.class));

		mockMvc.perform(post("/api/push-devices/zone")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest))
				.with(csrf()))
				.andExpect(status().isOk());

		verify(pushDeviceService, times(1)).updateDeviceZone(any(UpdatePushDeviceZoneRequest.class));
	}

	@Test
	@DisplayName("Should unregister device successfully")
	@WithMockUser(username = "test@example.com")
	void testUnregister() throws Exception {
		doNothing().when(pushDeviceService).unregisterDevice(any(UnregisterPushDeviceRequest.class));

		mockMvc.perform(post("/api/push-devices/unregister")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(unregisterRequest))
				.with(csrf()))
				.andExpect(status().isOk());

		verify(pushDeviceService, times(1)).unregisterDevice(any(UnregisterPushDeviceRequest.class));
	}

	@Test
	@DisplayName("Should deny access without authentication")
	void testRegisterWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/api/push-devices/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest))
				.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
}
