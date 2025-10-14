package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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

    @BeforeEach
    void setUp() {
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 1, 17));

        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));


        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
    }

    @Test
    void testCreateTrade_Success() {
        // Given
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);

        // When
        Trade result = tradeService.createTrade(tradeDTO);

        // Then
        assertNotNull(result);
        assertEquals(100001L, result.getTradeId());
        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testCreateTrade_InvalidDates_ShouldFail() {
        // Given - This test is intentionally failing for candidates to fix
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 10)); // Before trade date

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        // This assertion is intentionally wrong - candidates need to fix it
        assertEquals("Wrong error message", exception.getMessage());
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
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeStatusRepository.findByTradeStatus("AMENDED")).thenReturn(Optional.of(new com.technicalchallenge.model.TradeStatus()));
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);

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
       
       //setup TradeLegDTOs
       TradeLegDTO leg1 = tradeDTO.getTradeLegs().get(0);
       TradeLegDTO leg2 = tradeDTO.getTradeLegs().get(1);

       //set schedule to "1M" on TradeLegDTOs
       leg1.setCalculationPeriodSchedule("1M");
       leg2.setCalculationPeriodSchedule("1M");

       //set required reference data names on tradeDTO
       tradeDTO.setBookName("Book");
       tradeDTO.setCounterpartyName("Counterparty");
       tradeDTO.setTradeStatus("NEW");

       //create reference data entities
       Book book = new Book();
       book.setId(1L);
       book.setBookName("Book");

       Counterparty counterparty = new Counterparty();
       counterparty.setId(1L);
       counterparty.setName("Counterparty");

       TradeStatus status = new TradeStatus();
       status.setId(1L);
       status.setTradeStatus("NEW");

       Schedule schedule = new Schedule();
       schedule.setSchedule("1M");

       //updating the trade object with reference data
       trade.setBook(book);
       trade.setCounterparty(counterparty);
       trade.setTradeStatus(status);
       trade.setTradeDate(tradeDTO.getTradeDate());
       trade.setTradeStartDate(tradeDTO.getTradeStartDate());
       trade.setTradeMaturityDate(tradeDTO.getTradeMaturityDate());
 
        //creating TradeLeg entities 1 and 2
        // creating fixed trade leg
        TradeLeg tradeLeg1 = new TradeLeg();
        tradeLeg1 = new TradeLeg();
        tradeLeg1.setLegId(1L);
        tradeLeg1.setTrade(trade);
        tradeLeg1.setNotional(BigDecimal.valueOf(1000000.0));
        tradeLeg1.setRate(0.05);
        tradeLeg1.setCalculationPeriodSchedule(schedule);

        //creating floating trade leg
        TradeLeg tradeLeg2 = new TradeLeg();
        tradeLeg2.setLegId(2L);
        tradeLeg2.setTrade(trade);
        tradeLeg2.setNotional(BigDecimal.valueOf(1000000.0));
        tradeLeg2.setRate(0.00);
        tradeLeg2.setCalculationPeriodSchedule(schedule);
    
        //creating cashflows for each TradeLeg
        List<Cashflow> cashflows1 = new ArrayList<>();
        List<Cashflow> cashflows2 = new ArrayList<>();

        LocalDate startDate = tradeDTO.getTradeStartDate();// 17-1-2025

        for(int i = 0; i < 12; i++){
            //cashflow for leg 1
            Cashflow cf1 = new Cashflow();
            cf1.setId(1L);
            cf1.setTradeLeg(tradeLeg1);
            cf1.setValueDate(startDate.plusMonths(i + 1));//Payment dates - Feb 17, Mar 17, Apr 17 etc
            cf1.setRate(0.05);
            cf1.setPaymentValue(BigDecimal.valueOf(4166.67)); // (1,000,000 * 0.05 * 1)/12 - calculation for fixed leg
            cashflows1.add(cf1);

            //cashflow for leg 2
            Cashflow cf2 = new Cashflow();
            cf2.setId(2L);
            cf2.setTradeLeg(tradeLeg2);
            cf2.setValueDate(startDate.plusMonths(i + 1));
            cf2.setRate(0.05);
            cf2.setPaymentValue(BigDecimal.ZERO);//zero value for floating trade leg
            cashflows2.add(cf2);
        }
       
       //attaching cashflows to legs
       tradeLeg1.setCashflows(cashflows1);
       tradeLeg2.setCashflows(cashflows2);

       //attaching legs to trade
       trade.setTradeLegs(Arrays.asList(tradeLeg1, tradeLeg2));

       //mocking reference data lookups triggered by populateReferenceDataByName() in TradeService
       when(bookRepository.findByBookName("Book")).thenReturn(Optional.of(book));
       when(counterpartyRepository.findByName("Counterparty")).thenReturn(Optional.of(counterparty));
       when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(status));

       //mocking schedule lookup triggered by populateLegReferenceData() in TradeService
       when(scheduleRepository.findBySchedule("1M")).thenReturn(Optional.of(schedule));

       //mocking trade save
       when(tradeRepository.save(any(Trade.class))).thenReturn(trade);
       //mocking trade leg saves
       when(tradeLegRepository.save(any(TradeLeg.class)))
            .thenReturn(tradeLeg1)
            .thenReturn(tradeLeg2);
       //mocking cashflow save to return what was passed     
        when(cashflowRepository.save(any(Cashflow.class))).thenAnswer(
            invocation -> invocation.getArgument(0));

        

        // When - execute the service method, createTrade()
         Trade result = tradeService.createTrade(tradeDTO);

        
        // Then - Assert the results

        // Verify that trade is created with reference data
        assertNotNull(result, "Trade is null");
        assertNotNull(result.getBook(),"Book is not set");
        assertEquals("Book", result.getBook().getBookName(), 
        "Book name does not match");
        assertNotNull(result.getCounterparty(),"Counterparty is not set");
        assertEquals("Counterparty", result.getCounterparty().getName(), 
        "Counterparty name does not match");
        assertNotNull(result.getTradeStatus(),"Trade status is not set");
        assertEquals("NEW", result.getTradeStatus().getTradeStatus(), 
        "Trade status should be NEW");


        //Verify that trade has exactly 2 legs
        assertEquals(2, result.getTradeLegs().size(), 
        "Trade must have exactly 2 legs");

        //Retrieve trade legs
        TradeLeg createdTradeLeg1 = result.getTradeLegs().get(0);
        TradeLeg createdTradeLeg2 = result.getTradeLegs().get(1);

        //Verify that both legs have cashflows
        assertNotNull(createdTradeLeg1.getCashflows(), "Leg 1 should have cashflows");
        assertNotNull(createdTradeLeg2.getCashflows(), "Leg 2 should have cashflows");

        //Verify that each leg has exactly 12 monthly cashflows
        assertEquals(12, createdTradeLeg1.getCashflows().size(), 
        "Leg 1 should have 12 monthly cashflows (1M schedule for one year)");
        assertEquals(12, createdTradeLeg2.getCashflows().size(), 
        "Leg 2 should have 12 monthly cashflows (1M schedule for one year)");

        //Verify that the schedule is correctly set
        assertNotNull(createdTradeLeg1.getCalculationPeriodSchedule(), 
        "Leg 1 should have a schedule");
        assertEquals("1M", createdTradeLeg2.getCalculationPeriodSchedule().getSchedule(), 
        "Leg 1 schedule should be monthly(1M)");

        assertNotNull(createdTradeLeg2.getCalculationPeriodSchedule(), 
        "Leg 2 should have a schedule");
        assertEquals("1M", createdTradeLeg2.getCalculationPeriodSchedule().getSchedule(), 
        "Leg 2 schedule should be monthly(1M)");


        //Verify payment dates are correct for monthly intervals
        LocalDate expectedFirstPaymentDate = LocalDate.of(2025, 2, 17);// Start date + 1 month
        LocalDate expectedLastPaymentDate = LocalDate.of(2026, 1, 17);// Start date + 12 months

        assertEquals(expectedFirstPaymentDate, createdTradeLeg1.getCashflows().get(0).getValueDate(), 
        "First cashflow should be 1 month after start date");
        assertEquals(expectedLastPaymentDate, createdTradeLeg1.getCashflows().get(11).getValueDate(), 
        "Last cashflow should be at maturity date");

        //Verify cashflow values are calculated correctly for fixed leg
        assertEquals(BigDecimal.valueOf(4166.67), createdTradeLeg1.getCashflows().get(0).getPaymentValue(), 
        "Fixed leg cashflow value should be calculated correctly");
        //Verify cashflow values are zero for floating leg since rate is 0.0
        assertEquals(BigDecimal.ZERO, createdTradeLeg2.getCashflows().get(0).getPaymentValue(), 
        "Floating leg cashflow value should be zero");
        
        
        //Verify repository interactions
        verify(bookRepository).findByBookName("Book");
        verify(counterpartyRepository).findByName("Counterparty");
        verify(tradeStatusRepository).findByTradeStatus("NEW");
        verify(scheduleRepository, times(2)).findBySchedule("1M");//Call once per leg
        verify(tradeRepository).save(any(Trade.class));
        verify(tradeLegRepository, times(2)).save(any(TradeLeg.class));// Call once per leg
        verify(cashflowRepository, times(24)).save(any(Cashflow.class));// Call 12 cashflows X 2 legs
    }
}
