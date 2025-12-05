package com.paytool.service.event.subscriber;

import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import com.paytool.service.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GraphQL-specific subscriber that converts group events to Reactor Flux streams
 * for GraphQL subscriptions.
 * This maintains backward compatibility with the existing SubscriptionResolver.
 */
@Component
public class GraphQLGroupEventSubscriber implements GroupEventSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(GraphQLGroupEventSubscriber.class);
    
    private final Map<String, Sinks.Many<Group>> groupSinks = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<GroupMember>> memberSinks = new ConcurrentHashMap<>();

    @Override
    public void onGroupCreated(GroupCreatedEvent event) {
        logger.debug("GraphQL subscriber handling GroupCreatedEvent for groupId: {}", event.getGroupId());
        publishGroupUpdate(event.getGroupId().toString(), event.getGroup());
    }

    @Override
    public void onGroupStatusChanged(GroupStatusChangedEvent event) {
        logger.debug("GraphQL subscriber handling GroupStatusChangedEvent for groupId: {}", event.getGroupId());
        publishGroupUpdate(event.getGroupId().toString(), event.getGroup());
    }

    @Override
    public void onMemberJoined(GroupMemberJoinedEvent event) {
        logger.debug("GraphQL subscriber handling GroupMemberJoinedEvent for groupId: {}", event.getGroupId());
        publishMemberUpdate(event.getGroupId().toString(), event.getMember());
    }

    @Override
    public void onMemberStatusChanged(GroupMemberStatusChangedEvent event) {
        logger.debug("GraphQL subscriber handling GroupMemberStatusChangedEvent for groupId: {}", event.getGroupId());
        publishMemberUpdate(event.getGroupId().toString(), event.getMember());
    }

    /**
     * Get a Flux stream for group status changes.
     * Used by SubscriptionResolver for GraphQL subscriptions.
     */
    public Flux<Group> getGroupStatusFlux(String groupId) {
        logger.debug("Creating group flux for groupId: {}", groupId);
        return getOrCreateGroupSink(groupId).asFlux()
            .doOnSubscribe(s -> logger.debug("Group subscription started for: {}", groupId))
            .doOnNext(g -> logger.debug("Group update emitted for: {}, status: {}", groupId, g.getStatus()))
            .doOnError(e -> logger.error("Error in group flux for {}: {}", groupId, e.getMessage(), e))
            .doOnComplete(() -> logger.debug("Group subscription completed for: {}", groupId));
    }

    /**
     * Get a Flux stream for member status changes.
     * Used by SubscriptionResolver for GraphQL subscriptions.
     */
    public Flux<GroupMember> getMemberStatusFlux(String groupId) {
        logger.debug("Creating member flux for groupId: {}", groupId);
        return getOrCreateMemberSink(groupId).asFlux()
            .doOnSubscribe(s -> logger.debug("Member subscription started for: {}", groupId))
            .doOnNext(m -> logger.debug("Member update emitted for: {}, member: {}", groupId, m.getId()))
            .doOnError(e -> logger.error("Error in member flux for {}: {}", groupId, e.getMessage(), e))
            .doOnComplete(() -> logger.debug("Member subscription completed for: {}", groupId));
    }

    private void publishGroupUpdate(String groupId, Group group) {
        logger.debug("Publishing group update to GraphQL stream for groupId: {}, status: {}", 
            groupId, group.getStatus());
        Sinks.Many<Group> sink = getOrCreateGroupSink(groupId);
        Sinks.EmitResult result = sink.tryEmitNext(group);
        if (result.isFailure()) {
            logger.error("Failed to emit group update for {}: {}", groupId, result);
        } else {
            logger.debug("Successfully emitted group update for {}", groupId);
        }
    }

    private void publishMemberUpdate(String groupId, GroupMember member) {
        logger.debug("Publishing member update to GraphQL stream for groupId: {}, member: {}", 
            groupId, member.getId());
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

