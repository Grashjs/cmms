package com.grash.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Scalar owner IDs deliberately survive deletion of a business target. Never query by operation ID alone. */
@Repository
@RequiredArgsConstructor
public class MobileOfflineRepository {
    private final JdbcTemplate jdbc;

    public record Receipt(UUID operationId, long workOrderId, String payloadHash, String resultJson) {}

    public boolean reserveOperation(long companyId, long userId, UUID id, long workOrderId, String hash, Instant now) {
        return jdbc.update("""
                insert into mobile_offline_receipt(company_id, user_id, operation_id, work_order_id, payload_hash, received_at)
                values (?, ?, ?, ?, ?, ?) on conflict (company_id, user_id, operation_id) do nothing
                """, companyId, userId, id, workOrderId, hash, java.sql.Timestamp.from(now)) == 1;
    }

    public Optional<Receipt> receipt(long companyId, long userId, UUID id) {
        return jdbc.query("""
                select operation_id, work_order_id, payload_hash, result_json from mobile_offline_receipt
                where company_id = ? and user_id = ? and operation_id = ?
                """, (rs, row) -> new Receipt(rs.getObject(1, UUID.class), rs.getLong(2), rs.getString(3), rs.getString(4)),
                companyId, userId, id).stream().findFirst();
    }

    public void finishOperation(long companyId, long userId, UUID id, String resultJson) {
        jdbc.update("""
                update mobile_offline_receipt set result_json = ?
                where company_id = ? and user_id = ? and operation_id = ?
                """, resultJson, companyId, userId, id);
    }

    public void lockDevice(long companyId, long userId, UUID deviceId) {
        // The UPDATE is intentional: under REPEATABLE READ, a concurrent preparation must fail
        // serialization instead of checking its quota against a snapshot from before this lock.
        jdbc.update("""
                insert into mobile_offline_device(company_id, user_id, device_id) values (?, ?, ?)
                on conflict (company_id, user_id, device_id)
                do update set revision = mobile_offline_device.revision + 1
                """, companyId, userId, deviceId);
    }

    public List<Long> preparedIds(long companyId, long userId, UUID deviceId) {
        // The device lock and the insert guard keep this collection at the fixed pilot quota.
        return jdbc.queryForList("""
                select work_order_id from mobile_offline_preparation
                where company_id = ? and user_id = ? and device_id = ?
                order by work_order_id limit 26
                """, Long.class, companyId, userId, deviceId);
    }

    public void prepare(long companyId, long userId, UUID deviceId, long workOrderId, Instant now) {
        jdbc.update("""
                insert into mobile_offline_preparation(company_id, user_id, device_id, work_order_id, prepared_at)
                values (?, ?, ?, ?, ?) on conflict (company_id, user_id, device_id, work_order_id)
                do update set prepared_at = excluded.prepared_at
                """, companyId, userId, deviceId, workOrderId, java.sql.Timestamp.from(now));
    }

    public void removePreparation(long companyId, long userId, UUID deviceId, long workOrderId) {
        jdbc.update("""
                delete from mobile_offline_preparation
                where company_id = ? and user_id = ? and device_id = ? and work_order_id = ?
                """, companyId, userId, deviceId, workOrderId);
    }
}
