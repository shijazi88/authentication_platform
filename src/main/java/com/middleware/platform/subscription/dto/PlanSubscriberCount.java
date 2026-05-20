package com.middleware.platform.subscription.dto;

import java.util.UUID;

/**
 * Aggregate count of subscriptions per plan. Used by the plans list page
 * to show how many clients are on each plan without firing one request
 * per row.
 */
public record PlanSubscriberCount(
        UUID planId,
        long total,
        long active
) {}
