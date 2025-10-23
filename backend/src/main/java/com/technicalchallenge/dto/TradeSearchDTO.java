package com.technicalchallenge.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradeSearchDTO {
    private String counterparty;
    private String book;
    private String trader;
    private String tradeStatus;
    private LocalDate startDate;
    private LocalDate endDate;
}
