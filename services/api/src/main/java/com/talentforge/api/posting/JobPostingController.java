package com.talentforge.api.posting;

import com.talentforge.api.identity.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recruiter side of the careers site: publish approved requisitions and close postings. */
@RestController
@RequestMapping("/v1/organizations/{organizationId}/jobs")
public class JobPostingController {

    private final JobPostingService service;

    public JobPostingController(JobPostingService service) {
        this.service = service;
    }

    @GetMapping
    public List<JobPostingResponse> list(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId, @RequestParam(required = false) UUID requisitionId) {
        return service.list(user.getId(), organizationId, Optional.ofNullable(requisitionId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobPostingResponse publish(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId, @Valid @RequestBody PublishRequest request) {
        return service.publish(user.getId(), organizationId, request.requisitionId());
    }

    @PostMapping("/{postingId}/close")
    public JobPostingResponse close(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID organizationId,
            @PathVariable UUID postingId, @Valid @RequestBody CloseRequest request) {
        return service.close(user.getId(), organizationId, postingId, request.version());
    }
}
