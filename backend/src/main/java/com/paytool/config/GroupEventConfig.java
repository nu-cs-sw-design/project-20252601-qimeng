package com.paytool.config;

import com.paytool.service.event.DefaultGroupEventPublisher;
import com.paytool.service.event.subscriber.GraphQLGroupEventSubscriber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up the group event system.
 * Registers all event subscribers with the event publisher.
 */
@Configuration
@RequiredArgsConstructor
public class GroupEventConfig {
    private final DefaultGroupEventPublisher eventPublisher;
    private final GraphQLGroupEventSubscriber graphQLSubscriber;

    /**
     * Register all subscribers after bean initialization.
     * This ensures all dependencies are properly injected.
     */
    @PostConstruct
    public void registerSubscribers() {
        // Register GraphQL subscriber for GraphQL subscriptions
        eventPublisher.registerSubscriber(graphQLSubscriber);
        
        // Future subscribers can be registered here:
        // eventPublisher.registerSubscriber(auditSubscriber);
        // eventPublisher.registerSubscriber(notificationSubscriber);
        // eventPublisher.registerSubscriber(webhookSubscriber);
    }
}

