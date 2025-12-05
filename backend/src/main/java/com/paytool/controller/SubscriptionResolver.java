package com.paytool.controller;

import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import com.paytool.service.event.subscriber.GraphQLGroupEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class SubscriptionResolver {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionResolver.class);
    
    private final GraphQLGroupEventSubscriber graphQLSubscriber;

    @SubscriptionMapping("groupStatusChanged")
    public Flux<Group> groupStatusChanged(@Argument("groupId") String groupId) {
        logger.debug("Group subscription established for groupId: {}", groupId);
        return graphQLSubscriber
            .getGroupStatusFlux(groupId)
            .doOnNext(g -> logger.debug("Emitting group update: {}", g))
            .doOnCancel(() -> logger.debug("Group subscription cancelled for {}", groupId))
            .doOnError(e -> logger.error("Error in group subscription for {}: {}", groupId, e.getMessage()));
    }

    @SubscriptionMapping("memberStatusChanged")
    public Flux<GroupMember> memberStatusChanged(@Argument("groupId") String groupId) {
        logger.debug("Member subscription established for groupId: {}", groupId);
        return graphQLSubscriber
            .getMemberStatusFlux(groupId)
            .doOnNext(m -> logger.debug("Emitting member update: {}", m))
            .doOnCancel(() -> logger.debug("Member subscription cancelled for {}", groupId))
            .doOnError(e -> logger.error("Error in member subscription for {}: {}", groupId, e.getMessage()));
    }
}

