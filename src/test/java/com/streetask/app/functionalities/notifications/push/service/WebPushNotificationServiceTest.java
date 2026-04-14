package com.streetask.app.functionalities.notifications.push.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.functionalities.notifications.push.dto.PushMessage;
import com.streetask.app.functionalities.notifications.push.model.PushDevice;
import com.streetask.app.functionalities.notifications.push.repository.PushDeviceRepository;
import com.streetask.app.user.RegularUser;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebPushNotificationService Tests")
class WebPushNotificationServiceTest {

	@Mock
	private PushDeviceRepository pushDeviceRepository;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private WebPushNotificationService webPushNotificationService;

	private RegularUser testUser;
	private PushDevice pushDevice1;
	private PushDevice pushDevice2;
	private PushMessage pushMessage;

	@BeforeEach
	void setUp() {
		testUser = new RegularUser();
		testUser.setId(UUID.randomUUID());
		testUser.setEmail("test@example.com");

		pushDevice1 = new PushDevice();
		pushDevice1.setId(UUID.randomUUID());
		pushDevice1.setEndpoint("https://example.com/push/1");
		pushDevice1.setP256dh("test-p256dh-1");
		pushDevice1.setAuth("test-auth-1");
		pushDevice1.setUser(testUser);
		pushDevice1.setZoneKey("zone-1");
		pushDevice1.setLatitude(40.4168);
		pushDevice1.setLongitude(-3.7038);
		pushDevice1.setNotificationsEnabled(true);

		pushDevice2 = new PushDevice();
		pushDevice2.setId(UUID.randomUUID());
		pushDevice2.setEndpoint("https://example.com/push/2");
		pushDevice2.setP256dh("test-p256dh-2");
		pushDevice2.setAuth("test-auth-2");
		pushDevice2.setUser(testUser);
		pushDevice2.setZoneKey("zone-1");
		pushDevice2.setLatitude(41.0);
		pushDevice2.setLongitude(-4.0);
		pushDevice2.setNotificationsEnabled(true);

		pushMessage = PushMessage.builder()
				.type("QUESTION")
				.title("New Question")
				.body("A new question in your area")
				.referenceId(UUID.randomUUID())
				.referenceType("QUESTION")
				.questionLatitude(40.4168)
				.questionLongitude(-3.7038)
				.radiusKm(5.0)
				.build();

		// Set subject, publicKey, privateKey using reflection
		ReflectionTestUtils.setField(webPushNotificationService, "subject", "mailto:test@example.com");
		ReflectionTestUtils.setField(webPushNotificationService, "publicKey",
				"BCzmM4oFvgyIoji531RyMjAMxwSEcgRHivSvGBtDeP93MssCAQdfnZZlZ-24mpUMGlCRselBYpHj1onx9eHwqcQ");
		ReflectionTestUtils.setField(webPushNotificationService, "privateKey",
				"xS6S6T2vAkW3mr43IsHOeb6J1DzkXA2AxcGn88z7uq8");
	}

	@Test
	@DisplayName("Should send notification to single user with devices")
	void testSendToUserWithDevices() {
		when(pushDeviceRepository.findByUserEmailAndNotificationsEnabledTrue("test@example.com"))
				.thenReturn(Arrays.asList(pushDevice1, pushDevice2));

		webPushNotificationService.sendToUser("test@example.com", pushMessage);

		verify(pushDeviceRepository, times(1))
				.findByUserEmailAndNotificationsEnabledTrue("test@example.com");
	}

	@Test
	@DisplayName("Should not send notification when user has no devices")
	void testSendToUserNoDevices() {
		when(pushDeviceRepository.findByUserEmailAndNotificationsEnabledTrue("test@example.com"))
				.thenReturn(Arrays.asList());

		webPushNotificationService.sendToUser("test@example.com", pushMessage);

		verify(pushDeviceRepository, times(1))
				.findByUserEmailAndNotificationsEnabledTrue("test@example.com");
	}

	@Test
	@DisplayName("Should send notification to zones")
	void testSendToZones() {
		Set<String> zoneKeys = new HashSet<>(Arrays.asList("zone-1", "zone-2"));

		when(pushDeviceRepository.findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys))
				.thenReturn(Arrays.asList(pushDevice1, pushDevice2));

		webPushNotificationService.sendToZones(zoneKeys, pushMessage);

		verify(pushDeviceRepository, times(1)).findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys);
	}

	@Test
	@DisplayName("Should not send notification when zone set is empty")
	void testSendToZonesEmpty() {
		Set<String> zoneKeys = new HashSet<>();

		webPushNotificationService.sendToZones(zoneKeys, pushMessage);

		verify(pushDeviceRepository, never()).findByZoneKeyInAndNotificationsEnabledTrue(any());
	}

	@Test
	@DisplayName("Should not send notification when zone set is null")
	void testSendToZonesNull() {
		webPushNotificationService.sendToZones(null, pushMessage);

		verify(pushDeviceRepository, never()).findByZoneKeyInAndNotificationsEnabledTrue(any());
	}

	@Test
	@DisplayName("Should filter devices by distance")
	void testSendToZonesWithDistanceFilter() {
		// Create a device far away
		PushDevice farDevice = new PushDevice();
		farDevice.setId(UUID.randomUUID());
		farDevice.setEndpoint("https://example.com/push/3");
		farDevice.setP256dh("test-p256dh-3");
		farDevice.setAuth("test-auth-3");
		farDevice.setUser(testUser);
		farDevice.setZoneKey("zone-1");
		farDevice.setLatitude(45.0); // Far away
		farDevice.setLongitude(5.0);
		farDevice.setNotificationsEnabled(true);

		Set<String> zoneKeys = new HashSet<>(Arrays.asList("zone-1"));

		when(pushDeviceRepository.findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys))
				.thenReturn(Arrays.asList(pushDevice1, pushDevice2, farDevice));

		webPushNotificationService.sendToZones(zoneKeys, pushMessage);

		verify(pushDeviceRepository, times(1)).findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys);
	}

	@Test
	@DisplayName("Should not filter when radius is null")
	void testSendToZonesNoRadiusFilter() {
		PushMessage messageNoRadius = PushMessage.builder()
				.type("QUESTION")
				.title("New Question")
				.body("A new question")
				.questionLatitude(40.4168)
				.questionLongitude(-3.7038)
				.build();

		Set<String> zoneKeys = new HashSet<>(Arrays.asList("zone-1"));

		when(pushDeviceRepository.findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys))
				.thenReturn(Arrays.asList(pushDevice1, pushDevice2));

		webPushNotificationService.sendToZones(zoneKeys, messageNoRadius);

		verify(pushDeviceRepository, times(1)).findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys);
	}

	@Test
	@DisplayName("Should skip device without coordinates")
	void testSendToZonesDeviceNoCoordinates() {
		PushDevice deviceNoCoords = new PushDevice();
		deviceNoCoords.setId(UUID.randomUUID());
		deviceNoCoords.setEndpoint("https://example.com/push/4");
		deviceNoCoords.setP256dh("test-p256dh-4");
		deviceNoCoords.setAuth("test-auth-4");
		deviceNoCoords.setUser(testUser);
		deviceNoCoords.setZoneKey("zone-1");
		// No latitude/longitude
		deviceNoCoords.setNotificationsEnabled(true);

		Set<String> zoneKeys = new HashSet<>(Arrays.asList("zone-1"));

		when(pushDeviceRepository.findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys))
				.thenReturn(Arrays.asList(pushDevice1, deviceNoCoords));

		webPushNotificationService.sendToZones(zoneKeys, pushMessage);

		verify(pushDeviceRepository, times(1)).findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys);
	}

	@Test
	@DisplayName("Should handle negative radius gracefully")
	void testSendToZonesNegativeRadius() {
		PushMessage messageNegativeRadius = PushMessage.builder()
				.type("QUESTION")
				.title("New Question")
				.body("A new question")
				.questionLatitude(40.4168)
				.questionLongitude(-3.7038)
				.radiusKm(-5.0)
				.build();

		Set<String> zoneKeys = new HashSet<>(Arrays.asList("zone-1"));

		when(pushDeviceRepository.findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys))
				.thenReturn(Arrays.asList(pushDevice1, pushDevice2));

		webPushNotificationService.sendToZones(zoneKeys, messageNegativeRadius);

		verify(pushDeviceRepository, times(1)).findByZoneKeyInAndNotificationsEnabledTrue(zoneKeys);
	}
}
