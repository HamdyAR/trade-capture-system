package com.technicalchallenge.controller;

import com.technicalchallenge.dto.SettlementInstructionsUpdateDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeSearchDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/trades")
@Validated
@Tag(name = "Trades", description = "Trade management operations including booking, searching, and lifecycle management")
public class TradeController {
    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    @Autowired
    private TradeService tradeService;
    @Autowired
    private TradeMapper tradeMapper;

    @GetMapping
    @Operation(summary = "Get all trades",
               description = "Retrieves a list of all trades in the system. Returns comprehensive trade information including legs and cashflows.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all trades",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<TradeDTO> getAllTrades(@RequestHeader("X-User-Id") String userId) {
        logger.info("Fetching all trades");
        return tradeService.getAllTrades(userId).stream()
                .map(tradeMapper::toDto)
                .map(tradeService::addAdditionalInfo)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trade by ID",
               description = "Retrieves a specific trade by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade found and returned successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Invalid trade ID format")
    })
    public ResponseEntity<TradeDTO> getTradeById(
            @Parameter(description = "Unique identifier of the trade", required = true)
            @PathVariable(name = "id") Long id) {
        logger.debug("Fetching trade by id: {}", id);
        return tradeService.getTradeById(id)
                .map(tradeMapper::toDto)
                .map(tradeService::addAdditionalInfo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new trade",
               description = "Creates a new trade with the provided details. Automatically generates cashflows and validates business rules.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Trade created successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid trade data or business rule violation"),
        @ApiResponse(responseCode = "500", description = "Internal server error during trade creation")
    })
    public ResponseEntity<?> createTrade(
            @Parameter(description = "Trade details for creation", required = true)
            @Valid @RequestBody TradeDTO tradeDTO,
            @RequestHeader("X-User-Id") String userId)
            {
        logger.info("Creating new trade: {}", tradeDTO);
        try {
            Trade trade = tradeMapper.toEntity(tradeDTO);
            tradeService.populateReferenceDataByName(trade, tradeDTO);
            Trade savedTrade = tradeService.saveTrade(trade, tradeDTO, userId);
            TradeDTO responseDTO = tradeMapper.toDto(savedTrade);
            responseDTO = tradeService.addAdditionalInfo(responseDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (Exception e) {
            logger.error("Error creating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error creating trade: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing trade",
               description = "Updates an existing trade with new information. Subject to business rule validation and user privileges.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade updated successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Invalid trade data or business rule violation"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to update trade")
    })
    public ResponseEntity<?> updateTrade(
            @Parameter(description = "Unique identifier of the trade to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated trade details", required = true)
            @Valid @RequestBody TradeDTO tradeDTO,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Updating trade with id: {}", id);
        try {
            // Validates the consistency between path and request body ID
            if(tradeDTO.getTradeId() != null && !tradeDTO.getTradeId().equals(id)){
               return ResponseEntity.badRequest().body("Trade ID in path must match Trade ID in request body");
            }

            tradeDTO.setTradeId(id); // Ensure the ID matches
            Trade trade = tradeMapper.toEntity(tradeDTO);
            trade.setId(id);// Ensure entity has ID for amendment
            tradeService.populateReferenceDataByName(trade, tradeDTO);
            Trade savedTrade = tradeService.saveTrade(trade, tradeDTO, userId);
            TradeDTO responseDTO = tradeMapper.toDto(savedTrade);
            responseDTO = tradeService.addAdditionalInfo(responseDTO);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error updating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error updating trade: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete trade",
               description = "Deletes an existing trade. This is a soft delete that changes the trade status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Trade cannot be deleted in current status"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to delete trade")
    })
    public ResponseEntity<?> deleteTrade(
            @Parameter(description = "Unique identifier of the trade to delete", required = true)
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Deleting trade with id: {}", id);
        try {
            tradeService.deleteTrade(id, userId);
            return ResponseEntity.ok().body("Trade deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error deleting trade: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate trade",
               description = "Terminates an existing trade before its natural maturity date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade terminated successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Trade cannot be terminated in current status"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to terminate trade")
    })
    public ResponseEntity<?> terminateTrade(
            @Parameter(description = "Unique identifier of the trade to terminate", required = true)
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Terminating trade with id: {}", id);
        try {
            Trade terminatedTrade = tradeService.terminateTrade(id, userId);
            TradeDTO responseDTO = tradeMapper.toDto(terminatedTrade);
            responseDTO = tradeService.addAdditionalInfo(responseDTO);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error terminating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error terminating trade: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel trade",
               description = "Cancels an existing trade by changing its status to cancelled")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade cancelled successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Trade cannot be cancelled in current status"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to cancel trade")
    })
    public ResponseEntity<?> cancelTrade(
            @Parameter(description = "Unique identifier of the trade to cancel", required = true)
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Cancelling trade with id: {}", id);
        try {
            Trade cancelledTrade = tradeService.cancelTrade(id, userId);
            TradeDTO responseDTO = tradeMapper.toDto(cancelledTrade);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error cancelling trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error cancelling trade: " + e.getMessage());
        }
         }     


@GetMapping("/search")
    @Operation(summary = "Multi-criteria trade search",
               description = "Search by counterparty, book, status, trader, and date ranges. Returns all trades that match the search criteria.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all matching trades",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<TradeDTO> searchTrade(
    @Parameter(description = "Search criteria - all fields are optional", required=false)    
    @ModelAttribute TradeSearchDTO searchDTO
    ) 
    {
        logger.info("Searching trade with criteria: {}", searchDTO);
        return tradeService.searchTrade(searchDTO).stream()
                .map(tradeMapper::toDto)
                .map(tradeService::addAdditionalInfo)
                .toList();
    }




    @GetMapping("/filter")
    @Operation(summary = "Paginated multi-criteria trade search",
               description = "Search by counterparty, book, status, trader, and date ranges with pagination support. Ideal for high-volume result sets.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated trades",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),                            
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Page<TradeDTO> filterTrade(
    @Parameter(description = "Search criteria - all fields are optional", required=false)    
      @ModelAttribute TradeSearchDTO searchDTO,
    @Parameter(description="Page number (0-indexed)", example="0")
     @RequestParam(defaultValue = "0") int page,
    @Parameter(description="Page size", example="20")
     @RequestParam(defaultValue = "20") int size,
    @Parameter(description="Sort by field", example="tradeDate")
     @RequestParam(defaultValue = "tradeDate") String sortBy,
    @Parameter(description="Sort direction(asc/desc)", example="desc")
     @RequestParam(defaultValue = "desc") String sortDir
    ) 
    {
        logger.info("Filtering trades with pagination - criteria: {}, page: {}, size: {}, sort: {} {}", searchDTO, page, size, sortBy, sortDir);

        //creating Pageable object
        Sort sort = sortDir.equalsIgnoreCase("asc")
          ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Trade> tradePage = tradeService.filterTradeWithPagination(searchDTO, pageable);


        return tradePage.map(tradeMapper::toDto).map(tradeService::addAdditionalInfo);
    }



    @GetMapping("/rsql")
    @Operation(summary = "RSQL based trade search",
               description = "Search trades using RSQL query language for advanced filtering, book, status, trader, and date ranges with pagination support. Ideal for high-volume result sets.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved trades matching RSQL query",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid RSQL query syntax"),                            
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Page<TradeDTO> searchTradeWithRSQL(
    @Parameter(description = "RSQL query string", required=true)    
      @RequestParam String query,
    @Parameter(description="Page number (0-indexed)", example="0")
     @RequestParam(defaultValue = "0") int page,
    @Parameter(description="Page size", example="20")
     @RequestParam(defaultValue = "20") int size,
    @Parameter(description="Sort by field", example="tradeDate")
     @RequestParam(defaultValue = "tradeDate") String sortBy,
    @Parameter(description="Sort direction(asc/desc)", example="desc")
     @RequestParam(defaultValue = "desc") String sortDir
    ) 
    {
        logger.info("RSQL search - query: {}, page: {}, size: {}, sort: {} {}", query, page, size, sortBy, sortDir);

        //creating Pageable object
        Sort sort = sortDir.equalsIgnoreCase("asc")
          ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Trade> tradePage = tradeService.searchTradeWithRsql(query, pageable);

        return tradePage.map(tradeMapper::toDto).map(tradeService::addAdditionalInfo);
    }


    @GetMapping("/search/settlement-instructions")
     @Operation(summary = "Search trades by settlement instructions",
               description = "Retrieves a list of trades that contain the specified text in their settlement instructions.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved trades that match settlement instructions",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid search parameter"),                            
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })    
    public ResponseEntity<List<TradeDTO>> searchBySettlementInstructions(
        @Parameter(description = "Setllement instructions string", required=true) 
    @RequestParam String instructions) {
    // Search trades by settlement instruction content
    
       List<TradeDTO> trades = tradeService.searchTradesBySettlementInstructions(instructions).stream()
                .map(tradeMapper::toDto)
                .map(tradeService::addAdditionalInfo)
                .toList();
          return ResponseEntity.ok(trades);
        
    }


    @PutMapping("/{id}/settlement-instructions")
    @Operation(summary = "Updates settlement instructions for a trade",
               description = "Updates an existing trade with new settlement instructions.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved trades that match settlement instructions",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Trade not found with provided ID"),                             
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })    
    public ResponseEntity<?> updateSettlementInstructions(
        @PathVariable Long id, 
        @Valid @RequestBody SettlementInstructionsUpdateDTO request) {
    // Update settlement instructions for existing trades
            Trade updatedTrade = tradeService.updateSettlementInstructions(id, request.getInstructions());
            logger.info("Settlment instructions successfully updated for trade with trade ID:{}", id);

            updatedTrade = tradeService.getTradeById(id).orElseThrow(() -> new RuntimeException("Trade not found after update"));

            TradeDTO tradeDTO = tradeMapper.toDto(updatedTrade);
            tradeDTO = tradeService.addAdditionalInfo(tradeDTO);
            return ResponseEntity.ok(tradeDTO);
}

}    

   
