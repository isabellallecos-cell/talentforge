package com.talentforge.api.requisition;

import java.util.UUID;

/** Validated, trimmed fields of a requisition draft. */
public record RequisitionDetails(String title, String department, String location, EmploymentType employmentType,
        int headcount, Integer salaryMin, Integer salaryMax, String salaryCurrency, String description,
        UUID hiringManagerId) {
}
