package com.paytool.service.event;

import com.paytool.model.Group;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a new group is created.
 */
@Getter
public class GroupCreatedEvent implements GroupEvent {
    private final Long groupId;
    private final Group group;
    private final LocalDateTime timestamp;
    private final String eventType = "GROUP_CREATED";

    public GroupCreatedEvent(Long groupId, Group group) {
        this.groupId = groupId;
        this.group = group;
        this.timestamp = LocalDateTime.now();
    }
}

