package com.grash.event;

/**
 * A maintenance request has been created.
 *
 * <p>Carries the id and nothing else, on purpose. A listener runs after the transaction that
 * created the request has committed, and often on another thread; an entity travelling in the
 * event would arrive detached, with its lazy associations unusable and its field values possibly
 * already out of date. An id forces the listener to load what it needs in its own transaction,
 * which is the only version of the request it may trust.
 *
 * @param requestId the request that was created
 * @param companyId the company it belongs to, so a listener can scope its work without loading
 *                  the request first
 */
public record RequestCreatedEvent(Long requestId, Long companyId) {
}
