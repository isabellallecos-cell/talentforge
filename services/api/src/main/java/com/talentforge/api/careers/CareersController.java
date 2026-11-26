package com.talentforge.api.careers;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public careers site. No sign-in; writes still need the CSRF token from
 * {@code GET /v1/auth/csrf}.
 */
@RestController
@RequestMapping("/v1/public/organizations/{slug}/jobs")
public class CareersController {

    private final CareersService service;

    public CareersController(CareersService service) {
        this.service = service;
    }

    @GetMapping
    public PublicJobList list(@PathVariable String slug) {
        return service.openJobs(slug);
    }

    @GetMapping("/{postingId}")
    public PublicJob get(@PathVariable String slug, @PathVariable UUID postingId) {
        return service.openJob(slug, postingId);
    }

    /** 202 for a first and a repeated application alike. */
    @PostMapping("/{postingId}/applications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApplicationReceipt apply(@PathVariable String slug, @PathVariable UUID postingId,
            @Valid @RequestBody ApplicationRequest request) {
        return service.apply(slug, postingId, request);
    }
}
