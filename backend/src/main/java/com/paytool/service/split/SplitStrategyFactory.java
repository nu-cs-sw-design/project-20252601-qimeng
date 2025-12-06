package com.paytool.service.split;

import com.paytool.model.SplitStrategyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SplitStrategyFactory {
    
    private final Map<SplitStrategyType, SplitStrategy> strategies;
    
    @Autowired
    public SplitStrategyFactory(List<SplitStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                    this::getStrategyType,
                    Function.identity()
                ));
    }
    
    public SplitStrategy getStrategy(SplitStrategyType type) {
        if (type == null) {
            type = SplitStrategyType.EQUAL;
        }
        
        SplitStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported split strategy type: " + type);
        }
        return strategy;
    }
    
    private SplitStrategyType getStrategyType(SplitStrategy strategy) {
        if (strategy instanceof EqualSplitStrategy) {
            return SplitStrategyType.EQUAL;
        } else if (strategy instanceof PercentageSplitStrategy) {
            return SplitStrategyType.PERCENTAGE;
        } else {
            throw new IllegalArgumentException("Unknown strategy type: " + strategy.getClass().getName());
        }
    }
}
