package com.lifetracker.domain.labeling;

/**
 * The tree stops at {@link LabelTree#MAX_DEPTH} levels (ADR-0015). Raising the cap later is safe;
 * lowering it would invalidate labels already stored, which is why it starts tight.
 */
public class LabelDepthExceededException extends RuntimeException {

    public LabelDepthExceededException(int attempted) {
        super("labels go at most " + LabelTree.MAX_DEPTH + " levels deep; this would reach " + attempted);
    }
}
