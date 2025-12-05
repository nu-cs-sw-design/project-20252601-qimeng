package com.paytool.service.event;

import com.paytool.model.GroupMember;
import com.paytool.model.MemberStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a group member's status changes.
 */
@Getter
public class GroupMemberStatusChangedEvent implements GroupEvent {
    private final Long groupId;
    private final GroupMember member;
    private final MemberStatus oldStatus;
    private final MemberStatus newStatus;
    private final LocalDateTime timestamp;
    private final String eventType = "GROUP_MEMBER_STATUS_CHANGED";

    public GroupMemberStatusChangedEvent(Long groupId, GroupMember member, MemberStatus oldStatus, MemberStatus newStatus) {
        this.groupId = groupId;
        this.member = member;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }
}

