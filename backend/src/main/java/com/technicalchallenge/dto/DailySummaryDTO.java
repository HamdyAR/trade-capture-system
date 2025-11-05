package com.technicalchallenge.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

import lombok.AllArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor


public class DailySummaryDTO {
    private int totalTrades;
    private int totalNotional;
    private List<TradeDTO> bookLevelSummaries;
    private String riskExposureSummaries;
}
