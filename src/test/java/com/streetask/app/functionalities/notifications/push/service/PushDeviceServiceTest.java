package com.streetask.app.functionalities.notifications.push.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.functionalities.notifications.push.dto.RegisterPushDeviceRequest;
import com.streetask.app.functionalities.notifications.push.dto.UnregisterPushDeviceRequest;
import com.streetask.app.functionalities.notifications.push.dto.UpdatePushDeviceZoneRequest;
import com.streetask.app.functionalities.notifications.push.model.PushDevice;
import com.streetask.app.functionalities.notifications.push.repository.PushDeviceRepository;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushDeviceService Tests")
class PushDeviceServiceTest {

	@Mock
	private PushDeviceRepository pushDeviceRepository;

	@Mock
	private RegularUserRepository regularUserRepository;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	@InjectMocks
	private PushDeviceService pushDeviceService;

	private RegularUser testUser;
	private RegisterPushDeviceRequest registerRequest;
	private PushDevice pushDevice;

	@BeforeEach
	void setUp() {
		testUser = new RegularUser();
		testUser.setId(UUID.randomUUID());
		testUser.setEmail("test@example.com");

		pushDevice = new PushDevice();
		pushDevice.setId(UUID.randomUUID());
		pushDevice.setEndpoint("https://example.com/push");
		pushDevice.setP256dh("test-p256dh");
		pushDevice.setAuth("test-auth");
		pushDevice.setUser(testUser);
		pushDevice.setZoneKey("zone-1");
		pushDevice.setLatitude(40.4168);
		pushDevice.setLongitude(-3.7038);
		pushDevice.setNotificationsEnabled(true);

		registerRequest = new RegisterPushDeviceRequest();
		registerRequest.setEndpoint("https://example.com/push");
		registerRequest.setP256dh("test-p256dh");
		registerRequest.setAuth("test-auth");
		registerRequest.setZoneKey("zone-1");
		registerRequest.setLatitude(40.4168);
		registerRequest.setLongitude(-3.7038);
	}

	private void mockAuthentication(String email) {
		SecurityContextHolder.setContext(securityContext);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.getName()).thenReturn(email);
	}

	@Test
	@DisplayName("Should register device successfully")
	void testRegisterDevice() {
		mockAuthentication("test@example.com");
		when(regularUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
		when(pushDeviceRepository.findByEndpoint("https://example.com/push")).thenReturn(Optional.empty());
		when(pushDeviceRepository.save(any(PushDevice.class))).thenReturn(pushDevice);

		pushDeviceService.registerDevice(registerRequest);

		verify(pushDeviceRepository, times(1)).save(any(PushDevice.class));
		verify(regularUserRepository, times(1)).findByEmail("test@example.com");
	}

	@Test
	@DisplayName("Should update existing device when registering with same endpoint")
	void testRegisterDeviceUpdateExisting() {
		mockAuthentication("test@example.com");
		when(regularUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
		when(pushDeviceRepository.findByEndpoint("https://example.com/push")).thenReturn(Optional.of(pushDevice));
		when(pushDeviceRepository.save(any(PushDevice.class))).thenReturn(pushDevice);

		pushDeviceService.registerDevice(registerRequest);

		verify(pushDeviceRepository, times(1)).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when user not found during registration")
	void testRegisterDeviceUserNotFound() {
		mockAuthentication("test@example.com");
		when(regularUserRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.registerDevice(registerRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when endpoint is blank")
	void testRegisterDeviceBlankEndpoint() {
		registerRequest.setEndpoint("");

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.registerDevice(registerRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when p256dh is blank")
	void testRegisterDeviceBlankP256dh() {
		registerRequest.setP256dh("");

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.registerDevice(registerRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when auth is blank")
	void testRegisterDeviceBlankAuth() {
		registerRequest.setAuth("");

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.registerDevice(registerRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when latitude is out of range")
	void testRegisterDeviceInvalidLatitude() {
		registerRequest.setLatitude(91.0);

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.registerDevice(registerRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when longitude is out of range")
	void testRegisterDeviceInvalidLongitude() {
		registerRequest.setLongitude(181.0);

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.registerDevice(registerRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should update device zone successfully")
	void testUpdateDeviceZone() {
		mockAuthentication("test@example.com");
		UpdatePushDeviceZoneRequest updateRequest = new UpdatePushDeviceZoneRequest();
		updateRequest.setEndpoint("https://example.com/push");
		updateRequest.setZoneKey("new-zone");
		updateRequest.setLatitude(41.0);
		updateRequest.setLongitude(-4.0);

		when(pushDeviceRepository.findByEndpoint("https://example.com/push")).thenReturn(Optional.of(pushDevice));
		when(pushDeviceRepository.save(any(PushDevice.class))).thenReturn(pushDevice);

		pushDeviceService.updateDeviceZone(updateRequest);

		verify(pushDeviceRepository, times(1)).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when updating zone for device of another user")
	void testUpdateDeviceZoneUnauthorized() {
		mockAuthentication("test@example.com");
		UpdatePushDeviceZoneRequest updateRequest = new UpdatePushDeviceZoneRequest();
		updateRequest.setEndpoint("https://example.com/push");

		RegularUser otherUser = new RegularUser();
		otherUser.setEmail("other@example.com");
		pushDevice.setUser(otherUser);

		when(pushDeviceRepository.findByEndpoint("https://example.com/push")).thenReturn(Optional.of(pushDevice));

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.updateDeviceZone(updateRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should unregister device successfully")
	void testUnregisterDevice() {
		mockAuthentication("test@example.com");
		UnregisterPushDeviceRequest unregisterRequest = new UnregisterPushDeviceRequest();
		unregisterRequest.setEndpoint("https://example.com/push");

		when(pushDeviceRepository.findByEndpoint("https://example.com/push")).thenReturn(Optional.of(pushDevice));
		when(pushDeviceRepository.save(any(PushDevice.class))).thenReturn(pushDevice);

		pushDeviceService.unregisterDevice(unregisterRequest);

		verify(pushDeviceRepository, times(1)).save(any(PushDevice.class));
	}

	@Test
	@DisplayName("Should throw exception when no authenticated user")
	void testNoAuthenticatedUser() {
		mockAuthentication("");

		assertThrows(IllegalArgumentException.class, () -> pushDeviceService.registerDevice(registerRequest));

		verify(pushDeviceRepository, never()).save(any(PushDevice.class));
	}
}
