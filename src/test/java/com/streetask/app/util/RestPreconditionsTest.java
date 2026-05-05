package com.streetask.app.util; // Asegúrate de que este es el paquete correcto

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class RestPreconditionsTest {

    @Test
    void privateConstructorShouldThrowAssertionError() throws NoSuchMethodException {
        Constructor<RestPreconditions> constructor = RestPreconditions.class.getDeclaredConstructor();

        assertThatThrownBy(() -> constructor.newInstance())
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(AssertionError.class);
    }
}