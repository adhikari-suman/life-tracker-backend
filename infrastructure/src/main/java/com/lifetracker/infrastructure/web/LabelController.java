package com.lifetracker.infrastructure.web;

import com.lifetracker.application.labeling.CreateLabel;
import com.lifetracker.application.labeling.CreateLabelCommand;
import com.lifetracker.application.labeling.DeleteLabel;
import com.lifetracker.application.labeling.LabelNotFoundException;
import com.lifetracker.application.labeling.UpdateLabel;
import com.lifetracker.application.labeling.UpdateLabelCommand;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.infrastructure.persistence.labeling.LabelQueryService;
import com.lifetracker.infrastructure.persistence.labeling.LabelView;
import com.lifetracker.infrastructure.web.dto.CreateLabelRequest;
import com.lifetracker.infrastructure.web.dto.LabelResponse;
import com.lifetracker.infrastructure.web.dto.UpdateLabelRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The caller's label tree — what money was FOR (ADR-0014). Create, list, rename/reparent/archive, and
 * delete. Owner-scoped from the token. Thin — parse, call, map.
 */
@RestController
@RequestMapping("/labels")
class LabelController {

    private final CreateLabel createLabel;
    private final UpdateLabel updateLabel;
    private final DeleteLabel deleteLabel;
    private final LabelQueryService query;

    LabelController(CreateLabel createLabel, UpdateLabel updateLabel, DeleteLabel deleteLabel,
                    LabelQueryService query) {
        this.createLabel = createLabel;
        this.updateLabel = updateLabel;
        this.deleteLabel = deleteLabel;
        this.query = query;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LabelResponse create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateLabelRequest request) {
        OwnerId owner = AuthPrincipal.ownerId(jwt);
        LabelId id = createLabel.execute(new CreateLabelCommand(owner, request.name(), request.parentId()));
        return readBack(owner, id);
    }

    @GetMapping
    List<LabelResponse> list(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived) {
        return query.findByOwner(AuthPrincipal.ownerId(jwt), includeArchived).stream()
                .map(LabelController::toResponse).toList();
    }

    @PatchMapping("/{labelId}")
    LabelResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID labelId,
                         @RequestBody UpdateLabelRequest request) {
        OwnerId owner = AuthPrincipal.ownerId(jwt);
        // isParentIdPresent is the difference between "leave the parent alone" and "move to the root":
        // an omitted key and an explicit null are different instructions.
        updateLabel.execute(new UpdateLabelCommand(owner, labelId, request.getName(),
                request.isParentIdPresent(), request.getParentId(), request.getArchived()));
        return readBack(owner, LabelId.of(labelId));
    }

    @DeleteMapping("/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID labelId) {
        deleteLabel.execute(AuthPrincipal.ownerId(jwt), LabelId.of(labelId));
    }

    /** Read back through the query service so the response carries the freshly-computed path. */
    private LabelResponse readBack(OwnerId owner, LabelId id) {
        return query.findByOwner(owner, true).stream()
                .filter(view -> view.id().equals(id.value()))
                .findFirst()
                .map(LabelController::toResponse)
                .orElseThrow(LabelNotFoundException::new);
    }

    private static LabelResponse toResponse(LabelView view) {
        return new LabelResponse(view.id(), view.name(), view.path(), view.parentId(), view.archived());
    }
}
