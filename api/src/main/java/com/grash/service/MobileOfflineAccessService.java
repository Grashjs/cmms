package com.grash.service;

import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** The same work-order rules, evaluated with scalar queries instead of loading unbounded team memberships. */
@Service
@RequiredArgsConstructor
public class MobileOfflineAccessService {
    private final EntityManager em;

    public boolean canView(long id, User user) {
        Object[] owner = owner(id, user);
        if (owner == null) return false;
        if (Objects.equals(owner[1], user.getId())) return true; // Requester of the parent request.
        return user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS)
                && (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS)
                || Objects.equals(owner[0], user.getId()) || assigned(id, user.getId()));
    }

    public boolean canEdit(long id, User user) {
        Object[] owner = owner(id, user);
        return owner != null && (user.getRole().getEditOtherPermissions().contains(PermissionEntity.WORK_ORDERS)
                || Objects.equals(owner[0], user.getId()) || assigned(id, user.getId()));
    }

    public boolean existsInCompany(long id, User user) { return owner(id, user) != null; }

    private Object[] owner(long id, User user) {
        List<Object[]> rows = em.createQuery("""
                select w.createdBy, r.createdBy from WorkOrder w left join w.parentRequest r
                where w.id = :id and w.company.id = :company
                """, Object[].class).setParameter("id", id).setParameter("company", user.getCompany().getId())
                .setMaxResults(1).getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean assigned(long id, long userId) {
        return count("select count(w) from WorkOrder w where w.id = :id and w.primaryUser.id = :user", id, userId) > 0
                || count("select count(w) from WorkOrder w join w.assignedTo u where w.id = :id and u.id = :user", id, userId) > 0
                || count("select count(w) from WorkOrder w join w.team.users u where w.id = :id and u.id = :user", id, userId) > 0;
    }

    private long count(String query, long id, long user) {
        return em.createQuery(query, Long.class).setParameter("id", id).setParameter("user", user).getSingleResult();
    }
}
