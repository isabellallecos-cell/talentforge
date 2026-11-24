package com.talentforge.api.requisition;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequisitionActionConverter implements AttributeConverter<RequisitionAction, String> {

    @Override
    public String convertToDatabaseColumn(RequisitionAction value) {
        return value == null ? null : value.dbValue();
    }

    @Override
    public RequisitionAction convertToEntityAttribute(String value) {
        return value == null ? null : RequisitionAction.fromDbValue(value);
    }
}
