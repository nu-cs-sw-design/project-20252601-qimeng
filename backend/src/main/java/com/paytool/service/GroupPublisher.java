package com.paytool.service;

import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import com.paytool.service.event.subscriber.GraphQLGroupEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Adapter class for backward compatibility.
 * Delegates to GraphQLGroupEventSubscriber for GraphQL subscription functionality.
 * 
 * @deprecated This class is kept for backward compatibility.
 * New code should use GroupEventPublisher and GraphQLGroupEventSubscriber directly.
 */
@Component
@Deprecated
@RequiredArgsConstructor
public class GroupPublisher {
    private final GraphQLGroupEventSubscriber graphQLSubscriber;

    /**
     * Get a Flux stream for group status changes.
     * Delegates to GraphQLGroupEventSubscriber.
     */
    public Flux<Group> getGroupStatusFlux(String groupId) {
        return graphQLSubscriber.getGroupStatusFlux(groupId);
    }

    /**
     * Get a Flux stream for member status changes.
     * Delegates to GraphQLGroupEventSubscriber.
     */
    public Flux<GroupMember> getMemberStatusFlux(String groupId) {
        return graphQLSubscriber.getMemberStatusFlux(groupId);
    }

    /**
     * @deprecated This method is kept for backward compatibility.
     * Publishing should be done through GroupEventPublisher instead.
     */
    @Deprecated
    public void publishGroupStatus(String groupId, Group group) {
        // This method is deprecated. Events should be published through GroupEventPublisher.
        // Keeping for backward compatibility but it won't work properly without event publisher.
    }

    /**
     * @deprecated This method is kept for backward compatibility.
     * Publishing should be done through GroupEventPublisher instead.
     */
    @Deprecated
    public void publishMemberStatus(String groupId, GroupMember member) {
        // This method is deprecated. Events should be published through GroupEventPublisher.
        // Keeping for backward compatibility but it won't work properly without event publisher.
    }
} 