package com.talentforge.api.requisition;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body of {@code POST …/requisitions} and {@code PUT …/requisitions/{id}}.
 * {@code version} is required on PUT and ignored on POST. Salaries are whole
 * units of {@code salaryCurrency} per year.
 */
public record RequisitionRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 120) String department,
        @NotBlank @Size(max = 120) String location,
        @NotNull @Pattern(regexp = EmploymentType.PATTERN,
                message = "must be one of full_time, part_time, contract, internship") String employmentType,
        @NotNull @Min(1) @Max(50) Integer headcount,
        @PositiveOrZero Integer salaryMin,
        @PositiveOrZero Integer salaryMax,
        @Pattern(regexp = "[A-Z]{3}", message = "must be a three-letter currency code such as EUR") String salaryCurrency,
        @Size(max = 4000) String description,
        @NotNull UUID hiringManagerId,
        Long version) {
}
