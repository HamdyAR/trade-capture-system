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

public class TradeSummaryDTO {
    private int totalTradesByStatus;
    private int totalNotionalAmountsByCurrency;
    private List<TradeDTO> tradesByTradeTypeAndCounterparty;
    private String riskExposureSummaries;
}
