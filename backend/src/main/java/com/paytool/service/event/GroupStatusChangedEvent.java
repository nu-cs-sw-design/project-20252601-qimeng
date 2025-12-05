package com.paytool.service.event;

import com.paytool.model.Group;
import com.paytool.model.GroupStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a group's status changes.
 */
@Getter
public class GroupStatusChangedEvent implements GroupEvent {
    private final Long groupId;
    private final Group group;
    private final GroupStatus oldStatus;
    private final GroupStatus newStatus;
    private final LocalDateTime timestamp;
    private final String eventType = "GROUP_STATUS_CHANGED";

    public GroupStatusChangedEvent(Long groupId, Group group, GroupStatus oldStatus, GroupStatus newStatus) {
        this.groupId = groupId;
        this.group = group;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }
}

