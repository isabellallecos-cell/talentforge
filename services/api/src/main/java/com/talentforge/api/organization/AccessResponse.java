package com.talentforge.api.organization;

import java.util.List;
import java.util.UUID;

public record AccessResponse(UUID organizationId, String role, List<String> permissions) {
}
