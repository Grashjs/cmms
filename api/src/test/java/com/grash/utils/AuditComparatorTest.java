package com.grash.utils;

import com.grash.model.abstracts.Audit;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditComparatorTest {

    private static class TestAudit extends Audit {
        TestAudit(Date createdAt) {
            setCreatedAt(createdAt);
        }
    }

    private final AuditComparator comparator = new AuditComparator();

    private static Date date(long millis) {
        return new Date(millis);
    }

    @Test
    void earlierDate_comesBeforeLaterDate() {
        TestAudit earlier = new TestAudit(date(1000));
        TestAudit later = new TestAudit(date(2000));
        assertTrue(comparator.compare(earlier, later) < 0);
    }

    @Test
    void laterDate_comesAfterEarlierDate() {
        TestAudit earlier = new TestAudit(date(1000));
        TestAudit later = new TestAudit(date(2000));
        assertTrue(comparator.compare(later, earlier) > 0);
    }

    @Test
    void equalDates_returnZero() {
        TestAudit a = new TestAudit(date(1000));
        TestAudit b = new TestAudit(date(1000));
        assertEquals(0, comparator.compare(a, b));
        assertEquals(0, comparator.compare(b, a));
    }

    @Test
    void sortsListChronologically() {
        List<Audit> audits = Arrays.asList(
                new TestAudit(date(3000)),
                new TestAudit(date(1000)),
                new TestAudit(date(2000)));

        audits.sort(comparator);

        assertEquals(1000, audits.get(0).getCreatedAt().getTime());
        assertEquals(2000, audits.get(1).getCreatedAt().getTime());
        assertEquals(3000, audits.get(2).getCreatedAt().getTime());
    }

    @Test
    void min_returnsEarliest() {
        List<Audit> audits = Arrays.asList(
                new TestAudit(date(3000)),
                new TestAudit(date(1000)),
                new TestAudit(date(2000)));

        Audit min = Collections.min(audits, comparator);

        assertEquals(1000, min.getCreatedAt().getTime());
    }
}
