package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ShareToken;
import com.lifetracker.domain.sharing.ShareTokens;

/** Deterministic {@link ShareTokens} fake — a new "share-token-N" each call. */
final class FakeShareTokens implements ShareTokens {

    private int counter = 0;

    @Override
    public ShareToken generate() {
        return new ShareToken("share-token-" + (++counter));
    }
}
