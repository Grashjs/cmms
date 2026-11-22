package com.grash.repository;

import com.grash.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ReadingRepository extends JpaRepository<Reading, Long> {
    @Query("SELECT r from Reading r where r.meter.company.id = :x ")
    Collection<Reading> findByCompany_Id(@Param("x") Long id);

    Collection<Reading> findByMeter_Id(Long id);

    Optional<Reading> findFirstByMeter_IdOrderByCreatedAtDesc(Long id);

    Collection<Reading> findByMeter_IdAndCreatedAtBetween(Long meterId, Date start, Date end);

    /**
     * Date and value of the readings of a meter in the period, oldest first: only two columns, since meters fed
     * automatically can have many readings.
     */
    @Query("select r.createdAt, r.value from Reading r where r.meter.id = :meterId " +
            "and r.createdAt between :start and :end order by r.createdAt, r.id")
    List<Object[]> findDatesAndValues(@Param("meterId") Long meterId, @Param("start") Date start,
                                      @Param("end") Date end);

    Optional<Reading> findFirstByMeter_IdAndCreatedAtBeforeOrderByCreatedAtDesc(Long meterId, Date date);
}
