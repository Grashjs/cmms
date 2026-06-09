package com.grash.service;

import com.grash.model.User;
import com.grash.model.UserAppStats;
import com.grash.repository.UserAppStatsRepository;
import com.grash.utils.Helper;
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
            stats = userAppStatsRepository.save(stats);
            user.setAppStats(stats);
            cacheService.putUserInCache(user);
        }
        return stats;
    }

    public boolean isEligible(UserAppStats stats, Date userCreatedAt) {
        Date lastPrompt = stats.getLastReviewPromptAt();
        if (userCreatedAt != null && lastPrompt == null && Helper.getDateDiff(userCreatedAt,
                new Date(), TimeUnit.DAYS) > 14)
            return true;
        if (stats.isHasRatedApp()) return false;
        if (stats.getCompletedWorkOrders() < 3) return false;
        if (stats.getAppSessions() < 2) return false;
        if (stats.getFeedback() != null && !stats.getFeedback().isEmpty()) return false;

        if (lastPrompt != null) {
            long diffDays = Helper.getDateDiff(lastPrompt, new Date(), TimeUnit.DAYS);
            return diffDays >= 14;
        }

        return true;
    }

    public UserAppStats markReviewShown(UserAppStats stats) {
        stats.setReviewPromptShownCount(stats.getReviewPromptShownCount() + 1);
        stats.setLastReviewPromptAt(new Date());
        return userAppStatsRepository.save(stats);
    }

    public UserAppStats markReviewClicked(UserAppStats stats) {
        stats.setReviewClickCount(stats.getReviewClickCount() + 1);
        return userAppStatsRepository.save(stats);
    }

    public UserAppStats markRated(UserAppStats stats) {
        stats.setHasRatedApp(true);
        return userAppStatsRepository.save(stats);
    }

    public UserAppStats incrementSession(UserAppStats stats) {
        stats.setAppSessions(stats.getAppSessions() + 1);
        return userAppStatsRepository.save(stats);
    }

    public UserAppStats incrementWorkOrder(UserAppStats stats) {
        stats.setCompletedWorkOrders(stats.getCompletedWorkOrders() + 1);
        return userAppStatsRepository.save(stats);
    }

    public UserAppStats setFeedback(UserAppStats stats, String feedback) {
        stats.setFeedback(feedback);
        return userAppStatsRepository.save(stats);
    }
}
