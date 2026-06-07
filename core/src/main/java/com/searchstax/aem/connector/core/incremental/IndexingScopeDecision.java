package com.searchstax.aem.connector.core.incremental;

public final class IndexingScopeDecision {

    private final boolean accepted;
    private final String reason;

    private IndexingScopeDecision(final boolean accepted, final String reason) {
        this.accepted = accepted;
        this.reason = reason;
    }

    public static IndexingScopeDecision accept() {
        return new IndexingScopeDecision(true, null);
    }

    public static IndexingScopeDecision reject(final String reason) {
        return new IndexingScopeDecision(false, reason);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getReason() {
        return reason;
    }
}
