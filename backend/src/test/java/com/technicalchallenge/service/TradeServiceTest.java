package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSearchDTO;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Schedule;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.model.TradeStatus;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CashflowRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.ScheduleRepository;
import com.technicalchallenge.repository.TradeLegRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeLegRepository tradeLegRepository;

    @Mock
    private CashflowRepository cashflowRepository;

    @Mock
    private TradeStatusRepository tradeStatusRepository;
   
    @Mock
    private BookRepository bookRepository;

    @Mock
    private CounterpartyRepository counterpartyRepository;

    @Mock
    private ScheduleRepository scheduleRepository;


    @Mock
    private AdditionalInfoService additionalInfoService;

    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;
    private Book book;
    private Counterparty counterparty;
    private TradeStatus tradeStatus;

    @BeforeEach
    void setUp() {
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 1, 17));
        tradeDTO.setVersion(1);

        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);
        leg1.setCalculationPeriodSchedule("1M");

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);
        leg2.setCalculationPeriodSchedule("1M");

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));

        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
        trade.setVersion(1);

        book = new Book();
        book.setId(1L);
        book.setBookName("Book");

        counterparty = new Counterparty();
        counterparty.setId(1L);
        counterparty.setName("Counterparty");

        tradeStatus = new TradeStatus();
        tradeStatus.setId(1L);
        tradeStatus.setTradeStatus("NEW");

        tradeDTO.setBookName(book.getBookName());
        tradeDTO.setCounterpartyName(counterparty.getName());
        tradeDTO.setTradeStatus(tradeStatus.getTradeStatus());
    }

    private void createTradeMocks(){
        when(bookRepository.findByBookName(any(String.class))).thenReturn(Optional.of(new Book()));
        when(counterpartyRepository.findByName(any(String.class))).thenReturn(Optional.of(new Counterparty()));
        when(tradeStatusRepository.findByTradeStatus(any(String.class))).thenReturn(Optional.of(new TradeStatus()));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCreateTrade_Success() { 
        // Given
        createTradeMocks();

        // When
        Trade result = tradeService.createTrade(tradeDTO);

        // Then
        assertNotNull(result);
        assertEquals(100001L, result.getTradeId());
        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testCreateTrade_InvalidDates_ShouldFail() {
        //Case 1: Invalid start date
        // Given
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 10)); // Invalid - Before trade date

        // When & Then
        RuntimeException startDateException = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        //Assert
        assertEquals("Start date cannot be before trade date", startDateException.getMessage());

        //Case 2: Invalid maturity date
        //Given
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17)); //valid start trade date
        tradeDTO.setTradeMaturityDate(LocalDate.of(2025, 1, 10)); // Invalid - Before start date

        // When & Then
        RuntimeException maturityDateException = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        //Assert
        assertEquals("Maturity date cannot be before start date", maturityDateException.getMessage());

    }

    @Test
    void testCreateTrade_InvalidLegCount_ShouldFail() {
        // Given
        tradeDTO.setTradeLegs(Arrays.asList(new TradeLegDTO())); // Only 1 leg

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        assertTrue(exception.getMessage().contains("exactly 2 legs"));
    }

    @Test
    void testGetTradeById_Found() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));

        // When
        Optional<Trade> result = tradeService.getTradeById(100001L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(100001L, result.get().getTradeId());
    }

    @Test
    void testGetTradeById_NotFound() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When
        Optional<Trade> result = tradeService.getTradeById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testAmendTrade_Success() {
        // Given
        tradeDTO.setTradeStatus("AMENDED");

        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeStatusRepository.findByTradeStatus("AMENDED")).thenReturn(Optional.of(new TradeStatus()));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Trade result = tradeService.amendTrade(100001L, tradeDTO);

        // Then
        assertNotNull(result);
        verify(tradeRepository, times(2)).save(any(Trade.class)); // Save old and new
    }

    @Test
    void testAmendTrade_TradeNotFound() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.amendTrade(999L, tradeDTO);
        });

        assertTrue(exception.getMessage().contains("Trade not found"));
    }

    // This test has a deliberate bug for candidates to find and fix
    @Test
     void testCashflowGeneration_MonthlySchedule() {
      // Given -     
       Schedule schedule = new Schedule();
       schedule.setId(1L);
       schedule.setSchedule("1M");

       createTradeMocks();
       
       when(scheduleRepository.findBySchedule(any(String.class))).thenReturn(Optional.of(schedule));
    
        // When - execute the service method, createTrade()
        Trade result = tradeService.createTrade(tradeDTO);
     
        // Then - Assert the results
        for(TradeLeg leg : result.getTradeLegs()){
            assertEquals(12, leg.getCashflows().size());
        }
    }

    @Test
     void testSearch_Trade(){
        trade.setBook(book);
        trade.setCounterparty(counterparty);
        trade.setTradeStatus(tradeStatus);

        Trade trade2 = new Trade();
        trade2.setId(101L);
        trade2.setTradeId(100002L);
        trade2.setVersion(1);
        trade2.setBook(book);
        trade2.setCounterparty(counterparty);
        trade2.setTradeStatus(tradeStatus);

      List<Trade> trades = Arrays.asList(trade, trade2);

       TradeSearchDTO searchDTO = new TradeSearchDTO();
       searchDTO.setBook(book.getBookName());
       searchDTO.setCounterparty(counterparty.getName());

        when(tradeRepository.findAll(any(Specification.class))).thenReturn(trades);

        List<Trade> result = tradeService.searchTrade(searchDTO);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tradeRepository).findAll(any(Specification.class));
     }

     @Test
      void testSearchTrade_InvalidDates_ShouldFail(){
        TradeSearchDTO searchDTO = new TradeSearchDTO();
        searchDTO.setStartDate(LocalDate.of(2025, 1, 10));
        searchDTO.setEndDate(LocalDate.of(2025, 1, 2));

         RuntimeException startDateSearchException = assertThrows(RuntimeException.class, () -> {
            tradeService.searchTrade(searchDTO);
        });
        //Assert
        assertEquals("Start date cannot be after end date", startDateSearchException.getMessage());
      }

      @Test
       void testSearchTrade_WithoutFilters(){
         TradeSearchDTO searchDTO = new TradeSearchDTO();
         List<Trade> trades = Arrays.asList(trade);

         when(tradeRepository.findAll(any(Specification.class))).thenReturn(trades);

        List<Trade> result = tradeService.searchTrade(searchDTO);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tradeRepository).findAll(any(Specification.class));
       }


       @Test
       void testSearchTrade_WithNullValues(){
         TradeSearchDTO searchDTO = new TradeSearchDTO();
          searchDTO.setBook(null);
          searchDTO.setCounterparty(null);

         List<Trade> trades = Arrays.asList(trade);

         when(tradeRepository.findAll(any(Specification.class))).thenReturn(trades);

        List<Trade> result = tradeService.searchTrade(searchDTO);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tradeRepository).findAll(any(Specification.class));
       }
       
       @Test
        void testsearchTrade_WithNoMatches(){
            TradeSearchDTO searchDTO = new TradeSearchDTO();
          searchDTO.setStatus("dance");

         when(tradeRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        List<Trade> result = tradeService.searchTrade(searchDTO);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(tradeRepository).findAll(any(Specification.class));
        }
}
