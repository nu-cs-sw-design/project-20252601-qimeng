package com.paytool.service.split;

import com.paytool.exception.CustomException;
import com.paytool.model.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PercentageSplitStrategy implements SplitStrategy {
    
    private static final String PERCENTAGES_KEY = "percentages";
    private static final double PERCENTAGE_TOLERANCE = 0.01;
    
    @Override
    public Map<Long, Double> calculateSplit(Double totalAmount, List<User> members, Map<String, Object> parameters) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Members list cannot be null or empty");
        }
        
        if (totalAmount == null || totalAmount <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than 0");
        }
        
        if (parameters == null || !parameters.containsKey(PERCENTAGES_KEY)) {
            throw new CustomException("Percentage split strategy requires 'percentages' parameter");
        }
        
        Object percentagesObj = parameters.get(PERCENTAGES_KEY);
        List<Double> percentages;
        
        if (percentagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Double> percentagesList = (List<Double>) percentagesObj;
            percentages = percentagesList;
        } else if (percentagesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Integer, Double> percentagesMap = (Map<Integer, Double>) percentagesObj;
            int maxIndex = percentagesMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
            percentages = new java.util.ArrayList<>();
            for (int i = 0; i <= maxIndex; i++) {
                percentages.add(percentagesMap.getOrDefault(i, 0.0));
            }
        } else {
            throw new CustomException("Percentages must be a List<Double> or Map<Integer, Double>");
        }
        
        if (percentages == null || percentages.isEmpty()) {
            throw new CustomException("Percentages cannot be null or empty");
        }
        
        if (members.size() > percentages.size()) {
            throw new CustomException(
                String.format("Not enough percentages. Expected at least %d, but got %d", 
                    members.size(), percentages.size())
            );
        }
        
        if (members.size() == 1 && percentages.size() > 1) {
            double leaderPercentage = percentages.get(0);
            if (Math.abs(leaderPercentage - 100.0) > PERCENTAGE_TOLERANCE) {
                throw new CustomException(
                    String.format("When creating a group with only the leader, the leader's percentage should be 100%%, but got %.2f%%", 
                        leaderPercentage)
                );
            }
        } else {
            double totalPercentage = 0.0;
            for (int i = 0; i < members.size(); i++) {
                totalPercentage += percentages.get(i);
            }
            
            if (Math.abs(totalPercentage - 100.0) > PERCENTAGE_TOLERANCE) {
                throw new CustomException(
                    String.format("Percentages for current members must sum to 100%%, but got %.2f%%", totalPercentage)
                );
            }
        }
        
        Map<Long, Double> splitResult = new HashMap<>();
        double totalCalculated = 0.0;
        
        for (int i = 0; i < members.size() - 1; i++) {
            User member = members.get(i);
            Double percentage = percentages.get(i);
            double amount = totalAmount * percentage / 100.0;
            splitResult.put(member.getId(), amount);
            totalCalculated += amount;
        }
        
        User lastMember = members.get(members.size() - 1);
        double lastAmount = totalAmount - totalCalculated;
        splitResult.put(lastMember.getId(), lastAmount);
        
        return splitResult;
    }
}
