package com.streetask.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.auth.payload.request.BusinessSignupRequest;
import com.streetask.app.auth.payload.request.CompleteSignupRequest;
import com.streetask.app.auth.payload.request.SignupRequest;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.business.BusinessAccountRepository;
import com.streetask.app.business.RequestStatus;
import com.streetask.app.user.AccountType;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.AuthoritiesService;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.User;
import com.streetask.app.user.UserService;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AuthoritiesService authoritiesService;

    @Mock
    private UserService userService;

    @Mock
    private RegularUserRepository regularUserRepository;

    @Mock
    private BusinessAccountRepository businessAccountRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "entityManager", entityManager);
    }

    @Test
    void createBasicUserShouldEncodePasswordSetDefaultsAssignUserAuthorityAndDelegatePersistence() {
        SignupRequest request = new SignupRequest();
        request.setEmail("basic@streetask.com");
        request.setUserName("basicUser");
        request.setPassword("plain-password");
        request.setFirstName("Basic");
        request.setLastName("User");

        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");

        User savedUser = new User();
        savedUser.setEmail("basic@streetask.com");

        when(encoder.encode("plain-password")).thenReturn("encoded-password");
        when(authoritiesService.findByAuthority("USER")).thenReturn(userAuthority);
        when(userService.saveUser(any(User.class))).thenReturn(savedUser);

        User result = authService.createBasicUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(userCaptor.capture());

        User userToSave = userCaptor.getValue();
        assertThat(userToSave.getEmail()).isEqualTo("basic@streetask.com");
        assertThat(userToSave.getUserName()).isEqualTo("basicUser");
        assertThat(userToSave.getPassword()).isEqualTo("encoded-password");
        assertThat(userToSave.getFirstName()).isEqualTo("Basic");
        assertThat(userToSave.getLastName()).isEqualTo("User");
        assertThat(userToSave.getActive()).isFalse();
        assertThat(userToSave.getCreatedAt()).isNotNull();
        assertThat(userToSave.getAuthority()).isEqualTo(userAuthority);

        verify(encoder).encode("plain-password");
        verify(authoritiesService).findByAuthority("USER");
        assertThat(result).isEqualTo(savedUser);
    }

    @Test
    void createRegularUserShouldCopyBaseFieldsSetDefaultsDeleteBasicUserFlushAndSaveRegularUser() {
        CompleteSignupRequest request = new CompleteSignupRequest();
        request.setEmail("regular@streetask.com");

        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");

        User basicUser = new User();
        basicUser.setId(UUID.randomUUID());
        basicUser.setEmail("regular@streetask.com");
        basicUser.setUserName("regularUser");
        basicUser.setPassword("encoded-password");
        basicUser.setFirstName("Regular");
        basicUser.setLastName("User");
        basicUser.setCreatedAt(LocalDateTime.of(2026, 3, 10, 12, 0));

        when(userService.findUser("regular@streetask.com")).thenReturn(basicUser);
        when(authoritiesService.findByAuthority("USER")).thenReturn(userAuthority);

        authService.createRegularUser(request);

        ArgumentCaptor<RegularUser> regularUserCaptor = ArgumentCaptor.forClass(RegularUser.class);
        verify(regularUserRepository).save(regularUserCaptor.capture());

        RegularUser savedRegularUser = regularUserCaptor.getValue();
        assertThat(savedRegularUser.getEmail()).isEqualTo(basicUser.getEmail());
        assertThat(savedRegularUser.getUserName()).isEqualTo(basicUser.getUserName());
        assertThat(savedRegularUser.getPassword()).isEqualTo(basicUser.getPassword());
        assertThat(savedRegularUser.getFirstName()).isEqualTo(basicUser.getFirstName());
        assertThat(savedRegularUser.getLastName()).isEqualTo(basicUser.getLastName());
        assertThat(savedRegularUser.getCreatedAt()).isEqualTo(basicUser.getCreatedAt());

        assertThat(savedRegularUser.getAccountType()).isEqualTo(AccountType.REGULAR_USER);
        assertThat(savedRegularUser.getActive()).isTrue();
        assertThat(savedRegularUser.getCoinBalance()).isEqualTo(0);
        assertThat(savedRegularUser.getRating()).isEqualTo(0.0f);
        assertThat(savedRegularUser.getVerified()).isFalse();
        assertThat(savedRegularUser.getVisibilityRadiusKm()).isEqualTo(10.0f);
        assertThat(savedRegularUser.getPremiumActive()).isFalse();
        assertThat(savedRegularUser.getAuthority()).isEqualTo(userAuthority);

        verify(userService).findUser("regular@streetask.com");
        verify(authoritiesService).findByAuthority("USER");

        var inOrder = inOrder(userService, entityManager, regularUserRepository);
        inOrder.verify(userService).deleteUser(basicUser.getId());
        inOrder.verify(entityManager).flush();
        inOrder.verify(regularUserRepository).save(any(RegularUser.class));
    }

    @Test
    void convertToBusinessUserShouldCopyBaseFieldsSetBusinessFieldsDeleteBasicUserFlushAndSaveBusinessUser() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business@streetask.com");
        request.setTaxId("B12345678");
        request.setCompanyName("StreetAsk Business");
        request.setAddress("Calle Real 123");
        request.setWebsite("https://streetask-business.com");
        request.setDescription("Business description");

        Authorities businessAuthority = new Authorities();
        businessAuthority.setAuthority("BUSINESS");

        User basicUser = new User();
        basicUser.setId(UUID.randomUUID());
        basicUser.setEmail("business@streetask.com");
        basicUser.setUserName("businessUser");
        basicUser.setPassword("encoded-password");
        basicUser.setFirstName("Business");
        basicUser.setLastName("Owner");
        basicUser.setCreatedAt(LocalDateTime.of(2026, 3, 10, 10, 30));

        when(userService.findUser("business@streetask.com")).thenReturn(basicUser);
        when(authoritiesService.findByAuthority("BUSINESS")).thenReturn(businessAuthority);

        authService.convertToBusinessUser(request);

        ArgumentCaptor<BusinessAccount> businessCaptor = ArgumentCaptor.forClass(BusinessAccount.class);
        verify(businessAccountRepository).save(businessCaptor.capture());

        BusinessAccount savedBusiness = businessCaptor.getValue();
        assertThat(savedBusiness.getEmail()).isEqualTo(basicUser.getEmail());
        assertThat(savedBusiness.getUserName()).isEqualTo(basicUser.getUserName());
        assertThat(savedBusiness.getPassword()).isEqualTo(basicUser.getPassword());
        assertThat(savedBusiness.getFirstName()).isEqualTo(basicUser.getFirstName());
        assertThat(savedBusiness.getLastName()).isEqualTo(basicUser.getLastName());
        assertThat(savedBusiness.getCreatedAt()).isEqualTo(basicUser.getCreatedAt());

        assertThat(savedBusiness.getAccountType()).isEqualTo(AccountType.BUSINESS);
        assertThat(savedBusiness.getActive()).isFalse();

        assertThat(savedBusiness.getTaxId()).isEqualTo("B12345678");
        assertThat(savedBusiness.getCompanyName()).isEqualTo("StreetAsk Business");
        assertThat(savedBusiness.getAddress()).isEqualTo("Calle Real 123");
        assertThat(savedBusiness.getWebsite()).isEqualTo("https://streetask-business.com");
        assertThat(savedBusiness.getDescription()).isEqualTo("Business description");

        assertThat(savedBusiness.getVerified()).isFalse();
        assertThat(savedBusiness.getRating()).isEqualTo(0.0f);
        assertThat(savedBusiness.getRequestStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(savedBusiness.getSubscriptionActive()).isFalse();
        assertThat(savedBusiness.getAuthority()).isEqualTo(businessAuthority);

        verify(userService).findUser("business@streetask.com");
        verify(authoritiesService).findByAuthority("BUSINESS");

        var inOrder = inOrder(userService, entityManager, businessAccountRepository);
        inOrder.verify(userService).deleteUser(basicUser.getId());
        inOrder.verify(entityManager).flush();
        inOrder.verify(businessAccountRepository).save(any(BusinessAccount.class));
    }
}
