package com.talentforge.api.requisition;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmploymentTypeConverter implements AttributeConverter<EmploymentType, String> {

    @Override
    public String convertToDatabaseColumn(EmploymentType value) {
        return value == null ? null : value.dbValue();
    }

    @Override
    public EmploymentType convertToEntityAttribute(String value) {
        return value == null ? null : EmploymentType.fromDbValue(value);
    }
}
