package com.idp.backend.util;


import com.idp.backend.dto.SummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class AlertRuleEngine {

    public boolean cpuHigh(SummaryResponse s){
        return s.getAvgCpu() != null && s.getAvgCpu() >80;
    }

    public boolean memoryHigh(SummaryResponse s){
        return s.getAvgMemory() != null && s.getAvgMemory()>1024;
    }

    public boolean serviceDown(SummaryResponse s){
        return "DOWN".equals(s.getStatus());
    }
}
