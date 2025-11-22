package com.paytool.controller;

import com.paytool.model.Group;
import com.paytool.model.GroupMember;
import com.paytool.model.Transaction;
import com.paytool.model.User;
import com.paytool.service.GroupService;
import com.paytool.service.TransactionService;
import com.paytool.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class QueryResolver {
    private final UserService userService;
    private final GroupService groupService;
    private final TransactionService transactionService;

    @QueryMapping
    public String hello() {
        return "Hello from PayTool GraphQL!";
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return userService.findById(id).orElse(null);
    }

    @QueryMapping
    public List<User> users() {
        return userService.findAll();
    }

    @QueryMapping
    public Group group(@Argument Long id) {
        return groupService.findById(id).orElse(null);
    }

    @QueryMapping
    public List<Group> groups() {
        return groupService.findAll();
    }

    @QueryMapping
    public List<Group> userGroups(@Argument Long userId) {
        return groupService.findByUserId(userId);
    }

    @QueryMapping
    public List<GroupMember> groupMembers(@Argument Long groupId) {
        return groupService.findGroupMembers(groupId);
    }

    @QueryMapping
    public Transaction transaction(@Argument Long id) {
        return transactionService.findById(id).orElse(null);
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.findAll();
    }

    @QueryMapping
    public List<Transaction> userTransactions(@Argument Long userId) {
        return transactionService.findByUser(userId);
    }
}

