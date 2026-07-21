package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ShareLink;

/** The Share Link, and whether this call minted it ({@code true}) or returned an existing one. */
public record CreateShareLinkResult(ShareLink shareLink, boolean created) {
}
