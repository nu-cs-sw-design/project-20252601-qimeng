package com.paytool.service;

import com.paytool.dto.CreateGroupInput;
import com.paytool.exception.CustomException;
import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import com.paytool.model.GroupStatus;
import com.paytool.model.MemberStatus;
import com.paytool.model.User;
import com.paytool.repository.GroupMemberRepository;
import com.paytool.repository.GroupRepository;
import com.paytool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPublisher groupPublisher;

    @Transactional
    public GroupMember joinGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

        // Check if user is already a member
        Optional<GroupMember> existingMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (existingMember.isPresent()) {
            throw new CustomException("User is already a member of this group");
        }

        // Calculate split amount
        double splitAmount = group.getTotalAmount() / group.getTotalPeople();

        // Create new member
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setAmount(splitAmount);
        member.setStatus(MemberStatus.PENDING);

        // Save member
        GroupMember savedMember = groupMemberRepository.save(member);

        // Fetch updated group with all members
        Group updatedGroup = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException("Group not found"));

        // Publish group update
        groupPublisher.publishGroupStatus(groupId.toString(), updatedGroup);

        return savedMember;
    }

    @Transactional
    public GroupMember updateMemberStatus(Long groupId, Long userId, MemberStatus status) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new CustomException("Member not found"));

        member.setStatus(status);
        member = groupMemberRepository.save(member);

        groupPublisher.publishMemberStatus(groupId.toString(), member);
        return member;
    }

    @Transactional
    public Group updateGroupStatus(Long groupId, GroupStatus status) {
        logger.debug("Updating group {} status to {}", groupId, status);
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException("Group not found"));

        // Validate status transition if needed
        validateStatusTransition(group.getStatus(), status);

        group.setStatus(status);
        Group updatedGroup = groupRepository.save(group);

        groupPublisher.publishGroupStatus(groupId.toString(), updatedGroup);
        logger.info("Group {} status updated to {}", groupId, status);
        return updatedGroup;
    }

    @Transactional
    public Group createGroup(CreateGroupInput input) {
        logger.debug("Creating group with leader ID: {}, total amount: {}, total people: {}", 
            input.getLeaderId(), input.getTotalAmount(), input.getTotalPeople());
        
        if (input.getLeaderId() == null) {
            throw new CustomException("Leader ID cannot be null");
        }
        
        if (input.getTotalPeople() == null || input.getTotalPeople() <= 0) {
            throw new CustomException("Total people must be greater than 0");
        }
        
        if (input.getTotalAmount() == null || input.getTotalAmount() <= 0) {
            throw new CustomException("Total amount must be greater than 0");
        }

        User leader = userRepository.findById(input.getLeaderId())
            .orElseThrow(() -> new CustomException("Leader not found with ID: " + input.getLeaderId()));

        // Calculate split amount
        double splitAmount = input.getTotalAmount() / input.getTotalPeople();

        // Create group
        Group group = new Group();
        group.setLeader(leader);
        group.setTotalAmount(input.getTotalAmount());
        group.setDescription(input.getDescription());
        group.setStatus(GroupStatus.PENDING);
        group.setQrCode(UUID.randomUUID().toString());
        group.setTotalPeople(input.getTotalPeople());

        Group savedGroup = groupRepository.save(group);
        logger.info("Group created successfully with ID: {}", savedGroup.getId());

        // Create leader as first member with AGREED status
        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroup(savedGroup);
        leaderMember.setUser(leader);
        leaderMember.setAmount(splitAmount);
        leaderMember.setStatus(MemberStatus.AGREED);

        GroupMember savedMember = groupMemberRepository.save(leaderMember);
        logger.info("Leader member created with ID: {} for group: {}", 
            savedMember.getId(), savedGroup.getId());

        // Publish group creation event
        groupPublisher.publishGroupStatus(savedGroup.getId().toString(), savedGroup);

        return savedGroup;
    }

    @Transactional(readOnly = true)
    public Optional<Group> findById(Long id) {
        return groupRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Group findByIdOrThrow(Long id) {
        return groupRepository.findById(id)
            .orElseThrow(() -> new CustomException("Group not found"));
    }

    @Transactional(readOnly = true)
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Group> findByUserId(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("User not found"));
        
        List<GroupMember> memberships = groupMemberRepository.findByUser(user);
        return memberships.stream()
            .map(GroupMember::getGroup)
            .distinct()
            .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupMember> findGroupMembers(Long groupId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new CustomException("Group not found"));
        return groupMemberRepository.findByGroup(group);
    }

    private void validateStatusTransition(GroupStatus currentStatus, GroupStatus newStatus) {
        // Define valid status transitions
        switch (currentStatus) {
            case PENDING:
                // Can transition to ACTIVE, COMPLETED, or CANCELLED
                break;
            case ACTIVE:
                // Can transition to COMPLETED or CANCELLED
                if (newStatus == GroupStatus.PENDING) {
                    throw new CustomException("Cannot revert group status from ACTIVE to PENDING");
                }
                break;
            case COMPLETED:
                throw new CustomException("Cannot change status of a completed group");
            case CANCELLED:
                throw new CustomException("Cannot change status of a cancelled group");
        }
    }
} 