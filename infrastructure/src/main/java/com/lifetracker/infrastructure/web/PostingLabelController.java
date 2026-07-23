package com.lifetracker.infrastructure.web;

import com.lifetracker.application.labeling.AssignPostingLabel;
import com.lifetracker.application.labeling.ClearPostingLabel;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.transaction.PostingId;
import com.lifetracker.infrastructure.web.dto.SetPostingLabelRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A posting's label, as a sub-resource rather than a field of the posting — the URL says what the
 * architecture says: a label is metadata attached beside the ledger core, and the posting itself is
 * immutable truth that is never rewritten (ADR-0014).
 *
 * <p>Retagging is allowed at any age. It shifts the label breakdown and nothing else: net worth,
 * per-account spending and income never consult labels.
 */
@RestController
@RequestMapping("/postings/{postingId}/label")
class PostingLabelController {

    private final AssignPostingLabel assignLabel;
    private final ClearPostingLabel clearLabel;

    PostingLabelController(AssignPostingLabel assignLabel, ClearPostingLabel clearLabel) {
        this.assignLabel = assignLabel;
        this.clearLabel = clearLabel;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void set(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postingId,
             @RequestBody SetPostingLabelRequest request) {
        assignLabel.execute(AuthPrincipal.ownerId(jwt), PostingId.of(postingId), LabelId.of(request.labelId()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clear(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postingId) {
        clearLabel.execute(AuthPrincipal.ownerId(jwt), PostingId.of(postingId));
    }
}
