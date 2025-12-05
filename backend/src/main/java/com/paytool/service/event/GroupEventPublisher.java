package com.paytool.service.event;

/**
 * Interface for publishing group-related events.
 * Implementations should notify all registered subscribers when events are published.
 */
public interface GroupEventPublisher {
    /**
     * Publish a group created event.
     * @param event The group created event
     */
    void publishGroupCreated(GroupCreatedEvent event);

    /**
     * Publish a group status changed event.
     * @param event The group status changed event
     */
    void publishGroupStatusChanged(GroupStatusChangedEvent event);

    /**
     * Publish a member joined event.
     * @param event The member joined event
     */
    void publishMemberJoined(GroupMemberJoinedEvent event);

    /**
     * Publish a member status changed event.
     * @param event The member status changed event
     */
    void publishMemberStatusChanged(GroupMemberStatusChangedEvent event);

    /**
     * Register a subscriber to receive group events.
     * @param subscriber The subscriber to register
     */
    void registerSubscriber(GroupEventSubscriber subscriber);

    /**
     * Unregister a subscriber from receiving group events.
     * @param subscriber The subscriber to unregister
     */
    void unregisterSubscriber(GroupEventSubscriber subscriber);
}

