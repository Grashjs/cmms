package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.offline.MobileOfflineLimits;
import com.grash.dto.offline.MobileOfflineSnapshot;
import com.grash.dto.offline.MobileOfflineAsset;
import com.grash.dto.offline.MobileOfflineTimePermissions;
import com.grash.dto.offline.MobileOfflineLabor;
import com.grash.dto.offline.MobileOfflineClosure;
import com.grash.model.enums.PlanFeatures;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.model.enums.PermissionEntity;
import java.util.LinkedHashMap;
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
    private final LicenseService license;

    public MobileOfflineSnapshot build(long id, User user) {
        if (!access.existsInCompany(id, user)) throw new CustomException("TARGET_NOT_FOUND", HttpStatus.NOT_FOUND);
        if (!access.canView(id, user)) throw new CustomException("ACCESS_REVOKED", HttpStatus.FORBIDDEN);
        long itemCount = verifyCounts(id);
        WorkOrder workOrder = em.find(WorkOrder.class, id);
        var tasks = em.createQuery("select t from Task t where t.workOrder.id = :id order by t.createdAt, t.id", Task.class)
                .setParameter("id", id).setMaxResults(MobileOfflineLimits.PILOT.tasks() + 1).getResultList();
        var comments = em.createQuery("select c from Comment c where c.workOrder.id = :id order by c.createdAt, c.id", Comment.class)
                .setParameter("id", id).setMaxResults(MobileOfflineLimits.PILOT.comments() + 1).getResultList();
        var history = em.createQuery("select h from WorkOrderHistory h where h.workOrder.id = :id order by h.createdAt, h.id", WorkOrderHistory.class)
                .setParameter("id", id).setMaxResults(MobileOfflineLimits.PILOT.history() + 1).getResultList();
        var labors = em.createQuery("select l from Labor l where l.workOrder.id = :id order by l.id", Labor.class)
                .setParameter("id", id).setMaxResults(MobileOfflineLimits.PILOT.snapshotItems() + 1).getResultList();
        boolean canEdit = access.canEdit(id, user);
        boolean timeLicense = license.hasEntitlement(LicenseEntitlement.TIME_TRACKING);
        var timePermissions = new MobileOfflineTimePermissions(canEdit && timeLicense, canEdit, canEdit,
                canEdit && timeLicense && LaborService.hasManualRole(user) && LaborService.hasManualPlan(user));
        boolean canReadAssets = user.getRole().getViewPermissions().contains(PermissionEntity.ASSETS);
        boolean canScan = canReadAssets && license.hasEntitlement(LicenseEntitlement.NFC_BARCODE);
        var availableAssets = new LinkedHashMap<Long, Asset>();
        if (workOrder.getAsset() != null) availableAssets.put(workOrder.getAsset().getId(), workOrder.getAsset());
        tasks.forEach(task -> {
            Asset asset = task.getTaskBase().getAsset();
            if (asset != null) availableAssets.put(asset.getId(), asset);
        });
        var assets = availableAssets.values().stream().filter(asset -> asset.getCompany().getId().equals(user.getCompany().getId())
                && asset.canBeViewedBy(user)).map(asset -> new MobileOfflineAsset(asset.getId(), asset.getName(),
                asset.getCustomId(), asset.getDescription(), canScan ? asset.getBarCode() : null,
                canScan ? asset.getNfcId() : null, asset.getStatus() == null ? null : asset.getStatus().name(),
                asset.getModel(), asset.getSerialNumber(), asset.getManufacturer(), asset.getPower(), asset.getArea())).toList();
        if (itemCount + assets.size() > MobileOfflineLimits.PILOT.snapshotItems()) throw tooLarge();
        var settings = user.getCompany().getCompanySettings();
        var subscription = user.getCompany().getSubscription();
        boolean signaturePlan = subscription != null && subscription.getSubscriptionPlan() != null
                && subscription.getSubscriptionPlan().getFeatures().contains(PlanFeatures.SIGNATURE);
        var closure = new MobileOfflineClosure(WorkOrderService.requiredClosureFields(user),
                settings != null && settings.getGeneralPreferences() != null && settings.getGeneralPreferences().isAskFeedBackOnWOClosed(),
                workOrder.isRequiredSignature(), canEdit,
                canEdit && signaturePlan && license.hasEntitlement(LicenseEntitlement.SIGNATURE_CAPTURE),
                count("select count(p) from PartQuantity p where p.workOrder.id = :id", id) > 0,
                count("select count(c) from AdditionalCost c where c.workOrder.id = :id", id) > 0);
        var snapshot = new MobileOfflineSnapshot(workOrderMapper.toShowDto(workOrder),
                tasks.stream().map(taskMapper::toShowDto).toList(),
                comments.stream().map(commentMapper::toShowDto).toList(),
                history.stream().map(historyMapper::toShowDto).toList(), Instant.now(), true, canEdit, true, assets, canScan, canReadAssets,
                labors.stream().map(MobileOfflineLabor::from).toList(), timePermissions, workOrder.getTimerStatusRevision(), closure);
        verifyEncodedSize(snapshot);
        return snapshot;
    }

    long verifyCounts(long id) {
        var limits = MobileOfflineLimits.PILOT;
        long tasks = count("select count(t) from Task t where t.workOrder.id = :id", id);
        long comments = count("select count(c) from Comment c where c.workOrder.id = :id", id);
        long history = count("select count(h) from WorkOrderHistory h where h.workOrder.id = :id", id);
        if (tasks > limits.tasks() || comments > limits.comments() || history > limits.history()) throw tooLarge();
        long total = 1 + 2 * tasks + comments + history + 2 * count("select count(l) from Labor l where l.workOrder.id = :id", id);
        String[] nestedCollections = {
                "select count(l.timeCategory) from Labor l where l.workOrder.id = :id",
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
        return total;
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
