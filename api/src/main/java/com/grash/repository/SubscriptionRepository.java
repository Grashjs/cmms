package com.grash.repository;

import com.grash.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByPaddleSubscriptionId(String id);

    @Query("SELECT s FROM Subscription s WHERE s.endsOn !=null and s.subscriptionPlan.code!='FREE'")
    List<Subscription> findPaidAndEnding();
}
