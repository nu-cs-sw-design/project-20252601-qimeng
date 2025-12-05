package com.paytool.service.event;

/**
 * Interface for subscribers that want to react to group-related events.
 * Implementations can handle events for logging, notifications, analytics, etc.
 */
public interface GroupEventSubscriber {
    /**
     * Handle a group created event.
     * @param event The group created event
     */
    void onGroupCreated(GroupCreatedEvent event);

    /**
     * Handle a group status changed event.
     * @param event The group status changed event
     */
    void onGroupStatusChanged(GroupStatusChangedEvent event);

    /**
     * Handle a member joined event.
     * @param event The member joined event
     */
    void onMemberJoined(GroupMemberJoinedEvent event);

    /**
     * Handle a member status changed event.
     * @param event The member status changed event
     */
    void onMemberStatusChanged(GroupMemberStatusChangedEvent event);
}

