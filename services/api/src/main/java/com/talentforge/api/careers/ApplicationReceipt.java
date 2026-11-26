package com.talentforge.api.careers;

/**
 * Reply to an application. Identical for a first and a repeated submission, so
 * the endpoint does not reveal whether an email address has applied before.
 */
public record ApplicationReceipt(String status, String jobTitle, String organizationName) {
}
