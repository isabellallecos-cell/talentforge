package com.talentforge.api.requisition;

import com.talentforge.api.identity.AppUserDetails;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/organizations/{organizationId}/requisitions")
public class RequisitionController {

    private final RequisitionService service;

    public RequisitionController(RequisitionService service) {
        this.service = service;
    }

    @GetMapping
    public List<RequisitionResponse> list(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId, @RequestParam(required = false) String status) {
        return service.list(user.getId(), organizationId, Optional.ofNullable(status).map(this::parseStatus));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequisitionResponse create(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId, @Valid @RequestBody RequisitionRequest request) {
        return service.create(user.getId(), organizationId, request);
    }

    /** People who can be picked as hiring manager on the requisition form. */
    @GetMapping("/hiring-managers")
    public List<PersonRef> hiringManagers(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId) {
        return service.hiringManagerOptions(user.getId(), organizationId);
    }

    @GetMapping("/{requisitionId}")
    public RequisitionResponse get(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID organizationId,
            @PathVariable UUID requisitionId) {
        return service.get(user.getId(), organizationId, requisitionId);
    }

    /** Replaces a draft's fields; {@code version} must match the stored one. */
    @PutMapping("/{requisitionId}")
    public RequisitionResponse update(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId, @PathVariable UUID requisitionId,
            @Valid @RequestBody RequisitionRequest request) {
        return service.update(user.getId(), organizationId, requisitionId, request);
    }

    @GetMapping("/{requisitionId}/events")
    public List<RequisitionEventResponse> events(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId, @PathVariable UUID requisitionId) {
        return service.history(user.getId(), organizationId, requisitionId);
    }

    /** {@code submit}, {@code withdraw}, {@code approve}, {@code reject} or {@code reopen}. */
    @PostMapping("/{requisitionId}/{action}")
    public RequisitionResponse transition(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId, @PathVariable UUID requisitionId, @PathVariable String action,
            @Valid @RequestBody ActionRequest request) {
        RequisitionAction parsed = RequisitionAction.fromPath(action)
                .filter(RequisitionWorkflow.STATUS_ACTIONS::contains)
                .orElseThrow(() -> new UnknownActionException(action));
        return service.transition(user.getId(), organizationId, requisitionId, parsed, request);
    }

    private RequisitionStatus parseStatus(String value) {
        return Arrays.stream(RequisitionStatus.values())
                .filter(s -> s.dbValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidStatusFilterException(value));
    }

    /** {@code ?status=} is not a known status. */
    public static class InvalidStatusFilterException extends RuntimeException {
        public InvalidStatusFilterException(String value) {
            super("Unknown status filter");
        }
    }

    /** The action segment is not one of the status actions. */
    public static class UnknownActionException extends RuntimeException {
        public UnknownActionException(String value) {
            super("Unknown requisition action");
        }
    }
}
