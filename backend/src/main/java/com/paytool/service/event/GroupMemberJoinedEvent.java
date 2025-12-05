package com.paytool.service.event;

import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a member joins a group.
 */
@Getter
public class GroupMemberJoinedEvent implements GroupEvent {
    private final Long groupId;
    private final Group group;
    private final GroupMember member;
    private final LocalDateTime timestamp;
    private final String eventType = "GROUP_MEMBER_JOINED";

    public GroupMemberJoinedEvent(Long groupId, Group group, GroupMember member) {
        this.groupId = groupId;
        this.group = group;
        this.member = member;
        this.timestamp = LocalDateTime.now();
    }
}

