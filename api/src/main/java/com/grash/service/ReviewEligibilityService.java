package com.grash.service;

import com.grash.model.User;
import com.grash.model.UserAppStats;
import com.grash.repository.UserAppStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReviewEligibilityService {

    private final UserAppStatsRepository userAppStatsRepository;
    private final CacheService cacheService;

    public UserAppStats getOrCreate(User user) {
        UserAppStats stats = user.getAppStats();
        if (stats == null) {
            stats = new UserAppStats();
            stats.setUser(user);
            stats = userAppStatsRepository.save(stats);
            user.setAppStats(stats);
            cacheService.putUserInCache(user);
        }
        return stats;
    }

    public boolean isEligible(User user) {
        UserAppStats stats = user.getAppStats();
        if (stats == null) return false;
        if (stats.isHasRatedApp()) return false;
        if (stats.getCompletedWorkOrders() < 3) return false;
        if (stats.getAppSessions() < 2) return false;

        Date lastPrompt = stats.getLastReviewPromptAt();
        if (lastPrompt != null) {
            long diffMillis = new Date().getTime() - lastPrompt.getTime();
            long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);
            if (diffDays < 14) return false;
        }

        return true;
    }

    public UserAppStats markReviewShown(User user) {
        UserAppStats stats = getOrCreate(user);
        stats.setReviewPromptShownCount(stats.getReviewPromptShownCount() + 1);
        stats.setLastReviewPromptAt(new Date());
        stats = userAppStatsRepository.save(stats);
        cacheService.putUserInCache(user);
        return stats;
    }

    public UserAppStats markReviewClicked(User user) {
        UserAppStats stats = getOrCreate(user);
        stats.setReviewClickCount(stats.getReviewClickCount() + 1);
        stats = userAppStatsRepository.save(stats);
        cacheService.putUserInCache(user);
        return stats;
    }

    public UserAppStats markRated(User user) {
        UserAppStats stats = getOrCreate(user);
        stats.setHasRatedApp(true);
        stats = userAppStatsRepository.save(stats);
        cacheService.putUserInCache(user);
        return stats;
    }

    public UserAppStats incrementSession(User user) {
        UserAppStats stats = getOrCreate(user);
        stats.setAppSessions(stats.getAppSessions() + 1);
        stats = userAppStatsRepository.save(stats);
        cacheService.putUserInCache(user);
        return stats;
    }

    public UserAppStats incrementWorkOrder(User user) {
        UserAppStats stats = getOrCreate(user);
        stats.setCompletedWorkOrders(stats.getCompletedWorkOrders() + 1);
        stats = userAppStatsRepository.save(stats);
        cacheService.putUserInCache(user);
        return stats;
    }

    public UserAppStats setFeedback(User user, String feedback) {
        UserAppStats stats = getOrCreate(user);
        stats.setFeedback(feedback);
        stats = userAppStatsRepository.save(stats);
        cacheService.putUserInCache(user);
        return stats;
    }
}
