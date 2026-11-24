package com.talentforge.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RoleTest {

    private final RoleConverter converter = new RoleConverter();

    @ParameterizedTest
    @EnumSource(Role.class)
    void roundTripsThroughDatabaseValue(Role role) {
        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(role))).isEqualTo(role);
    }

    @Test
    void usesFixtureRoleNames() {
        assertThat(Role.HIRING_MANAGER.dbValue()).isEqualTo("hiring_manager");
        assertThat(Role.fromDbValue("recruiter")).isEqualTo(Role.RECRUITER);
    }

    @Test
    void rejectsUnknownRole() {
        assertThatThrownBy(() -> Role.fromDbValue("RECRUITER")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsNullBothWays() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
