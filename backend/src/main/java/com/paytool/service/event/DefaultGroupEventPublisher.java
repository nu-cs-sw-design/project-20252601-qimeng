package com.paytool.service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of GroupEventPublisher.
 * Maintains a list of subscribers and notifies them when events are published.
 */
@Component
public class DefaultGroupEventPublisher implements GroupEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(DefaultGroupEventPublisher.class);
    
    private final List<GroupEventSubscriber> subscribers = new CopyOnWriteArrayList<>();

    @Override
    public void publishGroupCreated(GroupCreatedEvent event) {
        logger.debug("Publishing GroupCreatedEvent for groupId: {}", event.getGroupId());
        notifySubscribers(subscriber -> subscriber.onGroupCreated(event));
    }

    @Override
    public void publishGroupStatusChanged(GroupStatusChangedEvent event) {
        logger.debug("Publishing GroupStatusChangedEvent for groupId: {}, status: {} -> {}", 
            event.getGroupId(), event.getOldStatus(), event.getNewStatus());
        notifySubscribers(subscriber -> subscriber.onGroupStatusChanged(event));
    }

    @Override
    public void publishMemberJoined(GroupMemberJoinedEvent event) {
        logger.debug("Publishing GroupMemberJoinedEvent for groupId: {}, memberId: {}", 
            event.getGroupId(), event.getMember().getId());
        notifySubscribers(subscriber -> subscriber.onMemberJoined(event));
    }

    @Override
    public void publishMemberStatusChanged(GroupMemberStatusChangedEvent event) {
        logger.debug("Publishing GroupMemberStatusChangedEvent for groupId: {}, memberId: {}, status: {} -> {}", 
            event.getGroupId(), event.getMember().getId(), event.getOldStatus(), event.getNewStatus());
        notifySubscribers(subscriber -> subscriber.onMemberStatusChanged(event));
    }

    @Override
    public void registerSubscriber(GroupEventSubscriber subscriber) {
        if (subscriber != null && !subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
            logger.info("Registered new GroupEventSubscriber: {}", subscriber.getClass().getSimpleName());
        }
    }

    @Override
    public void unregisterSubscriber(GroupEventSubscriber subscriber) {
        if (subscribers.remove(subscriber)) {
            logger.info("Unregistered GroupEventSubscriber: {}", subscriber.getClass().getSimpleName());
        }
    }

    /**
     * Notify all subscribers about an event.
     * If a subscriber throws an exception, it is logged but does not stop other subscribers from being notified.
     */
    private void notifySubscribers(SubscriberNotifier notifier) {
        for (GroupEventSubscriber subscriber : subscribers) {
            try {
                notifier.notify(subscriber);
            } catch (Exception e) {
                logger.error("Error notifying subscriber {}: {}", 
                    subscriber.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @FunctionalInterface
    private interface SubscriberNotifier {
        void notify(GroupEventSubscriber subscriber);
    }
}

