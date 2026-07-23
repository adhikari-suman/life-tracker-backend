package com.lifetracker.application.labeling;

import com.lifetracker.domain.ledger.OwnerId;

import java.util.UUID;

/**
 * Input to {@link UpdateLabel}. Every change is optional, which is why {@code reparent} exists as its
 * own flag: omitting the parent must mean "leave it alone", while sending it as null must mean "move
 * this to the root". One nullable field cannot say both, so presence is carried separately.
 */
public record UpdateLabelCommand(OwnerId owner, UUID labelId, String name,
                                 boolean reparent, UUID newParentId, Boolean archived) {
}
