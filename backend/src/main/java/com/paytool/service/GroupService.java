package com.paytool.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytool.dto.CreateGroupInput;
import com.paytool.exception.CustomException;
import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import com.paytool.model.GroupStatus;
import com.paytool.model.MemberStatus;
import com.paytool.model.SplitStrategyType;
import com.paytool.model.User;
import com.paytool.repository.GroupMemberRepository;
import com.paytool.repository.GroupRepository;
import com.paytool.repository.UserRepository;
import com.paytool.service.event.*;
import com.paytool.service.split.SplitStrategy;
import com.paytool.service.split.SplitStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupEventPublisher eventPublisher;
    private final SplitStrategyFactory splitStrategyFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        SplitStrategyType strategyType = group.getSplitStrategyType() != null 
            ? group.getSplitStrategyType() 
            : SplitStrategyType.EQUAL;
        SplitStrategy strategy = splitStrategyFactory.getStrategy(strategyType);
        
        List<GroupMember> existingMembers = groupMemberRepository.findByGroup(group);
        List<User> allMembers = new ArrayList<>();
        for (GroupMember member : existingMembers) {
            allMembers.add(member.getUser());
        }
        allMembers.add(user);
        
        Map<String, Object> parameters = parseStrategyParameters(group.getSplitStrategyParameters());
        Map<Long, Double> splitAmounts = strategy.calculateSplit(
            group.getTotalAmount(), 
            allMembers, 
            parameters
        );
        
        // Get the amount for the new member
        Double splitAmount = splitAmounts.get(user.getId());
        if (splitAmount == null) {
            throw new CustomException("Failed to calculate split amount for new member");
        }

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

        // Publish member joined event
        GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(groupId, updatedGroup, savedMember);
        eventPublisher.publishMemberJoined(event);

        return savedMember;
    }

    @Transactional
    public GroupMember updateMemberStatus(Long groupId, Long userId, MemberStatus status) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new CustomException("Member not found"));

        MemberStatus oldStatus = member.getStatus();
        member.setStatus(status);
        member = groupMemberRepository.save(member);

        // Publish member status changed event
        GroupMemberStatusChangedEvent event = new GroupMemberStatusChangedEvent(
            groupId, member, oldStatus, status);
        eventPublisher.publishMemberStatusChanged(event);
        
        return member;
    }

    @Transactional
    public Group updateGroupStatus(Long groupId, GroupStatus status) {
        logger.debug("Updating group {} status to {}", groupId, status);
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException("Group not found"));

        // Validate status transition if needed
        GroupStatus oldStatus = group.getStatus();
        validateStatusTransition(oldStatus, status);

        group.setStatus(status);
        Group updatedGroup = groupRepository.save(group);

        // Publish group status changed event
        GroupStatusChangedEvent event = new GroupStatusChangedEvent(
            groupId, updatedGroup, oldStatus, status);
        eventPublisher.publishGroupStatusChanged(event);
        
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

        SplitStrategyType strategyType = input.getSplitStrategyType() != null 
            ? input.getSplitStrategyType() 
            : SplitStrategyType.EQUAL;
        SplitStrategy strategy = splitStrategyFactory.getStrategy(strategyType);
        
        List<User> members = new ArrayList<>();
        members.add(leader);
        
        Map<String, Object> parameters = Map.of();
        if (input.getSplitStrategyParameters() != null) {
            if (input.getSplitStrategyParameters() instanceof String) {
                String paramsJson = (String) input.getSplitStrategyParameters();
                if (!paramsJson.trim().isEmpty()) {
                    parameters = parseStrategyParameters(paramsJson);
                }
            } else if (input.getSplitStrategyParameters() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramsMap = (Map<String, Object>) input.getSplitStrategyParameters();
                parameters = paramsMap;
            }
        }
        Map<Long, Double> splitAmounts = strategy.calculateSplit(
            input.getTotalAmount(), 
            members, 
            parameters
        );
        Double splitAmount = splitAmounts.get(leader.getId());
        if (splitAmount == null) {
            throw new CustomException("Failed to calculate split amount for leader");
        }

        // Create group
        Group group = new Group();
        group.setLeader(leader);
        group.setTotalAmount(input.getTotalAmount());
        group.setDescription(input.getDescription());
        group.setStatus(GroupStatus.PENDING);
        group.setQrCode(UUID.randomUUID().toString());
        group.setTotalPeople(input.getTotalPeople());
        group.setSplitStrategyType(strategyType);
        
        if (!parameters.isEmpty()) {
            try {
                group.setSplitStrategyParameters(objectMapper.writeValueAsString(parameters));
            } catch (Exception e) {
                logger.warn("Failed to serialize strategy parameters: {}", e.getMessage());
            }
        }

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

        // Publish group created event
        GroupCreatedEvent event = new GroupCreatedEvent(savedGroup.getId(), savedGroup);
        eventPublisher.publishGroupCreated(event);

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
    
    private Map<String, Object> parseStrategyParameters(String parametersJson) {
        if (parametersJson == null || parametersJson.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            return objectMapper.readValue(
                parametersJson, 
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            logger.warn("Failed to parse strategy parameters: {}", e.getMessage());
            return Map.of();
        }
    }
} 