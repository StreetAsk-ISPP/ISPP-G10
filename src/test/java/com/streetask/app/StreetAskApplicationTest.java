package com.streetask.app;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class StreetAskApplicationTest {

    @Test
    void main_shouldRunSpringApplication() {
        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {

            mocked.when(() -> SpringApplication.run(StreetAskApplication.class, new String[] {}))
                    .thenReturn(null);

            StreetAskApplication.main(new String[] {});

            mocked.verify(() -> SpringApplication.run(StreetAskApplication.class, new String[] {}));
        }
    }
}