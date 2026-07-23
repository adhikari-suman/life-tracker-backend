package com.lifetracker.application.labeling;

/**
 * There is no single Income or Expense leg for the label to sit on (ADR-0014).
 *
 * <p>Either the transaction moves money only between accounts you hold — an internal transfer, a debt
 * payment, an opening balance — so nothing entered or left your world and there is nothing to
 * categorize; or, far more rarely, BOTH legs are boundary accounts and one label cannot say which was
 * meant.
 *
 * <p>Refused rather than ignored on purpose: attempting to categorize a transfer nearly always means
 * the wrong account kind was picked, and silently dropping the label would hide that.
 */
public class LabelNotApplicableException extends RuntimeException {

    public LabelNotApplicableException(String message) {
        super(message);
    }
}
