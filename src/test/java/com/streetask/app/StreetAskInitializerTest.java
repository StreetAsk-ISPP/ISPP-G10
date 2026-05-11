package com.streetask.app;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.boot.builder.SpringApplicationBuilder;

class StreetAskInitializerTest {

    @Test
    void configureShouldSetApplicationSources() {
        StreetAskInitializer initializer = new StreetAskInitializer();

        SpringApplicationBuilder builderMock = mock(SpringApplicationBuilder.class);

        when(builderMock.sources(StreetAskApplication.class)).thenReturn(builderMock);

        SpringApplicationBuilder result = initializer.configure(builderMock);

        verify(builderMock).sources(StreetAskApplication.class);
        assertThat(result).isEqualTo(builderMock);
    }
}
