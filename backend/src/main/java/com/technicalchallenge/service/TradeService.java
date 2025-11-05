package com.technicalchallenge.service;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.CashflowDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSearchDTO;
import com.technicalchallenge.exception.UnauthorizedAccessException;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.*;
import com.technicalchallenge.repository.*;
import com.technicalchallenge.rsql.RsqlSpecificationBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TradeService {
    private static final Logger logger = LoggerFactory.getLogger(TradeService.class);

    

    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private TradeLegRepository tradeLegRepository;
    @Autowired
    private CashflowRepository cashflowRepository;
    @Autowired
    private TradeStatusRepository tradeStatusRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CounterpartyRepository counterpartyRepository;
    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private TradeTypeRepository tradeTypeRepository;
    @Autowired
    private TradeSubTypeRepository tradeSubTypeRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private LegTypeRepository legTypeRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private HolidayCalendarRepository holidayCalendarRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private BusinessDayConventionRepository businessDayConventionRepository;
    @Autowired
    private PayRecRepository payRecRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private UserPrivilegeRepository userPrivilegeRepository;

    @Autowired
    private AdditionalInfoService additionalInfoService;

    @Autowired
    private AdditionalInfoRepository additionalInfoRepository;

    @Autowired
    private RsqlSpecificationBuilder<Trade> rsqlSpecificationBuilder;

    @Autowired
    private TradeMapper tradeMapper;



    public List<Trade> getAllTrades(String userId) {
        logger.info("Retrieving all trades");

        if(!validateUserPrivileges(userId, "getAllTrades", null)){
              throw new UnauthorizedAccessException("User does not have permission to view trades");
        }

        Optional<ApplicationUser> userOpt = applicationUserRepository.findByLoginId(userId);
        if(userOpt.isEmpty()){
            throw new RuntimeException("User not found " + userId);
        }

        ApplicationUser user = userOpt.get();

        if("TRADER_SALES".equalsIgnoreCase(user.getUserProfile().getUserType())){

            return tradeRepository.findByTraderUserId(user.getId());
        }

        return tradeRepository.findAll();
    }

    public Optional<Trade> getTradeById(Long tradeId) {
        logger.debug("Retrieving trade by id: {}", tradeId);
        return tradeRepository.findByTradeIdAndActiveTrue(tradeId);
    }

    @Transactional
    public Trade createTrade(TradeDTO tradeDTO, String userId) {
        logger.info("Creating new trade with ID: {}", tradeDTO.getTradeId());


        if(!validateUserPrivileges(userId, "createTrade", tradeDTO)){
           throw new UnauthorizedAccessException("User does not have permission to create trade");
        }

        ValidationResult validationResult = validateTradeAndLegs(tradeDTO);

        if(!validationResult.isValid()){
            String errors = String.join(" ,", validationResult.getErrors());
            throw new IllegalArgumentException("Trade validation failed " + errors);
        }

        // Generate trade ID if not provided
        if (tradeDTO.getTradeId() == null) {
            // Generate sequential trade ID starting from 10000
            Long generatedTradeId = generateNextTradeId();
            tradeDTO.setTradeId(generatedTradeId);
            logger.info("Generated trade ID: {}", generatedTradeId);
        }

        // Validate business rules
        validateTradeCreation(tradeDTO);

        // Create trade entity
        Trade trade = mapDTOToEntity(tradeDTO);
        trade.setVersion(1);
        trade.setActive(true);
        trade.setCreatedDate(LocalDateTime.now());
        trade.setLastTouchTimestamp(LocalDateTime.now());

        // Set default trade status to NEW if not provided
        if (tradeDTO.getTradeStatus() == null) {
            tradeDTO.setTradeStatus("NEW");
        }

        // Populate reference data
        populateReferenceDataByName(trade, tradeDTO);

        // Ensure we have essential reference data
        validateReferenceData(trade);

        Trade savedTrade = tradeRepository.save(trade);

        // Create trade legs and cashflows
        createTradeLegsWithCashflows(tradeDTO, savedTrade);

        if(tradeDTO.getAdditionalFields() != null || !tradeDTO.getAdditionalFields().isEmpty()){
            for(AdditionalInfoDTO additionalInfo : tradeDTO.getAdditionalFields()){
                if("SETTLEMENT_INSTRUCTIONS".equals(additionalInfo.getFieldName())){
                    additionalInfo.setEntityId(savedTrade.getId());
                    additionalInfo.setEntityType("TRADE");
                    additionalInfo.setFieldType("STRING");
                    additionalInfoService.addAdditionalInfo(additionalInfo);
                    
                    logger.info("Added settlement instructions for new trade with ID", savedTrade.getId());
                }
            }
        }

        logger.info("Successfully created trade with ID: {}", savedTrade.getTradeId());
        return savedTrade;
    }

    // NEW METHOD: For controller compatibility
    @Transactional
    public Trade saveTrade(Trade trade, TradeDTO tradeDTO, String userId) {
        logger.info("Saving trade with ID: {}", trade.getTradeId());

        // If this is an existing trade (has ID), handle as amendment
        if (trade.getId() != null) {
            return amendTrade(trade.getTradeId(), tradeDTO, userId);
        } else {
            return createTrade(tradeDTO, userId);
        }
    }

    // FIXED: Populate reference data by names from DTO
    public void populateReferenceDataByName(Trade trade, TradeDTO tradeDTO) {
        logger.debug("Populating reference data for trade");

        // Populate Book
        if (tradeDTO.getBookName() != null) {
            bookRepository.findByBookName(tradeDTO.getBookName())
                    .ifPresent(trade::setBook);
        } else if (tradeDTO.getBookId() != null) {
            bookRepository.findById(tradeDTO.getBookId())
                    .ifPresent(trade::setBook);
        }

        // Populate Counterparty
        if (tradeDTO.getCounterpartyName() != null) {
            counterpartyRepository.findByName(tradeDTO.getCounterpartyName())
                    .ifPresent(trade::setCounterparty);
        } else if (tradeDTO.getCounterpartyId() != null) {
            counterpartyRepository.findById(tradeDTO.getCounterpartyId())
                    .ifPresent(trade::setCounterparty);
        }

        // Populate TradeStatus
        if (tradeDTO.getTradeStatus() != null) {
            tradeStatusRepository.findByTradeStatus(tradeDTO.getTradeStatus())
                    .ifPresent(trade::setTradeStatus);
        } else if (tradeDTO.getTradeStatusId() != null) {
            tradeStatusRepository.findById(tradeDTO.getTradeStatusId())
                    .ifPresent(trade::setTradeStatus);
        }

        // Populate other reference data
        populateUserReferences(trade, tradeDTO);
        populateTradeTypeReferences(trade, tradeDTO);
    }

    private void populateUserReferences(Trade trade, TradeDTO tradeDTO) {
        // Handle trader user by name or ID with enhanced logging
        if (tradeDTO.getTraderUserName() != null) {
            logger.debug("Looking up trader user by name: {}", tradeDTO.getTraderUserName());
            String[] nameParts = tradeDTO.getTraderUserName().trim().split("\\s+");
            if (nameParts.length >= 1) {
                String firstName = nameParts[0];
                logger.debug("Searching for user with firstName: {}", firstName);
                Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
                if (userOpt.isPresent()) {
                    trade.setTraderUser(userOpt.get());
                    logger.debug("Found trader user: {} {}", userOpt.get().getFirstName(), userOpt.get().getLastName());
                } else {
                    logger.warn("Trader user not found with firstName: {}", firstName);
                    // Try with loginId as fallback
                    Optional<ApplicationUser> byLoginId = applicationUserRepository.findByLoginId(tradeDTO.getTraderUserName().toLowerCase());
                    if (byLoginId.isPresent()) {
                        trade.setTraderUser(byLoginId.get());
                        logger.debug("Found trader user by loginId: {}", tradeDTO.getTraderUserName());
                    } else {
                        logger.warn("Trader user not found by loginId either: {}", tradeDTO.getTraderUserName());
                    }
                }
            }
        } else if (tradeDTO.getTraderUserId() != null) {
            applicationUserRepository.findById(tradeDTO.getTraderUserId())
                    .ifPresent(trade::setTraderUser);
        }

        // Handle inputter user by name or ID with enhanced logging
        if (tradeDTO.getInputterUserName() != null) {
            logger.debug("Looking up inputter user by name: {}", tradeDTO.getInputterUserName());
            String[] nameParts = tradeDTO.getInputterUserName().trim().split("\\s+");
            if (nameParts.length >= 1) {
                String firstName = nameParts[0];
                logger.debug("Searching for inputter with firstName: {}", firstName);
                Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
                if (userOpt.isPresent()) {
                    trade.setTradeInputterUser(userOpt.get());
                    logger.debug("Found inputter user: {} {}", userOpt.get().getFirstName(), userOpt.get().getLastName());
                } else {
                    logger.warn("Inputter user not found with firstName: {}", firstName);
                    // Try with loginId as fallback
                    Optional<ApplicationUser> byLoginId = applicationUserRepository.findByLoginId(tradeDTO.getInputterUserName().toLowerCase());
                    if (byLoginId.isPresent()) {
                        trade.setTradeInputterUser(byLoginId.get());
                        logger.debug("Found inputter user by loginId: {}", tradeDTO.getInputterUserName());
                    } else {
                        logger.warn("Inputter user not found by loginId either: {}", tradeDTO.getInputterUserName());
                    }
                }
            }
        } else if (tradeDTO.getTradeInputterUserId() != null) {
            applicationUserRepository.findById(tradeDTO.getTradeInputterUserId())
                    .ifPresent(trade::setTradeInputterUser);
        }
    }

    private void populateTradeTypeReferences(Trade trade, TradeDTO tradeDTO) {
        if (tradeDTO.getTradeType() != null) {
            logger.debug("Looking up trade type: {}", tradeDTO.getTradeType());
            Optional<TradeType> tradeTypeOpt = tradeTypeRepository.findByTradeType(tradeDTO.getTradeType());
            if (tradeTypeOpt.isPresent()) {
                trade.setTradeType(tradeTypeOpt.get());
                logger.debug("Found trade type: {} with ID: {}", tradeTypeOpt.get().getTradeType(), tradeTypeOpt.get().getId());
            } else {
                logger.warn("Trade type not found: {}", tradeDTO.getTradeType());
            }
        } else if (tradeDTO.getTradeTypeId() != null) {
            tradeTypeRepository.findById(tradeDTO.getTradeTypeId())
                    .ifPresent(trade::setTradeType);
        }

        if (tradeDTO.getTradeSubType() != null) {
            Optional<TradeSubType> tradeSubTypeOpt = tradeSubTypeRepository.findByTradeSubType(tradeDTO.getTradeSubType());
            if (tradeSubTypeOpt.isPresent()) {
                trade.setTradeSubType(tradeSubTypeOpt.get());
            } else {
                List<TradeSubType> allSubTypes = tradeSubTypeRepository.findAll();
                for (TradeSubType subType : allSubTypes) {
                    if (subType.getTradeSubType().equalsIgnoreCase(tradeDTO.getTradeSubType())) {
                        trade.setTradeSubType(subType);
                        break;
                    }
                }
            }
        } else if (tradeDTO.getTradeSubTypeId() != null) {
            tradeSubTypeRepository.findById(tradeDTO.getTradeSubTypeId())
                    .ifPresent(trade::setTradeSubType);
        }
    }

    // NEW METHOD: Delete trade (mark as cancelled)
    @Transactional
    public void deleteTrade(Long tradeId, String userId) {
        logger.info("Deleting (cancelling) trade with ID: {}", tradeId);
        cancelTrade(tradeId, userId);
    }

    @Transactional
    public Trade amendTrade(Long tradeId, TradeDTO tradeDTO, String userId) {
        logger.info("Amending trade with ID: {}", tradeId);

        Optional<Trade> existingTradeOpt = getTradeById(tradeId);
        if (existingTradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade existingTrade = existingTradeOpt.get();
        TradeDTO existingTradeDTO = tradeMapper.toDto(existingTrade);

        if(!validateUserPrivileges(userId, "amendTrade", existingTradeDTO)){
           throw new UnauthorizedAccessException("User does not have permission to amend this trade");
        }
           
        ValidationResult tradeValidationResult = validateTradeAndLegs(tradeDTO);
        if(!tradeValidationResult.isValid()){
            String errors = String.join(" ,", tradeValidationResult.getErrors());
            throw new IllegalArgumentException("Trade validation failed " + errors);
        }

        // Deactivate existing trade
        existingTrade.setActive(false);
        existingTrade.setDeactivatedDate(LocalDateTime.now());
        tradeRepository.save(existingTrade);

        // Create new version
        Trade amendedTrade = mapDTOToEntity(tradeDTO);
        amendedTrade.setTradeId(tradeId);
        amendedTrade.setVersion(existingTrade.getVersion() + 1);
        amendedTrade.setActive(true);
        amendedTrade.setCreatedDate(LocalDateTime.now());
        amendedTrade.setLastTouchTimestamp(LocalDateTime.now());

        // Populate reference data
        populateReferenceDataByName(amendedTrade, tradeDTO);

        // Set status to AMENDED
        TradeStatus amendedStatus = tradeStatusRepository.findByTradeStatus("AMENDED")
                .orElseThrow(() -> new RuntimeException("AMENDED status not found"));
        amendedTrade.setTradeStatus(amendedStatus);

        Trade savedTrade = tradeRepository.save(amendedTrade);

        // Create new trade legs and cashflows
        createTradeLegsWithCashflows(tradeDTO, savedTrade);

        logger.info("Successfully amended trade with ID: {}", savedTrade.getTradeId());
        return savedTrade;
    }

    @Transactional
    public Trade terminateTrade(Long tradeId, String userId) {
        logger.info("Terminating trade with ID: {}", tradeId);

        Optional<Trade> tradeOpt = getTradeById(tradeId);
        if (tradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade trade = tradeOpt.get();
        TradeDTO tradeDTO = tradeMapper.toDto(trade);

        if(!validateUserPrivileges(userId, "terminateTrade", tradeDTO)){
              throw new UnauthorizedAccessException("User does not have permission to terminate this trade");
        }

        TradeStatus terminatedStatus = tradeStatusRepository.findByTradeStatus("TERMINATED")
                .orElseThrow(() -> new RuntimeException("TERMINATED status not found"));

        trade.setTradeStatus(terminatedStatus);
        trade.setLastTouchTimestamp(LocalDateTime.now());

        return tradeRepository.save(trade);
    }

    @Transactional
    public Trade cancelTrade(Long tradeId, String userId) {
        logger.info("Cancelling trade with ID: {}", tradeId);

        Optional<Trade> tradeOpt = getTradeById(tradeId);
        if (tradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade trade = tradeOpt.get();

        TradeDTO tradeDTO = tradeMapper.toDto(trade);

        if(!validateUserPrivileges(userId, "terminateTrade", tradeDTO)){
              throw new UnauthorizedAccessException("User does not have permission to cancel this trade");
        }

        TradeStatus cancelledStatus = tradeStatusRepository.findByTradeStatus("CANCELLED")
                .orElseThrow(() -> new RuntimeException("CANCELLED status not found"));

        trade.setTradeStatus(cancelledStatus);
        trade.setLastTouchTimestamp(LocalDateTime.now());

        return tradeRepository.save(trade);
    }

    private void validateTradeCreation(TradeDTO tradeDTO) {
        // Validate dates - Fixed to use consistent field names
        if (tradeDTO.getTradeStartDate() != null && tradeDTO.getTradeDate() != null) {
            if (tradeDTO.getTradeStartDate().isBefore(tradeDTO.getTradeDate())) {
                throw new RuntimeException("Start date cannot be before trade date");
            }
        }
        if (tradeDTO.getTradeMaturityDate() != null && tradeDTO.getTradeStartDate() != null) {
            if (tradeDTO.getTradeMaturityDate().isBefore(tradeDTO.getTradeStartDate())) {
                throw new RuntimeException("Maturity date cannot be before start date");
            }
        }

        // Validate trade has exactly 2 legs
        if (tradeDTO.getTradeLegs() == null || tradeDTO.getTradeLegs().size() != 2) {
            throw new RuntimeException("Trade must have exactly 2 legs");
        }
    }

    private Trade mapDTOToEntity(TradeDTO dto) {
        Trade trade = new Trade();
        trade.setTradeId(dto.getTradeId());
        trade.setTradeDate(dto.getTradeDate()); // Fixed field names
        trade.setTradeStartDate(dto.getTradeStartDate());
        trade.setTradeMaturityDate(dto.getTradeMaturityDate());
        trade.setTradeExecutionDate(dto.getTradeExecutionDate());
        trade.setUtiCode(dto.getUtiCode());
        trade.setValidityStartDate(dto.getValidityStartDate());
        trade.setLastTouchTimestamp(LocalDateTime.now());
        return trade;
    }

    private void createTradeLegsWithCashflows(TradeDTO tradeDTO, Trade savedTrade) {
        
        //added code to make cashflow generation test pass
        List<TradeLeg> legs = new ArrayList<>();

        for (int i = 0; i < tradeDTO.getTradeLegs().size(); i++) {
            var legDTO = tradeDTO.getTradeLegs().get(i);

            TradeLeg tradeLeg = new TradeLeg();
            tradeLeg.setTrade(savedTrade);
            tradeLeg.setNotional(legDTO.getNotional());
            tradeLeg.setRate(legDTO.getRate());
            tradeLeg.setActive(true);
            tradeLeg.setCreatedDate(LocalDateTime.now());

            // Populate reference data for leg
            populateLegReferenceData(tradeLeg, legDTO);

            TradeLeg savedLeg = tradeLegRepository.save(tradeLeg);
            
            //added code to make cashflow generation test pass
            legs.add(savedLeg);

            // Generate cashflows for this leg
            if (tradeDTO.getTradeStartDate() != null && tradeDTO.getTradeMaturityDate() != null) {
                generateCashflows(savedLeg, tradeDTO.getTradeStartDate(), tradeDTO.getTradeMaturityDate());
            }
        }
        //added code to make cashflow generation test pass
        savedTrade.setTradeLegs(legs);
    }

    private void populateLegReferenceData(TradeLeg leg, TradeLegDTO legDTO) {
        // Populate currency by name or ID
        if (legDTO.getCurrency() != null) {
            currencyRepository.findByCurrency(legDTO.getCurrency())
                    .ifPresent(leg::setCurrency);
        } else if (legDTO.getCurrencyId() != null) {
            currencyRepository.findById(legDTO.getCurrencyId())
                    .ifPresent(leg::setCurrency);
        }

        // Populate leg type by name or ID
        if (legDTO.getLegType() != null) {
            legTypeRepository.findByType(legDTO.getLegType())
                    .ifPresent(leg::setLegRateType);
        } else if (legDTO.getLegTypeId() != null) {
            legTypeRepository.findById(legDTO.getLegTypeId())
                    .ifPresent(leg::setLegRateType);
        }

        // Populate index by name or ID
        if (legDTO.getIndexName() != null) {
            indexRepository.findByIndex(legDTO.getIndexName())
                    .ifPresent(leg::setIndex);
        } else if (legDTO.getIndexId() != null) {
            indexRepository.findById(legDTO.getIndexId())
                    .ifPresent(leg::setIndex);
        }

        // Populate holiday calendar by name or ID
        if (legDTO.getHolidayCalendar() != null) {
            holidayCalendarRepository.findByHolidayCalendar(legDTO.getHolidayCalendar())
                    .ifPresent(leg::setHolidayCalendar);
        } else if (legDTO.getHolidayCalendarId() != null) {
            holidayCalendarRepository.findById(legDTO.getHolidayCalendarId())
                    .ifPresent(leg::setHolidayCalendar);
        }

        // Populate schedule by name or ID
        if (legDTO.getCalculationPeriodSchedule() != null) {
            scheduleRepository.findBySchedule(legDTO.getCalculationPeriodSchedule())
                    .ifPresent(leg::setCalculationPeriodSchedule);
        } else if (legDTO.getScheduleId() != null) {
            scheduleRepository.findById(legDTO.getScheduleId())
                    .ifPresent(leg::setCalculationPeriodSchedule);
        }

        // Populate payment business day convention by name or ID
        if (legDTO.getPaymentBusinessDayConvention() != null) {
            businessDayConventionRepository.findByBdc(legDTO.getPaymentBusinessDayConvention())
                    .ifPresent(leg::setPaymentBusinessDayConvention);
        } else if (legDTO.getPaymentBdcId() != null) {
            businessDayConventionRepository.findById(legDTO.getPaymentBdcId())
                    .ifPresent(leg::setPaymentBusinessDayConvention);
        }

        // Populate fixing business day convention by name or ID
        if (legDTO.getFixingBusinessDayConvention() != null) {
            businessDayConventionRepository.findByBdc(legDTO.getFixingBusinessDayConvention())
                    .ifPresent(leg::setFixingBusinessDayConvention);
        } else if (legDTO.getFixingBdcId() != null) {
            businessDayConventionRepository.findById(legDTO.getFixingBdcId())
                    .ifPresent(leg::setFixingBusinessDayConvention);
        }

        // Populate pay/receive flag by name or ID
        if (legDTO.getPayReceiveFlag() != null) {
            payRecRepository.findByPayRec(legDTO.getPayReceiveFlag())
                    .ifPresent(leg::setPayReceiveFlag);
        } else if (legDTO.getPayRecId() != null) {
            payRecRepository.findById(legDTO.getPayRecId())
                    .ifPresent(leg::setPayReceiveFlag);
        }
    }

    /**
     * FIXED: Generate cashflows based on schedule and maturity date
     */
    private void generateCashflows(TradeLeg leg, LocalDate startDate, LocalDate maturityDate) {
        logger.info("Generating cashflows for leg {} from {} to {}", leg.getLegId(), startDate, maturityDate);

        //added code to make cashflow generation test pass
        ArrayList<Cashflow> cashFlows = new ArrayList<Cashflow>();
        leg.setCashflows(cashFlows);

        // Use default schedule if not set
        String schedule = "3M"; // Default to quarterly
        if (leg.getCalculationPeriodSchedule() != null) {
            schedule = leg.getCalculationPeriodSchedule().getSchedule();
        }

        int monthsInterval = parseSchedule(schedule);
        List<LocalDate> paymentDates = calculatePaymentDates(startDate, maturityDate, monthsInterval);

        for (LocalDate paymentDate : paymentDates) {
            Cashflow cashflow = new Cashflow();
            cashflow.setTradeLeg(leg); // Fixed field name
            cashflow.setValueDate(paymentDate);
            cashflow.setRate(leg.getRate());

            // Calculate value based on leg type
            BigDecimal cashflowValue = calculateCashflowValue(leg, monthsInterval);
            cashflow.setPaymentValue(cashflowValue);

            cashflow.setPayRec(leg.getPayReceiveFlag());
            cashflow.setPaymentBusinessDayConvention(leg.getPaymentBusinessDayConvention());
            cashflow.setCreatedDate(LocalDateTime.now());
            cashflow.setActive(true);

            cashflowRepository.save(cashflow);
            
            //added code to make cashflow generation test pass
            leg.getCashflows().add(cashflow);
        }

        logger.info("Generated {} cashflows for leg {}", paymentDates.size(), leg.getLegId());
    }

    private int parseSchedule(String schedule) {
        if (schedule == null || schedule.trim().isEmpty()) {
            return 3; // Default to quarterly
        }

        schedule = schedule.trim();

        // Handle common schedule names
        switch (schedule.toLowerCase()) {
            case "monthly":
                return 1;
            case "quarterly":
                return 3;
            case "semi-annually":
            case "semiannually":
            case "half-yearly":
                return 6;
            case "annually":
            case "yearly":
                return 12;
            default:
                // Parse "1M", "3M", "12M" format
                if (schedule.endsWith("M") || schedule.endsWith("m")) {
                    try {
                        return Integer.parseInt(schedule.substring(0, schedule.length() - 1));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid schedule format: " + schedule);
                    }
                }
                throw new RuntimeException("Invalid schedule format: " + schedule + ". Supported formats: Monthly, Quarterly, Semi-annually, Annually, or 1M, 3M, 6M, 12M");
        }
    }

    private List<LocalDate> calculatePaymentDates(LocalDate startDate, LocalDate maturityDate, int monthsInterval) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate.plusMonths(monthsInterval);

        while (!currentDate.isAfter(maturityDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusMonths(monthsInterval);
        }

        return dates;
    }

    private BigDecimal calculateCashflowValue(TradeLeg leg, int monthsInterval) {
        if (leg.getLegRateType() == null) {
            return BigDecimal.ZERO;
        }

        String legType = leg.getLegRateType().getType();

        if ("Fixed".equals(legType)) {
            BigDecimal notional = leg.getNotional();
            BigDecimal rate = BigDecimal.valueOf(leg.getRate()).divide(BigDecimal.valueOf(100));
            BigDecimal months = BigDecimal.valueOf(monthsInterval);
            BigDecimal twelve = BigDecimal.valueOf(12);

            BigDecimal result = notional.multiply(rate).multiply(months).divide(twelve,2, RoundingMode.HALF_UP);

            return result;
        } else if ("Floating".equals(legType)) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    private void validateReferenceData(Trade trade) {
        // Validate essential reference data is populated
        if (trade.getBook() == null) {
            throw new RuntimeException("Book not found or not set");
        }
        if (trade.getCounterparty() == null) {
            throw new RuntimeException("Counterparty not found or not set");
        }
        if (trade.getTradeStatus() == null) {
            throw new RuntimeException("Trade status not found or not set");
        }

        logger.debug("Reference data validation passed for trade");
    }

    // NEW METHOD: Generate the next trade ID (sequential)
    private Long generateNextTradeId() {
        // For simplicity, using a static variable. In real scenario, this should be atomic and thread-safe.
        return 10000L + tradeRepository.count();
    }

    //New method of searching trades -/search
    public List<Trade> searchTrade(TradeSearchDTO searchDTO){
        logger.info("Searching all trades satisfying criteria, {}", searchDTO);

        validateDateRange(searchDTO);

        Specification<Trade> spec = buildTradeSearchSpecification(searchDTO);
        List<Trade> result = tradeRepository.findAll(spec);

        logger.info("Found {} trades matching search criteria, {}", result.size());

        return result;
    }

    private Specification<Trade> buildTradeSearchSpecification(TradeSearchDTO searchDTO){
        Specification<Trade> spec = Specification.where((root, query, cb) -> cb.conjunction());
        
        if (searchDTO.getCounterparty() != null && !searchDTO.getCounterparty().isEmpty()){
            spec = spec.and((root, query, cb) -> 
                cb.equal(cb.lower(root.get("counterparty").get("name")), searchDTO.getCounterparty().toLowerCase())
            );
        }

        if (searchDTO.getBook() != null && !searchDTO.getBook().isEmpty()){
            spec = spec.and((root, query, cb) -> 
                cb.equal(cb.lower(root.get("book").get("bookName")), searchDTO.getBook().toLowerCase())
            );
        }

        if (searchDTO.getTrader() != null && !searchDTO.getTrader().isEmpty()){
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.equal(cb.lower(root.get("traderUser").get("firstName")), searchDTO.getTrader().toLowerCase()),
                    cb.equal(cb.lower(root.get("traderUser").get("lastName")), searchDTO.getTrader().toLowerCase())   
                ));
        }

        if (searchDTO.getTradeStatus() != null && !searchDTO.getTradeStatus().isEmpty()){
            spec = spec.and((root, query, cb) -> 
                cb.equal(cb.lower(root.get("tradeStatus").get("tradeStatus")), searchDTO.getTradeStatus().toLowerCase())
            );
        }

        if (searchDTO.getStartDate() != null){
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo((root.get("tradeDate")), searchDTO.getStartDate())
            );
        }    

        if (searchDTO.getEndDate() != null){
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo((root.get("tradeDate")), searchDTO.getEndDate())
            );    
        }
        return spec;
    }

    private void validateDateRange(TradeSearchDTO searchDTO){
        if(searchDTO.getStartDate() != null && searchDTO.getEndDate() != null){
            if(searchDTO.getStartDate().isAfter(searchDTO.getEndDate())){
                throw new IllegalArgumentException("Start date cannot be after end date");
            }
        }
    }

    //New method of searching trades -/filter with pagination
    public Page<Trade> filterTradeWithPagination(TradeSearchDTO searchDTO, Pageable pageable){
          logger.info("Searching trades with pagination - criteria: {}, page: {}, size:{}", 
          searchDTO, pageable.getPageNumber(), pageable.getPageSize());

          validateDateRange(searchDTO);

          Specification<Trade> spec = buildTradeSearchSpecification(searchDTO);
          Page<Trade> result = tradeRepository.findAll(spec, pageable);
          
          logger.info("Found {} trades matching search criteria (page {} of {})", 
          result.getNumberOfElements(), 
          result.getNumber() + 1, result.getTotalPages());
        
          return result;

    }

    //New method of searching trades - /rsql 
    public Page<Trade> searchTradeWithRsql(String rsqlQuery, Pageable pageable){
         logger.info("Searching trades with RSQL query: {}, page: {}, size:{}", 
          rsqlQuery, pageable.getPageNumber(), pageable.getPageSize());

        try{
            Specification<Trade> spec = rsqlSpecificationBuilder.createSpecification(rsqlQuery);
            Page<Trade> result = tradeRepository.findAll(spec, pageable);

            logger.info("Found {} trades matching RSQL query (page {} of {})", 
            result.getNumberOfElements(), 
            result.getNumber() + 1, result.getTotalPages());

            return result;
        }catch(IllegalArgumentException e){
            throw e;
        }
    }

//new business rules validation methods
    public ValidationResult validateTradeBusinessRules(TradeDTO tradeDTO){
        LocalDate startDate = tradeDTO.getTradeStartDate();
        LocalDate maturityDate = tradeDTO.getTradeMaturityDate();
        LocalDate tradeDate = tradeDTO.getTradeDate();

        ValidationResult validationResult = new ValidationResult();
        List<String> errorMessages = new ArrayList<>();

       //1.Start date cannot be before trade date
        if(startDate != null && tradeDate != null){
            if(startDate.isBefore(tradeDate)){
                errorMessages.add("Start date cannot be before trade date");
            }
        }
        //2.Maturity date cannot be before start date or trade date
        if (maturityDate != null && startDate != null) {
            if (maturityDate.isBefore(startDate)) {
                errorMessages.add("Maturity date cannot be before start date");
                
            }
        }
        //3.Maturity date cannot be before trade date
        if(maturityDate.isBefore(tradeDate)){
          errorMessages.add("Maturity date cannot be before trade date");
        }
        //4.Trade date cannot be more than 30 days in the past
        if(tradeDate != null){
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            if(tradeDate.isBefore(thirtyDaysAgo)){
                errorMessages.add("Trade date cannot be more than 30 days in the past"); 
            }
        }
        
        //setting validation result
        if(errorMessages.isEmpty()){
            validationResult.setValid(true);
        }
        else{
            validationResult.setErrors(errorMessages);
            validationResult.setValid(false);
        }
        return validationResult;
    }

    public ValidationResult validateTradeLegConsistency(List<TradeLegDTO> legs){
        ValidationResult validationResult = new ValidationResult();
        List<String> errorMessages = new ArrayList<>();
  
        //validating that there are exactly 2 legs
        if(legs == null || legs.size() != 2){
            errorMessages.add("Trade must have exactly 2 legs");
            validationResult.setValid(false);
            validationResult.setErrors(errorMessages);
            return validationResult;
        }

        TradeLegDTO leg1 = legs.get(0);
        TradeLegDTO leg2 = legs.get(1);

        List<CashflowDTO> cashflowsLeg1 = leg1.getCashflows();
        List<CashflowDTO> cashflowsLeg2 = leg2.getCashflows();
        
        //validating that cashflows exist
        if(cashflowsLeg1 == null || cashflowsLeg1.isEmpty() || cashflowsLeg2 == null || cashflowsLeg2.isEmpty()){
             errorMessages.add("Both legs must have cashflows to validate maturity dates");
            validationResult.setValid(false);
            validationResult.setErrors(errorMessages);
            return validationResult;
        }

        //getting maturity dates from last cashflow for each leg
        LocalDate maturityDate1 = cashflowsLeg1.get(cashflowsLeg1.size() - 1).getValueDate();
        LocalDate maturityDate2 = cashflowsLeg2.get(cashflowsLeg2.size() - 1).getValueDate();

   
        //1.Both legs must have identical maturity dates
        if(maturityDate1 != null && maturityDate2 != null){
             if(!maturityDate1.equals(maturityDate2)){
            errorMessages.add("Both legs must have identical maturity dates");
        }
        }
        

        //2.Legs must have opposite pay/receive flags
        if(leg1.getPayReceiveFlag() != null && leg2.getPayReceiveFlag() != null){
             if(leg1.getPayReceiveFlag().equals(leg2.getPayReceiveFlag())){
                errorMessages.add("Legs must have opposite pay/receive flags");
            }
        }
       
        
        //Floating legs must have an index specified
        if(leg1.getLegType() != null &&  "floating".equalsIgnoreCase(leg1.getLegType())){
          if(leg1.getIndexName() == null || leg1.getIndexName().trim().isEmpty()){
                errorMessages.add("Floating leg 1 must have an index specified");
            }
        }

        if(leg2.getLegType() != null &&  "floating".equalsIgnoreCase(leg2.getLegType())){
          if(leg2.getIndexName() == null || leg2.getIndexName().trim().isEmpty()){
                errorMessages.add("Floating leg 2 must have an index specified");
            }
        }

        //Fixed legs must have a valid rate
        if(leg1.getLegType() != null &&  "fixed".equalsIgnoreCase(leg1.getLegType())){
          if(leg1.getRate() == null || leg1.getRate() <= 0){
                errorMessages.add("Fixed leg 1 must have a valid rate");
            }
        }

        if(leg2.getLegType() != null &&  "fixed".equalsIgnoreCase(leg2.getLegType())){
          if(leg2.getRate() == null || leg2.getRate() <= 0){
                errorMessages.add("Fixed leg 2 must have a valid rate");
            }
        }
       
        if(errorMessages.isEmpty()){
            validationResult.setValid(true);
        }
        else{
            validationResult.setErrors(errorMessages);
            validationResult.setValid(false);
        }
        return validationResult;        
    }

    public boolean validateUserPrivileges(String userId, String operation, TradeDTO tradeDTO){

        Optional<ApplicationUser> userOpt = applicationUserRepository.findByLoginId(userId);

        logger.info("User optional: {}", userOpt);

         if(userOpt.isEmpty()){
           return false;
        }
        
        ApplicationUser user = userOpt.get();

        //check if user is active
        if(!user.isActive()){
            return false;
        }

        String privilegeName = mapOperationToPrivilege(operation);
        
        if(privilegeName == null){
            return false;
        }

        Optional<Privilege> privilegeOpt = privilegeRepository.findByName(privilegeName);
        if(privilegeOpt.isEmpty()){
            return false;
        }

        Privilege privilege = privilegeOpt.get();

        boolean hasPrivilege = userPrivilegeRepository.existsByUserIdAndPrivilegeId(user.getId(), privilege.getId());

        logger.info("User valid: {}", hasPrivilege);

        if(!hasPrivilege){
            return false;
        }
        
        logger.info("User valid: {}", "TRADER_SALES".equalsIgnoreCase(user.getUserProfile().getUserType()));

        //check if trader owns the trade
        if("TRADER_SALES".equalsIgnoreCase(user.getUserProfile().getUserType()) && !operation.equalsIgnoreCase("getAllTrades")){
            return verifyTraderOwnership(user, operation, tradeDTO);
        };

        return true;
    }

    private String mapOperationToPrivilege(String operation){
        return switch(operation) {
            case "createTrade" -> "BOOK_TRADE";
            case "amendTrade" -> "AMEND_TRADE";
            case "getAllTrades" -> "READ_TRADE";
            case "terminateTrade" -> "TERMINATE_TRADE";
            case "cancelTrade" -> "CANCEL_TRADE";
            default -> null;
        };
    }

    private boolean verifyTraderOwnership(ApplicationUser user, String operation, TradeDTO tradeDTO){
        //createTrade does not require ownership check
        if(("createTrade").equals(operation)){
            return true;
        }

       if(tradeDTO == null){
            return false;
        }

        String tradeOwner = tradeDTO.getTraderUserName();
        String currentUserName = user.getFirstName() + " " + user.getLastName();

        return tradeOwner == null || tradeOwner.equalsIgnoreCase(currentUserName);
    }

    public ValidationResult validateReferenceDataStatus(TradeDTO tradeDTO){
        ValidationResult validationResult = new ValidationResult();
        List<String> errorMessages = new ArrayList<>();

        //1. Validate book exists and check if it is active
        if(tradeDTO.getBookName() != null){
            Optional<Book> bookOpt = bookRepository.findByBookName(tradeDTO.getBookName());
            if(bookOpt.isEmpty()){
                errorMessages.add("Book " + tradeDTO.getBookName() + " does not exist");
            }
            else if(!bookOpt.get().isActive()){
                errorMessages.add("Book " + tradeDTO.getBookName() + " is not active");
            } 
        }
        else{
            errorMessages.add("Book is required");
        }

        //2. Validate counterparty exists and check if it is active
        if(tradeDTO.getCounterpartyName() != null){
            Optional<Counterparty> counterpartyOpt = counterpartyRepository.findByName(tradeDTO.getCounterpartyName());
            if(counterpartyOpt.isEmpty()){
                errorMessages.add("Counterparty " + tradeDTO.getCounterpartyName() + " does not exist");
            }
            else if(!counterpartyOpt.get().isActive()){
                errorMessages.add("Counterparty " + tradeDTO.getCounterpartyName() + " is not active");
            } 
        }
        else{
            errorMessages.add("Counterparty is required");
        }

        //3. Validate trader user exists and check if it is active
        if(tradeDTO.getTraderUserName() != null){
            String[] nameParts = tradeDTO.getTraderUserName().split("\\s+");
            if(nameParts.length >= 1){
                String firstName = nameParts[0];
                Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
                if(userOpt.isEmpty()){
                    errorMessages.add("Trader user " + tradeDTO.getTraderUserName() + " does not exist");
                }
                else if(!userOpt.get().isActive()){
                    errorMessages.add("Trader user " + tradeDTO.getTraderUserName() +  " is not active");
                }
            }
        }else{
            errorMessages.add("Trader user is required");
        } 
        
        //4. Validate trade status
        if(tradeDTO.getTradeStatus() != null){
             
        Optional<TradeStatus> statusOpt = tradeStatusRepository.findByTradeStatus(tradeDTO.getTradeStatus());

        if(statusOpt.isEmpty()){
                errorMessages.add("Trade status " + tradeDTO.getTradeStatus() + "does not exist");
        }
        }
        
         //5. Validate trade type exists
        if(tradeDTO.getTradeType() != null){ 
        
        Optional<TradeType> tradeTypeOpt = tradeTypeRepository.findByTradeType(tradeDTO.getTradeType());

        if(tradeTypeOpt.isEmpty()){
                errorMessages.add("Trade type " + tradeDTO.getTradeType() + "does not exist");
        }
    } 

       //6. Validate trade sub type exists
        if(tradeDTO.getTradeSubType() != null){ 
        
        Optional<TradeSubType> tradeSubTypeOpt = tradeSubTypeRepository.findByTradeSubType(tradeDTO.getTradeSubType());

        if(tradeSubTypeOpt.isEmpty()){
                errorMessages.add("Trade sub type " + tradeDTO.getTradeSubType() + " does not exist");
        }
      }

       //7. Validate Trade Legs reference data 
       if(tradeDTO.getTradeLegs() != null){
          for(int i = 0; i < tradeDTO.getTradeLegs().size(); i++){
            TradeLegDTO leg = tradeDTO.getTradeLegs().get(i);
            String legNumber = "Leg " + (i + 1);

            if(leg.getCurrency() != null){
                Optional<Currency> currencyOpt = currencyRepository.findByCurrency(leg.getCurrency());
                if(currencyOpt.isEmpty()){
                errorMessages.add("Currency " + leg.getCurrency() + " does not exist");
                }
            }

            if(leg.getLegType() != null){
                Optional<LegType> legTypeOpt = legTypeRepository.findByType(leg.getLegType());
                if(legTypeOpt.isEmpty()){
                errorMessages.add(legNumber + " Leg type " + leg.getLegType() + " does not exist");
                }
            }

            if("floating".equalsIgnoreCase(leg.getLegType()) && leg.getIndexName() != null){
                Optional<Index> indexOpt = indexRepository.findByIndex(leg.getIndexName());
                if(indexOpt.isEmpty()){
                errorMessages.add(legNumber + " Index " + leg.getIndexName() + " does not exist");
                }
            }

            // if(leg.getCalculationPeriodSchedule() != null){
            //      Optional<Schedule> scheduleOpt = scheduleRepository.findBySchedule(leg.getCalculationPeriodSchedule());
            //     if(scheduleOpt.isEmpty()){
            //     errorMessages.add(legNumber + " Schedule " + leg.getCalculationPeriodSchedule() + " does not exist");
            //     }
            // }

            if(leg.getHolidayCalendar() != null){
                 Optional<HolidayCalendar> holidayCalendarOpt = holidayCalendarRepository.findByHolidayCalendar(leg.getHolidayCalendar());
                if(holidayCalendarOpt.isEmpty()){
                errorMessages.add(legNumber + " Holiday calendar " + leg.getHolidayCalendar() + " does not exist");
                }
            }

            if(leg.getPayReceiveFlag() != null){
                 Optional<PayRec> payRecOpt = payRecRepository.findByPayRec(leg.getPayReceiveFlag());
                if(payRecOpt.isEmpty()){
                errorMessages.add(legNumber + " Pay Receive flag " + leg.getPayReceiveFlag() + " does not exist");
                }
            }

            if(leg.getPaymentBusinessDayConvention() != null){
                 Optional<BusinessDayConvention> payBusinessOpt = businessDayConventionRepository.findByBdc(leg.getPaymentBusinessDayConvention());
                if(payBusinessOpt.isEmpty()){
                errorMessages.add(legNumber + " Payment Business Day Convention " + leg.getPaymentBusinessDayConvention() + " does not exist");
                }
            }
          }
       }

       if(errorMessages.isEmpty()){
          validationResult.setValid(true);
       }
       else{
         validationResult.setValid(false);
         validationResult.setErrors(errorMessages);
       }   

       return validationResult;
       
   }

   private ValidationResult validateTradeAndLegs(TradeDTO tradeDTO){
       ValidationResult cummulativeResult = new ValidationResult();

       List<String> allErrorMessages = new ArrayList<>();

       ValidationResult referenceDataResult = validateReferenceDataStatus(tradeDTO);

       if(!referenceDataResult.isValid()){
         allErrorMessages.addAll(referenceDataResult.getErrors());
       }
        
       ValidationResult businessRulesResult = validateTradeBusinessRules(tradeDTO);

       if(!businessRulesResult.isValid()){
         allErrorMessages.addAll(businessRulesResult.getErrors());
       }

       if(tradeDTO.getTradeLegs() != null && !tradeDTO.getTradeLegs().isEmpty()){
         TradeLegDTO leg1 = tradeDTO.getTradeLegs().get(0);
         TradeLegDTO leg2 = tradeDTO.getTradeLegs().get(1);

         List<TradeLegDTO> legs = Arrays.asList(leg1, leg2);

        ValidationResult tradeLegConsistencyResult = validateTradeLegConsistency(legs);

          if(!tradeLegConsistencyResult.isValid()){
            allErrorMessages.addAll(tradeLegConsistencyResult.getErrors());
           }
       }
       else{
          allErrorMessages.add("Trade must have 2 legs");
       }

       if(allErrorMessages.isEmpty()){
           cummulativeResult.setValid(true);
       }
       else{
         cummulativeResult.setErrors(allErrorMessages);
         cummulativeResult.setValid(false);
       }

       return cummulativeResult;
   }

   public List<Trade> searchTradesBySettlementInstructions(String searchString){
    //Additional infos that match the search parameter
       List<AdditionalInfo> additionalInfoSearchResult = additionalInfoRepository.findActiveByEntityTypeAndFieldNameAndFieldValue( "TRADE", "SETTLEMENT_INSTRUCTIONS", searchString);

       List<Long> tradeIds = additionalInfoSearchResult.stream()
       .map(AdditionalInfo::getEntityId)
       .distinct()
       .collect(Collectors.toList());

       return tradeRepository.findAllById(tradeIds);
   }

   public Trade updateSettlementInstructions(Long tradeId, String settlementInstructions){
      
    Optional<Trade> existingTradeOpt = getTradeById(tradeId);
        if (existingTradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }  
        
        Trade existingTrade = existingTradeOpt.get();

     if(settlementInstructions == null || settlementInstructions.trim().isEmpty()){
        additionalInfoService.removeAdditionalInfo("TRADE", tradeId, "SETTLEMENT_INSTRUCTIONS");
     } else{
        AdditionalInfoDTO additionalInfoDTO = new AdditionalInfoDTO();
        additionalInfoDTO.setEntityId(tradeId);
        additionalInfoDTO.setEntityType("TRADE");
        additionalInfoDTO.setFieldName("SETTLEMENT_INSTRUCTIONS");
        additionalInfoDTO.setFieldValue(settlementInstructions);
        additionalInfoDTO.setFieldType("STRING");

        additionalInfoService.addAdditionalInfo(additionalInfoDTO);

        logger.info("Settlement instructions saved for trade ID: {}", tradeId);
     }

     return existingTrade;
   }
  
   public TradeDTO addAdditionalInfo(TradeDTO tradeDTO){
     if(tradeDTO != null & tradeDTO.getId() != null){
        List<AdditionalInfoDTO> additionalFields = additionalInfoService.getAdditionalInfoForEntity("TRADE", tradeDTO.getId());
        tradeDTO.setAdditionalFields(additionalFields);
     }
     return tradeDTO;
   }
  
    
  
   
}
