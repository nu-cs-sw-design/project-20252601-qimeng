package com.paytool.service;

import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupPublisher {
    private static final Logger logger = LoggerFactory.getLogger(GroupPublisher.class);
    
    private final Map<String, Sinks.Many<Group>> groupSinks = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<GroupMember>> memberSinks = new ConcurrentHashMap<>();

    public Flux<Group> getGroupStatusFlux(String groupId) {
        logger.debug("Creating group flux for groupId: {}", groupId);
        return getOrCreateGroupSink(groupId).asFlux()
            .doOnSubscribe(s -> logger.debug("Group subscription started for: {}", groupId))
            .doOnNext(g -> logger.debug("Group update emitted for: {}, status: {}", groupId, g.getStatus()))
            .doOnError(e -> logger.error("Error in group flux for {}: {}", groupId, e.getMessage(), e))
            .doOnComplete(() -> logger.debug("Group subscription completed for: {}", groupId));
    }

    public Flux<GroupMember> getMemberStatusFlux(String groupId) {
        logger.debug("Creating member flux for groupId: {}", groupId);
        return getOrCreateMemberSink(groupId).asFlux()
            .doOnSubscribe(s -> logger.debug("Member subscription started for: {}", groupId))
            .doOnNext(m -> logger.debug("Member update emitted for: {}, member: {}", groupId, m.getId()))
            .doOnError(e -> logger.error("Error in member flux for {}: {}", groupId, e.getMessage(), e))
            .doOnComplete(() -> logger.debug("Member subscription completed for: {}", groupId));
    }

    public void publishGroupStatus(String groupId, Group group) {
        logger.debug("Publishing group status for groupId: {}, status: {}", groupId, group.getStatus());
        Sinks.Many<Group> sink = getOrCreateGroupSink(groupId);
        Sinks.EmitResult result = sink.tryEmitNext(group);
        if (result.isFailure()) {
            logger.error("Failed to emit group update for {}: {}", groupId, result);
        } else {
            logger.debug("Successfully emitted group update for {}", groupId);
        }
    }

    public void publishMemberStatus(String groupId, GroupMember member) {
        logger.debug("Publishing member status for groupId: {}, member: {}", groupId, member.getId());
        Sinks.Many<GroupMember> sink = getOrCreateMemberSink(groupId);
        Sinks.EmitResult result = sink.tryEmitNext(member);
        if (result.isFailure()) {
            logger.error("Failed to emit member update for {}: {}", groupId, result);
        } else {
            logger.debug("Successfully emitted member update for {}", groupId);
        }
    }

    private Sinks.Many<Group> getOrCreateGroupSink(String groupId) {
        return groupSinks.computeIfAbsent(groupId, k -> {
            logger.debug("Creating new group sink for groupId: {}", k);
            return Sinks.many().multicast().onBackpressureBuffer();
        });
    }

    private Sinks.Many<GroupMember> getOrCreateMemberSink(String groupId) {
        return memberSinks.computeIfAbsent(groupId, k -> {
            logger.debug("Creating new member sink for groupId: {}", k);
            return Sinks.many().multicast().onBackpressureBuffer();
        });
    }
} 