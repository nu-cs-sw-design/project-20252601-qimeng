package com.paytool.service.split;

import com.paytool.model.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EqualSplitStrategy implements SplitStrategy {
    
    @Override
    public Map<Long, Double> calculateSplit(Double totalAmount, List<User> members, Map<String, Object> parameters) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Members list cannot be null or empty");
        }
        
        if (totalAmount == null || totalAmount <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than 0");
        }
        
        int memberCount = members.size();
        double baseAmount = totalAmount / memberCount;
        double remainder = totalAmount - (baseAmount * memberCount);
        
        Map<Long, Double> splitResult = new HashMap<>();
        
        for (User member : members) {
            splitResult.put(member.getId(), baseAmount);
        }
        
        if (Math.abs(remainder) > 0.0001) {
            Long firstMemberId = members.get(0).getId();
            splitResult.put(firstMemberId, splitResult.get(firstMemberId) + remainder);
        }
        
        return splitResult;
    }
}
