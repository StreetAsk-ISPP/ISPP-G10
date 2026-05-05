package com.streetask.app.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class NamedEntityTest {

    @Test
    void shouldSetAndGetPropertiesAndToString() {
        NamedEntity entity = new NamedEntity();
        String testName = "Streetask Test";

        entity.setName(testName);

        assertThat(entity.getName()).isEqualTo(testName);

        assertThat(entity.toString()).isEqualTo(testName);
    }
}