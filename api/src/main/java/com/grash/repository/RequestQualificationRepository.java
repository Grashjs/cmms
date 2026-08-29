package com.grash.repository;

import com.grash.model.RequestQualification;
import com.grash.model.enums.QualificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestQualificationRepository extends JpaRepository<RequestQualification, Long> {

    /**
     * The one qualification that is still live for a request: everything except SUPERSEDED,
     * newest first. There should be at most one, but the query tolerates more rather than
     * throwing, because a duplicate is a cosmetic defect and refusing to show anything would be
     * worse than showing the newest.
     *
     * <p>The company id is a parameter rather than being left to {@code CompanyAudit.afterLoad},
     * which is inert on the triage thread (no security context) and would let a caller reach
     * another company's row. Every read path here passes it.
     */
    @Query("SELECT q FROM RequestQualification q "
            + "WHERE q.request.id = :requestId AND q.company.id = :companyId "
            + "AND q.status <> :superseded "
            + "ORDER BY q.createdAt DESC, q.id DESC")
    List<RequestQualification> findLive(@Param("requestId") Long requestId,
                                        @Param("companyId") Long companyId,
                                        @Param("superseded") QualificationStatus superseded);

    default Optional<RequestQualification> findLiveOne(Long requestId, Long companyId) {
        return findLive(requestId, companyId, QualificationStatus.SUPERSEDED).stream().findFirst();
    }

    Optional<RequestQualification> findByIdAndCompany_Id(Long id, Long companyId);

    /**
     * Retires whatever was live for a request before a new run stores its result. A bulk update
     * rather than load-and-save: the rows being retired are never read again, and loading them
     * would pull their candidate lists along for nothing.
     *
     * <p>Deliberately without {@code clearAutomatically}. The usual pairing with a bulk update is
     * to clear the persistence context so nothing stale survives it, and here that would be
     * actively harmful: the caller is holding the request it is about to attach a new
     * qualification to, and clearing detaches it mid-method, so the next lazy access blows up.
     * Nothing in this transaction re-reads the rows being superseded, so there is nothing stale
     * to guard against.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RequestQualification q SET q.status = :superseded "
            + "WHERE q.request.id = :requestId AND q.status <> :superseded")
    int supersedeAllFor(@Param("requestId") Long requestId,
                        @Param("superseded") QualificationStatus superseded);
}
