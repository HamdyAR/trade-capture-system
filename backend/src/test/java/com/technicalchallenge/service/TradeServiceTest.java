package com.technicalchallenge.service;

import com.technicalchallenge.controller.UserProfileController;
import com.technicalchallenge.dto.CashflowDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSearchDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.BusinessDayConvention;
import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Currency;
import com.technicalchallenge.model.HolidayCalendar;
import com.technicalchallenge.model.Index;
import com.technicalchallenge.model.LegType;
import com.technicalchallenge.model.PayRec;
import com.technicalchallenge.model.Privilege;
import com.technicalchallenge.model.Schedule;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.model.TradeStatus;
import com.technicalchallenge.model.TradeSubType;
import com.technicalchallenge.model.TradeType;
import com.technicalchallenge.model.UserPrivilege;
import com.technicalchallenge.model.UserProfile;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.BusinessDayConventionRepository;
import com.technicalchallenge.repository.CashflowRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.CurrencyRepository;
import com.technicalchallenge.repository.HolidayCalendarRepository;
import com.technicalchallenge.repository.IndexRepository;
import com.technicalchallenge.repository.LegTypeRepository;
import com.technicalchallenge.repository.PayRecRepository;
import com.technicalchallenge.repository.PrivilegeRepository;
import com.technicalchallenge.repository.ScheduleRepository;
import com.technicalchallenge.repository.TradeLegRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeStatusRepository;
import com.technicalchallenge.repository.TradeSubTypeRepository;
import com.technicalchallenge.repository.TradeTypeRepository;
import com.technicalchallenge.repository.UserPrivilegeRepository;
import com.technicalchallenge.rsql.RsqlSpecificationBuilder;

import org.h2.engine.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private ApplicationUserRepository applicationUserRepository;

     @Mock
    private PrivilegeRepository privilegeRepository;

     @Mock
    private UserPrivilegeRepository userPrivilegeRepository;

    @Mock
    private TradeTypeRepository tradeTypeRepository;

    @Mock
    private TradeSubTypeRepository tradeSubTypeRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private HolidayCalendarRepository holidayCalendarRepository;

    @Mock
    private BusinessDayConventionRepository businessDayConventionRepository;

    @Mock
    private LegTypeRepository legTypeRepository;

    @Mock
    private IndexRepository indexRepository;

    @Mock
    private PayRecRepository payRecRepository;

    @Mock
    private AdditionalInfoService additionalInfoService;

    @Mock
    private RsqlSpecificationBuilder<Trade> rsqlSpecificationBuilder;

    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;
    private Book book;
    private TradeLegDTO leg1;
    private TradeLegDTO leg2;
    private Counterparty counterparty;
    private TradeStatus tradeStatus;
    private ApplicationUser user;
    private Privilege privilege;
    private UserPrivilege userPrivilege;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 10, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 10, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 10, 17));
        tradeDTO.setVersion(1);
        tradeDTO.setTradeType("Swap");
        tradeDTO.setTradeSubType("IR Swap");


        CashflowDTO cashflow1 = new CashflowDTO();
        CashflowDTO cashflow2 = new CashflowDTO();

        cashflow1.setValueDate(LocalDate.of(2026, 10, 17));
        cashflow2.setValueDate(LocalDate.of(2026, 10, 17));

        List<CashflowDTO> leg1cashflow = Arrays.asList(cashflow1);
        List<CashflowDTO> leg2cashflow = Arrays.asList(cashflow2);

        leg1 = new TradeLegDTO();
        leg1.setLegId(1001L);
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);
        leg1.setCalculationPeriodSchedule("1M");
        leg1.setLegType("Fixed");
        leg1.setPayReceiveFlag("Pay");
        leg1.setCashflows(leg1cashflow);
        leg1.setCurrency("USD");
        leg1.setPaymentBusinessDayConvention("Following");
        leg1.setHolidayCalendar("NY");

        leg2 = new TradeLegDTO();
        leg2.setLegId(2001L);
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);
        leg2.setCalculationPeriodSchedule("1M");
        leg2.setLegType("Floating");
        leg2.setPayReceiveFlag("Receive");
        leg2.setCashflows(leg2cashflow);
        leg2.setIndexName("LIBOR");
        leg2.setCurrency("USD");
        leg2.setHolidayCalendar("NY");

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));
        tradeDTO.setTraderUserName("Simon King");

        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
        trade.setVersion(1);

        book = new Book();
        book.setId(1L);
        book.setBookName("Book");
        book.setActive(true);

        counterparty = new Counterparty();
        counterparty.setId(1L);
        counterparty.setName("Counterparty");
        counterparty.setActive(true);

        tradeStatus = new TradeStatus();
        tradeStatus.setId(1L);
        tradeStatus.setTradeStatus("NEW");

        tradeDTO.setBookName(book.getBookName());
        tradeDTO.setCounterpartyName(counterparty.getName());
        tradeDTO.setTradeStatus(tradeStatus.getTradeStatus());

        userProfile = new UserProfile();
        userProfile.setUserType("TRADER_SALES");

        user = new ApplicationUser();
        user.setLoginId("simon");
        user.setActive(true);
        user.setId(10001L);
        user.setUserProfile(userProfile);
        user.setFirstName("Simon");

        privilege = new Privilege();
        privilege.setName("BOOK_TRADE");
        privilege.setId(10001L);

        userPrivilege = new UserPrivilege();
        userPrivilege.setUserId(10001L);
        userPrivilege.setPrivilegeId(10003L);
    }

    private void createTradeMocks(){
        when(bookRepository.findByBookName(any(String.class))).thenReturn(Optional.of(book));
        when(counterpartyRepository.findByName(any(String.class))).thenReturn(Optional.of(counterparty));
        when(tradeStatusRepository.findByTradeStatus(any(String.class))).thenReturn(Optional.of(new TradeStatus()));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationUserRepository.findByLoginId(anyString())).thenReturn(Optional.of(user));
         when(privilegeRepository.findByName(anyString())).thenReturn(Optional.of(privilege));
         when(userPrivilegeRepository.existsByUserIdAndPrivilegeId(anyLong(), anyLong())).thenReturn(true);
        when(applicationUserRepository.findByFirstName(any(String.class))).thenReturn(Optional.of(user));
   

        LegType legType = new LegType();
        legType.setType("Fixed");
       
        when(tradeTypeRepository.findByTradeType(any(String.class))).thenReturn(Optional.of(new TradeType()));
        when(tradeSubTypeRepository.findByTradeSubType(any(String.class))).thenReturn(Optional.of(new TradeSubType()));
        when(currencyRepository.findByCurrency(any(String.class))).thenReturn(Optional.of(new Currency()));
        when(legTypeRepository.findByType(any(String.class))).thenReturn(Optional.of(legType));
        when(indexRepository.findByIndex(any(String.class))).thenReturn(Optional.of(new Index()));
        when(holidayCalendarRepository.findByHolidayCalendar(any(String.class))).thenReturn(Optional.of(new HolidayCalendar()));
        when(payRecRepository.findByPayRec(any(String.class))).thenReturn(Optional.of(new PayRec()));
        when(businessDayConventionRepository.findByBdc(any(String.class))).thenReturn(Optional.of(new BusinessDayConvention()));
    }

    @Test
    void testCreateTrade_Success() { 
        // Given
        createTradeMocks();
        
        // When
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());

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
        createTradeMocks();
    
        // When & Then
        RuntimeException startDateException = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO, user.getLoginId());
        });

        //Assert
        assertEquals("Trade validation failed Start date cannot be before trade date", startDateException.getMessage());

        //Case 2: Invalid maturity date
        //Given
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17)); //valid start trade date
        tradeDTO.setTradeMaturityDate(LocalDate.of(2025, 1, 10)); // Invalid - Before start date

        // When & Then
        RuntimeException maturityDateException = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO, user.getLoginId());
        });

        //Assert
        assertEquals("Maturity date cannot be before start date", maturityDateException.getMessage());

    }

    @Test
    void testCreateTrade_InvalidLegCount_ShouldFail() {
        // Given
        tradeDTO.setTradeLegs(Arrays.asList(new TradeLegDTO())); // Only 1 leg

        createTradeMocks();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO, user.getLoginId());
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
        Trade result = tradeService.amendTrade(100001L, tradeDTO, user.getLoginId());

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
            tradeService.amendTrade(999L, tradeDTO, user.getLoginId());
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
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());
     
        // Then - Assert the results
        for(TradeLeg leg : result.getTradeLegs()){
            assertEquals(12, leg.getCashflows().size());
        }
    }


    //advanced search tests
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
          searchDTO.setTradeStatus("dance");

         when(tradeRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        List<Trade> result = tradeService.searchTrade(searchDTO);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(tradeRepository).findAll(any(Specification.class));
        }

    @Test
    void testSearchTrade_WithPagination(){
         //Given   
          TradeSearchDTO searchDTO = new TradeSearchDTO();
          searchDTO.setBook("Book");
          trade.setBook(book);

          Pageable pageable = PageRequest.of(0, 20, Sort.by("tradeDate").descending());

          List<Trade> trades = List.of(trade);
          Page<Trade> expectedPage = new PageImpl<>(trades, pageable, trades.size());

          when(tradeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

          //When
          Page<Trade> result = tradeService.filterTradeWithPagination(searchDTO, pageable);

          //Then
          assertNotNull(result);
          assertEquals(1, result.getTotalElements());
          assertEquals("Book", result.getContent().get(0).getBook().getBookName());

          verify(tradeRepository).findAll(any(Specification.class), eq(pageable));
         }

    @Test
    void testSearchTrade_WithRSQL(){
         //Given   
          String rsqlQuery = "book.name==book";
          Pageable pageable = PageRequest.of(0, 20);

          List<Trade> trades = List.of(trade);
          Page<Trade> expectedPage = new PageImpl<>(trades, pageable, trades.size());

          Specification<Trade> spec = (root, query, cb) -> cb.conjunction();

          when(rsqlSpecificationBuilder.createSpecification(rsqlQuery)).thenReturn(spec);
          when(tradeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);



          //When
          Page<Trade> result = tradeService.searchTradeWithRsql(rsqlQuery, pageable);

          //Then
          assertNotNull(result);
          assertEquals(1, result.getTotalElements());
          assertEquals(1, result.getContent().size());

          verify(rsqlSpecificationBuilder).createSpecification(rsqlQuery);
          verify(tradeRepository).findAll(any(Specification.class), eq(pageable));
         }

    @Test
    void testSearchTrade_WithRSQL_InvalidQuery(){
         //Given   
          String query = "wrong==syntax";
          Pageable pageable = PageRequest.of(0, 20);

          when(rsqlSpecificationBuilder.createSpecification(query)).thenThrow(new IllegalArgumentException("Invalid RSQL query: " + query));

         assertThrows(IllegalArgumentException.class, () -> {
            tradeService.searchTradeWithRsql(query, pageable);
         });

         verify(rsqlSpecificationBuilder).createSpecification(query);
         }


    //cashflow value tests
    @Test
    void testCashflowValue_QuarterlySchedule(){
        //$10M at 3.5% quarterly = $87,500 (not $875,000)

        leg1.setNotional(BigDecimal.valueOf(10000000));
        leg1.setRate(3.5);
        leg1.setCalculationPeriodSchedule("3M");

        leg2.setNotional(BigDecimal.valueOf(10000000));

        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setSchedule("3M");

        createTradeMocks();
       
        when(scheduleRepository.findBySchedule(any(String.class))).thenReturn(Optional.of(schedule));
    
        // When - execute the service method
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());
     
        // Then - Assert the results
        TradeLeg fixedLeg = result.getTradeLegs().get(0);//fixed leg
            assertEquals(4, fixedLeg.getCashflows().size());
            assertEquals(0, fixedLeg.getCashflows().get(0).getPaymentValue().compareTo(BigDecimal.valueOf(87500.00)));
         }
        
    @Test
    void testCashflowValue_MonthlySchedule(){

        leg1.setNotional(BigDecimal.valueOf(2000000));
        leg1.setRate(5.0);
        leg1.setCalculationPeriodSchedule("1M");

        leg2.setNotional(BigDecimal.valueOf(2000000));

        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setSchedule("1M");

        createTradeMocks();
       
        when(scheduleRepository.findBySchedule(any(String.class))).thenReturn(Optional.of(schedule));
    
        // When - execute the service method
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());
     
        // Then - Assert the results
        TradeLeg fixedLeg = result.getTradeLegs().get(0);//fixed leg
            assertEquals(12, fixedLeg.getCashflows().size());
            assertEquals(0, fixedLeg.getCashflows().get(0).getPaymentValue().compareTo(BigDecimal.valueOf(8333.33)));
         }  
   
    @Test
    void testCashflowValue_SemiAnnuallySchedule(){

        leg1.setNotional(BigDecimal.valueOf(500000));
        leg1.setRate(2.5);
        leg1.setCalculationPeriodSchedule("6M");

        leg2.setNotional(BigDecimal.valueOf(500000));

        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setSchedule("6M");

        createTradeMocks();
       
        when(scheduleRepository.findBySchedule(any(String.class))).thenReturn(Optional.of(schedule));
    
        // When - execute the service method
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());
     
        // Then - Assert the results
        TradeLeg fixedLeg = result.getTradeLegs().get(0);//fixed leg
            assertEquals(2, fixedLeg.getCashflows().size());
            assertEquals(0, fixedLeg.getCashflows().get(0).getPaymentValue().compareTo(BigDecimal.valueOf(6250.00)));
         }  

    @Test
    void testCashflowValue_AnnuallySchedule(){

        leg1.setNotional(BigDecimal.valueOf(750000));
        leg1.setRate(4.5);
        leg1.setCalculationPeriodSchedule("Yearly");

        leg2.setNotional(BigDecimal.valueOf(750000));

        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setSchedule("12M");

        createTradeMocks();
       
        when(scheduleRepository.findBySchedule(any(String.class))).thenReturn(Optional.of(schedule));
    
        // When - execute the service method
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());
     
        // Then - Assert the results
        TradeLeg fixedLeg = result.getTradeLegs().get(0);//fixed leg
            assertEquals(1, fixedLeg.getCashflows().size());
            assertEquals(0, fixedLeg.getCashflows().get(0).getPaymentValue().compareTo(BigDecimal.valueOf(33750.00)));
         } 
         
       @Test
        void testCashflowValue_FloatingLeg(){

        //floating leg
        leg2.setNotional(BigDecimal.valueOf(750000));

        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setSchedule("12M");

        createTradeMocks();
       
        when(scheduleRepository.findBySchedule(any(String.class))).thenReturn(Optional.of(schedule));
    
        // When - execute the service method
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());
     
        // Then - Assert the results
        TradeLeg floatingLeg = result.getTradeLegs().get(1);//floating leg
            assertEquals(1, floatingLeg.getCashflows().size());
            assertEquals(0, floatingLeg.getCashflows().get(0).getPaymentValue().compareTo(BigDecimal.ZERO));
         }  

         @Test
        void testCashflowValue_NullLegType(){

        //null leg
        leg1.setLegType(null);

        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setSchedule("12M");

        createTradeMocks();
       
        when(scheduleRepository.findBySchedule(any(String.class))).thenReturn(Optional.of(schedule));
    
        // When - execute the service method
        Trade result = tradeService.createTrade(tradeDTO, user.getLoginId());
     
        // Then - Assert the results
        TradeLeg nullLeg = result.getTradeLegs().get(0);//null leg
            assertEquals(0, nullLeg.getCashflows().get(0).getPaymentValue().compareTo(BigDecimal.ZERO));
         } 
}
