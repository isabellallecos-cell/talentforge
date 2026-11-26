package com.talentforge.api.posting;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/** Whether a posting is listed on the public careers site and accepts applications. */
public enum PostingStatus {
    OPEN("open"),
    CLOSED("closed");

    private final String dbValue;

    PostingStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static PostingStatus fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown posting status: " + value));
    }

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<PostingStatus, String> {

        @Override
        public String convertToDatabaseColumn(PostingStatus value) {
            return value == null ? null : value.dbValue();
        }

        @Override
        public PostingStatus convertToEntityAttribute(String value) {
            return value == null ? null : fromDbValue(value);
        }
    }
}
