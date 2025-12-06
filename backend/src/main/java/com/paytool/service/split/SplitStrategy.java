package com.paytool.service.split;

import com.paytool.model.User;

import java.util.List;
import java.util.Map;

public interface SplitStrategy {
    Map<Long, Double> calculateSplit(Double totalAmount, List<User> members, Map<String, Object> parameters);
}
