package com.grash.model.enums;

/**
 * Life cycle of a triage suggestion.
 * <p>
 * There is deliberately no FAILED state. If the matcher cannot run, no qualification is written
 * at all and the request looks exactly as it did before triage existed - the admin triages by
 * hand, as always. A row that exists is a row that has something to say.
 */
public enum QualificationStatus {
    /** Written by the matcher, nobody has decided yet. */
    PENDING,
    /** A user took one of the candidates; the request now carries that asset. */
    APPLIED,
    /** A user looked and said none of these. The most valuable state for later stages. */
    REJECTED,
    /** A newer run replaced this one. Kept for the audit trail, never shown. */
    SUPERSEDED
}
