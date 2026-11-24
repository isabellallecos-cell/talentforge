package com.talentforge.api.requisition;

import java.util.UUID;

/** A user shown by display name only; emails are not part of requisition responses. */
public record PersonRef(UUID id, String displayName) {
}
