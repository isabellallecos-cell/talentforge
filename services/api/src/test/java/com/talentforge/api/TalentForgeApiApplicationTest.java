package com.talentforge.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class TalentForgeApiApplicationTest {

    @Test
    void isSpringBootApplication() {
        assertThat(TalentForgeApiApplication.class.isAnnotationPresent(SpringBootApplication.class))
                .isTrue();
    }
}
