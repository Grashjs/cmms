package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.offline.MobileOfflineLimits;
import com.grash.dto.offline.MobileOfflineSnapshot;
import com.grash.exception.CustomException;
import com.grash.mapper.*;
import com.grash.model.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;

/** Called within the preparation's repeatable-read transaction: counts and entities see one snapshot. */
@Service
@RequiredArgsConstructor
public class MobileOfflineSnapshotService {
    private final EntityManager em;
    private final WorkOrderMapper workOrderMapper;
    private final TaskMapper taskMapper;
    private final CommentMapper commentMapper;
    private final WorkOrderHistoryMapper historyMapper;
    private final ObjectMapper json;
    private final MobileOfflineAccessService access;

    public MobileOfflineSnapshot build(long id, User user) {
        if (!access.existsInCompany(id, user)) throw new CustomException("TARGET_NOT_FOUND", HttpStatus.NOT_FOUND);
        if (!access.canView(id, user)) throw new CustomException("ACCESS_REVOKED", HttpStatus.FORBIDDEN);
        verifyCounts(id);
        WorkOrder workOrder = em.find(WorkOrder.class, id);
        var tasks = em.createQuery("select t from Task t where t.workOrder.id = :id order by t.createdAt, t.id", Task.class)
                .setParameter("id", id).setMaxResults(MobileOfflineLimits.PILOT.tasks() + 1).getResultList();
        var comments = em.createQuery("select c from Comment c where c.workOrder.id = :id order by c.createdAt, c.id", Comment.class)
                .setParameter("id", id).setMaxResults(MobileOfflineLimits.PILOT.comments() + 1).getResultList();
        var history = em.createQuery("select h from WorkOrderHistory h where h.workOrder.id = :id order by h.createdAt, h.id", WorkOrderHistory.class)
                .setParameter("id", id).setMaxResults(MobileOfflineLimits.PILOT.history() + 1).getResultList();
        var snapshot = new MobileOfflineSnapshot(workOrderMapper.toShowDto(workOrder),
                tasks.stream().map(taskMapper::toShowDto).toList(),
                comments.stream().map(commentMapper::toShowDto).toList(),
                history.stream().map(historyMapper::toShowDto).toList(), Instant.now(), true, access.canEdit(id, user));
        verifyEncodedSize(snapshot);
        return snapshot;
    }

    void verifyCounts(long id) {
        var limits = MobileOfflineLimits.PILOT;
        long tasks = count("select count(t) from Task t where t.workOrder.id = :id", id);
        long comments = count("select count(c) from Comment c where c.workOrder.id = :id", id);
        long history = count("select count(h) from WorkOrderHistory h where h.workOrder.id = :id", id);
        if (tasks > limits.tasks() || comments > limits.comments() || history > limits.history()) throw tooLarge();
        long total = 1 + 2 * tasks + comments + history; // Task + task definition both occupy snapshot items.
        String[] nestedCollections = {
                "select count(u) from WorkOrder w join w.assignedTo u where w.id = :id",
                "select count(u) from WorkOrder w join w.team.users u where w.id = :id",
                "select count(c) from WorkOrder w join w.customers c where w.id = :id",
                "select count(f) from WorkOrder w join w.files f where w.id = :id",
                "select count(v) from CustomFieldValue v where v.workOrder.id = :id",
                "select count(o) from CustomFieldValue v join v.customField.options o where v.workOrder.id = :id",
                "select count(o) from Task t join t.taskBase.options o where t.workOrder.id = :id",
                "select count(f) from Task t join t.images f where t.workOrder.id = :id",
                "select count(f) from Comment c join c.files f where c.workOrder.id = :id"
        };
        for (String query : nestedCollections) {
            total += count(query, id);
            if (total > limits.snapshotItems()) throw tooLarge();
        }
    }

    private long count(String query, long id) {
        return em.createQuery(query, Long.class).setParameter("id", id).getSingleResult();
    }

    public void verifyEncodedSize(Object snapshot) {
        // Stream through Jackson's actual UTF-8 encoder; do not allocate an unbounded encoded byte array.
        try {
            json.writeValue(new OutputStream() {
                private long bytes;
                @Override public void write(int b) throws IOException { add(1); }
                @Override public void write(byte[] b, int off, int len) throws IOException { add(len); }
                private void add(int length) throws IOException {
                    bytes += length;
                    if (bytes > MobileOfflineLimits.PILOT.snapshotBytes()) throw new SnapshotSizeException();
                }
            }, snapshot);
        } catch (IOException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof SnapshotSizeException) throw tooLarge();
                cause = cause.getCause();
            }
            throw new IllegalStateException("Could not encode offline snapshot", e);
        }
    }

    private static class SnapshotSizeException extends IOException {}
    private CustomException tooLarge() { return new CustomException("SNAPSHOT_LIMIT_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE); }
}
