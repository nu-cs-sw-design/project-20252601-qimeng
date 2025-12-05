package com.paytool.service.event;

import java.time.LocalDateTime;

/**
 * Base interface for all group-related events.
 * All group events should implement this interface.
 */
public interface GroupEvent {
    /**
     * @return The ID of the group this event relates to
     */
    Long getGroupId();
    
    /**
     * @return The timestamp when this event occurred
     */
    LocalDateTime getTimestamp();
    
    /**
     * @return The type of event
     */
    String getEventType();
}

