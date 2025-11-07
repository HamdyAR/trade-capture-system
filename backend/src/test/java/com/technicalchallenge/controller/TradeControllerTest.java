package com.technicalchallenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeSearchDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.TradeService;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TradeController.class)
public class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeMapper tradeMapper;

    private ObjectMapper objectMapper;
    private TradeDTO tradeDTO;
    private Trade trade;
    private TradeSearchDTO searchDTO;
    private ApplicationUser user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create a sample TradeDTO for testing
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(1001L);
        tradeDTO.setVersion(1);
        tradeDTO.setTradeDate(LocalDate.now()); // Fixed: LocalDate instead of LocalDateTime
        tradeDTO.setTradeStartDate(LocalDate.now().plusDays(2)); // Fixed: correct method name
        tradeDTO.setTradeMaturityDate(LocalDate.now().plusYears(5)); // Fixed: correct method name
        tradeDTO.setTradeStatus("LIVE");
        tradeDTO.setBookName("TestBook");
        tradeDTO.setCounterpartyName("TestCounterparty");
        tradeDTO.setTraderUserName("TestTrader");
        tradeDTO.setInputterUserName("TestInputter");
        tradeDTO.setUtiCode("UTI123456789");

        // Create a sample Trade entity for testing
        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(1001L);
        trade.setVersion(1);
        trade.setTradeDate(LocalDate.now()); // Fixed: LocalDate instead of LocalDateTime
        trade.setTradeStartDate(LocalDate.now().plusDays(2)); // Fixed: correct method name
        trade.setTradeMaturityDate(LocalDate.now().plusYears(5)); // Fixed: correct method name

        // Set up default mappings
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        when(tradeMapper.toEntity(any(TradeDTO.class))).thenReturn(trade);
        when(tradeService.addAdditionalInfo(any(TradeDTO.class))).thenReturn(tradeDTO);
        
        searchDTO = new TradeSearchDTO();
        searchDTO.setBook(tradeDTO.getBookName());
        searchDTO.setCounterparty(tradeDTO.getCounterpartyName());
        searchDTO.setTradeStatus(tradeDTO.getTradeStatus());
 
        user = new ApplicationUser();
        user.setLoginId("simon");
        user.setActive(true);
    }

    @Test
    void testGetAllTrades() throws Exception {
        // Given
        List<Trade> trades = List.of(trade); // Fixed: use List.of instead of Arrays.asList for single item

        when(tradeService.getAllTrades(anyString())).thenReturn(trades);

        // When/Then
        mockMvc.perform(get("/api/trades")
                        .header("X-User-Id", "simon")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tradeId", is(1001)))
                .andExpect(jsonPath("$[0].bookName", is("TestBook")))
                .andExpect(jsonPath("$[0].counterpartyName", is("TestCounterparty")));

        verify(tradeService).getAllTrades(anyString());
    }

    @Test
    void testGetTradeById() throws Exception {
        // Given
        when(tradeService.getTradeById(1001L)).thenReturn(Optional.of(trade));
        

        // When/Then
        mockMvc.perform(get("/api/trades/1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId", is(1001)))
                .andExpect(jsonPath("$.bookName", is("TestBook")))
                .andExpect(jsonPath("$.counterpartyName", is("TestCounterparty")));

        verify(tradeService).getTradeById(1001L);
    }

    @Test
    void testGetTradeByIdNotFound() throws Exception {
        // Given
        when(tradeService.getTradeById(9999L)).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/trades/9999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(tradeService).getTradeById(9999L);
    }

    @Test
    void testCreateTrade() throws Exception {
        // Given
        when(tradeService.saveTrade(any(Trade.class), any(TradeDTO.class), anyString())).thenReturn(trade);
        doNothing().when(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .header("X-User-Id", "simon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradeId", is(1001)));

        verify(tradeService).saveTrade(any(Trade.class), any(TradeDTO.class), anyString());
        verify(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));
    }

    @Test
    void testCreateTradeValidationFailure_MissingTradeDate() throws Exception {
        // Given
        TradeDTO invalidDTO = new TradeDTO();
        invalidDTO.setBookName("TestBook");
        invalidDTO.setCounterpartyName("TestCounterparty");
        // Trade date is purposely missing

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Trade date is required"));

        verify(tradeService, never()).saveTrade(any(Trade.class), any(TradeDTO.class), anyString());
    }

    @Test
    void testCreateTradeValidationFailure_MissingBook() throws Exception {
        // Given
        TradeDTO invalidDTO = new TradeDTO();
        invalidDTO.setTradeDate(LocalDate.now());
        invalidDTO.setCounterpartyName("TestCounterparty");
        // Book name is purposely missing

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Book and Counterparty are required"));

        verify(tradeService, never()).saveTrade(any(Trade.class), any(TradeDTO.class), anyString());
    }

    @Test
    void testUpdateTrade() throws Exception {
        // Given
        Long tradeId = 1001L;
        tradeDTO.setTradeId(tradeId);
        when(tradeService.saveTrade(any(Trade.class), any(TradeDTO.class), anyString())).thenReturn(trade);
        doNothing().when(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));

        // When/Then
        mockMvc.perform(put("/api/trades/{id}", tradeId)
                        .header("X-User-Id", "simon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId", is(1001)));

        verify(tradeService).saveTrade(any(Trade.class), any(TradeDTO.class) , anyString());
    }

    @Test
    void testUpdateTradeIdMismatch() throws Exception {
        // Given
        Long pathId = 1001L;
        tradeDTO.setTradeId(2002L); // Different from path ID

        // When/Then
        mockMvc.perform(put("/api/trades/{id}", pathId)
                        .header("X-User-Id", "simon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Trade ID in path must match Trade ID in request body"));

        verify(tradeService, never()).saveTrade(any(Trade.class), any(TradeDTO.class), anyString());
    }

    @Test
    void testDeleteTrade() throws Exception {
        // Given
        doNothing().when(tradeService).deleteTrade(1001L, user.getLoginId());

        // When/Then
        mockMvc.perform(delete("/api/trades/1001")
                       .header("X-User-Id", "simon")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(tradeService).deleteTrade(1001L, user.getLoginId());
    }

    @Test
    void testCreateTradeWithValidationErrors() throws Exception {
        // Given
        TradeDTO invalidDTO = new TradeDTO();
        invalidDTO.setTradeDate(LocalDate.now()); // Fixed: LocalDate instead of LocalDateTime
        // Missing required fields to trigger validation errors

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(tradeService, never()).createTrade(any(TradeDTO.class), anyString());
    }

     @Test
    void testSearchTrade() throws Exception {
        // Given
        List<Trade> trades = List.of(trade);
        when(tradeService.searchTrade(any(TradeSearchDTO.class))).thenReturn((trades));

        // When/Then
        mockMvc.perform(get("/api/trades/search?book=TestBook")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tradeId", is(1001)))
                .andExpect(jsonPath("$[0].bookName", is("TestBook")))
                .andExpect(jsonPath("$[0].counterpartyName", is("TestCounterparty")));

        verify(tradeService).searchTrade(any(TradeSearchDTO.class));
    }

    @Test
    void testFilterTradeWithPagination() throws Exception {
        // Given
        List<Trade> trades = List.of(trade);
        Page<Trade> tradePage = new PageImpl<>(trades, PageRequest.of(0,20), trades.size());


        when(tradeService.filterTradeWithPagination(any(TradeSearchDTO.class), any(Pageable.class))).thenReturn((tradePage));

        // When/Then
        mockMvc.perform(get("/api/trades/filter")
                        .param("book", "book")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "tradeDate")
                        .param("sortDir", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tradeId", is(1001)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.number", is(0)));
                

        verify(tradeService).filterTradeWithPagination(any(TradeSearchDTO.class), any(Pageable.class));
    }



    @Test
    void testSearchTrade_Rsql() throws Exception {
        // Given
        List<Trade> trades = List.of(trade);
        Page<Trade> tradePage = new PageImpl<>(trades, PageRequest.of(0,20), trades.size());

        when(tradeService.searchTradeWithRsql(any(String.class), any(Pageable.class))).thenReturn((tradePage));

        // When/Then
        mockMvc.perform(get("/api/trades/rsql")
                        .param("query", "book.name==book")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tradeId", is(1001)))
                .andExpect(jsonPath("$.totalElements", is(1)));
                

        verify(tradeService).searchTradeWithRsql((any(String.class)), any(Pageable.class));
    }

    @Test
    void testSearchTrade_RsqlWithEmptyQuery() throws Exception {
        // Given

        when(tradeService.searchTradeWithRsql(any(String.class), any(Pageable.class))).thenThrow(new IllegalArgumentException("RSQL query cannot be empty"));

        // When/Then
        mockMvc.perform(get("/api/trades/rsql")
                        .param("query", "")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
