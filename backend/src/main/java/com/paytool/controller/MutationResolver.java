package com.paytool.controller;

import com.paytool.dto.CreateGroupInput;
import com.paytool.dto.CreateTransactionInput;
import com.paytool.dto.CreateUserInput;
import com.paytool.dto.UpdateUserInput;
import com.paytool.model.*;
import com.paytool.service.AuthService;
import com.paytool.service.GroupService;
import com.paytool.service.PaymentCardService;
import com.paytool.service.TransactionService;
import com.paytool.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MutationResolver {
    private final UserService userService;
    private final GroupService groupService;
    private final TransactionService transactionService;
    private final PaymentCardService paymentCardService;
    private final AuthService authService;

    @MutationMapping
    public String testMutation() {
        return "Mutation test successful!";
    }

    @MutationMapping
    public User createUser(@Argument("input") CreateUserInput input) {
        return userService.createUser(input);
    }

    @MutationMapping
    public User updateUser(@Argument("id") String id, @Argument("input") UpdateUserInput input) {
        return userService.updateUser(Long.parseLong(id), input);
    }

    @MutationMapping
    public Group createGroup(@Argument("input") CreateGroupInput input) {
        return groupService.createGroup(input);
    }

    @MutationMapping
    public GroupMember joinGroup(@Argument("groupId") Long groupId, @Argument("userId") Long userId) {
        return groupService.joinGroup(groupId, userId);
    }

    @MutationMapping
    public GroupMember updateMemberStatus(
            @Argument("groupId") Long groupId,
            @Argument("userId") Long userId,
            @Argument("status") MemberStatus status) {
        return groupService.updateMemberStatus(groupId, userId, status);
    }

    @MutationMapping
    public Group updateGroupStatus(
            @Argument("groupId") Long groupId,
            @Argument("status") GroupStatus status) {
        return groupService.updateGroupStatus(groupId, status);
    }

    @MutationMapping
    public PaymentCard generatePaymentCard(@Argument("groupId") Long groupId) {
        return paymentCardService.generatePaymentCard(groupId);
    }

    @MutationMapping
    public Transaction createTransaction(@Argument("input") CreateTransactionInput input) {
        return transactionService.createTransaction(input);
    }

    @MutationMapping
    public Transaction updateTransactionStatus(
            @Argument("id") String id,
            @Argument("status") TransactionStatus status) {
        return transactionService.updateTransactionStatus(Long.parseLong(id), status);
    }

    @MutationMapping
    public AuthPayload login(@Argument String username, @Argument String password) {
        return authService.login(username, password);
    }
}

