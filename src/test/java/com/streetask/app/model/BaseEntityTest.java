package com.streetask.app.model;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    @Test
    void shouldSetAndGetId() {
        BaseEntity entity = new BaseEntity();
        UUID testId = UUID.randomUUID();

        entity.setId(testId);

        assertThat(entity.getId()).isEqualTo(testId);
    }

    @Test
    void isNewShouldReturnTrueWhenIdIsNull() {
        BaseEntity entity = new BaseEntity();

        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void isNewShouldReturnFalseWhenIdIsSet() {
        BaseEntity entity = new BaseEntity();
        entity.setId(UUID.randomUUID());

        assertThat(entity.isNew()).isFalse();
    }

    @Test
    void equalsAndHashCodeShouldCheckIdOnly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        BaseEntity entity1 = new BaseEntity();
        entity1.setId(id1);

        BaseEntity entity2 = new BaseEntity();
        entity2.setId(id1);

        BaseEntity entity3 = new BaseEntity();
        entity3.setId(id2);

        assertThat(entity1).isEqualTo(entity1);
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1).isNotEqualTo(entity3);
        assertThat(entity1).isNotEqualTo(null);
        assertThat(entity1).isNotEqualTo(new Object());

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        assertThat(entity1.hashCode()).isNotEqualTo(entity3.hashCode());
    }
}