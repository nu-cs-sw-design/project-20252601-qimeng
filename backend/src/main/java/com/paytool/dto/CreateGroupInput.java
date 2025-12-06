package com.paytool.dto;

import com.paytool.model.SplitStrategyType;
import lombok.Data;

@Data
public class CreateGroupInput {
    private Long leaderId;
    private Double totalAmount;
    private String name;
    private String description;
    private Integer totalPeople;
    private SplitStrategyType splitStrategyType;
    private Object splitStrategyParameters;

    public Integer getTotalPeople() {
        return totalPeople;
    }
    
    public void setTotalPeople(Integer totalPeople) {
        this.totalPeople = totalPeople;
    }
} 