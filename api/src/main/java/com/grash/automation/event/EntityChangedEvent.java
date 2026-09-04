package com.grash.automation.event;

import java.util.Set;
import java.util.UUID;

/**
 * An entity changed in a way a rule might care about.
 *
 * <p>Carries ids and primitives only, never an entity. A listener runs after the transaction
 * committed and on another thread; an entity travelling in the event would arrive detached and
 * possibly stale. The same reasoning as {@code com.grash.event.RequestCreatedEvent}, which this
 * generalises.
 *
 * <p>Three fields exist purely because the listener runs somewhere else:
 *
 * <ul>
 *   <li><b>{@code actorUserId}</b> — there is no {@code SecurityContext} on the executor thread,
 *       so anything that wants to know who caused the change has to be told. Null means the
 *       change itself had no logged-in actor (the request portal, a scheduled job, or another
 *       rule).</li>
 *   <li><b>{@code correlationId}</b> — the identity of the whole cascade, not of this event. Two
 *       runs of the same rule on the same entity within one cascade are a loop, and the only way
 *       to recognise that later is to have carried the root's id along.</li>
 *   <li><b>{@code depth}</b> — how many rule executions deep this event already is. A
 *       ThreadLocal cannot survive the AFTER_COMMIT hop, so the counter travels in the event or
 *       it does not exist.</li>
 * </ul>
 *
 * @param changedFields for {@link ChangeType#UPDATED}, the field names that actually differ;
 *                      empty for everything else. A rule can filter on it, which is both an
 *                      optimisation and the first of the loop guards.
 */
public record EntityChangedEvent(
        ChangeType changeType,
        EntityType entityType,
        Long entityId,
        Long companyId,
        Set<String> changedFields,
        Long actorUserId,
        UUID correlationId,
        int depth) {

    /** The common case: a change caused by a person, starting a fresh cascade. */
    public static EntityChangedEvent root(ChangeType changeType,
                                          EntityType entityType,
                                          Long entityId,
                                          Long companyId,
                                          Set<String> changedFields,
                                          Long actorUserId) {
        return new EntityChangedEvent(changeType, entityType, entityId, companyId, changedFields,
                actorUserId, UUID.randomUUID(), 0);
    }

    /** An event caused by a rule action, one level deeper in the same cascade. */
    public EntityChangedEvent child(ChangeType childChangeType,
                                    EntityType childEntityType,
                                    Long childEntityId,
                                    Set<String> childChangedFields) {
        return new EntityChangedEvent(childChangeType, childEntityType, childEntityId, companyId,
                childChangedFields, actorUserId, correlationId, depth + 1);
    }
}
