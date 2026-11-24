package com.talentforge.api.requisition;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequisitionStatusConverter implements AttributeConverter<RequisitionStatus, String> {

    @Override
    public String convertToDatabaseColumn(RequisitionStatus value) {
        return value == null ? null : value.dbValue();
    }

    @Override
    public RequisitionStatus convertToEntityAttribute(String value) {
        return value == null ? null : RequisitionStatus.fromDbValue(value);
    }
}
