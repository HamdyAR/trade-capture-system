# Project Documentation
This documentation outlines the technical decisions, design considerations, thought process and key learnings from all stages of this project.

## Step 2 Test Fix Documentation
### Overview
This is a comprehensive documentation of all the failing backend tests that were identified after setting up and running the project successfully. In this documentation, each test case will be analyzed based on the following criteria: 
- **Problem Description**
- **Root Cause Analysis**
- **Solution Implemented**
- **Verification** 

The aim of this documentation is to provide a clear understanding of the causes of test failures and errors, the reasoning behind each fix, and the verification process that ensures that all the issues were properly resolved.

### Summary of Test Results
After running `mvn clean install`, a total of **11 failing test cases** were identified. These test cases are grouped into two main categories:
- **Failures**: Tests that executed but failed due to incorrect logic (9 total).
- **Errors**: Tests that could not execute successfully due to thrown exceptions (2 total).


### Test Failures
This section documents all **9** test failures and how each was fixed.
### 1. TradeControllerTest
**Total Failures: 6**

#### i. `testCreateTrade`
- **Problem Description:**
The `testCreateTrade` test in `TradeControllerTest.java` failed because it expected a `200 OK` response, but the endpoint returned `201 Created` after successfully creating a trade.

- **Root Cause Analysis:**
According to REST API best practices, a successful resource creation should return `201 Created`, not `200 OK`. The controller implementation in the `createTrade()` method of `TradeController.java` correctly returns `201`, but the test was incorrectly written to expect `200`, resulting in a logical mismatch.

- **Solution Implemented:**
The test expectation on line **138** in the `testCreateTrade` method was updated from `.andExpect(status().isOk())` to `andExpect(status().isCreated())`.
This ensures consistency between the test and the actual behaviour of the `createTrade()` method.

- **Verification:**
The fix was verified by rerunning the previously failing test using: 
```
mvn -Dtest=TradeControllerTest#testCreateTrade test
```
and it passed successfully. Subsequently, a full build was executed with:
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 9 to 8, and `testCreateTrade` was no longer listed among the failed tests.

#### ii. `testCreateTradeValidationFailure_MissingBook`
- **Problem Description:**
The `testCreateTradeValidationFailure_MissingBook` test in `TradeControllerTest.java` failed because it expected a `400 Bad Request` response, but the endpoint returned `201 Created` after creating a trade without a book.

- **Root Cause Analysis:**
In Spring Boot, required fields should be annotated with validation annotations such as `@NotNull`or `@NotBlank`, to ensure they are validated during request binding.
The `bookName` field in `TradeDTO.java` (around line **47**) did not have a `@NotNull` annotation. As a result, Spring Boot did not perform validation on this field, and the controller proceeded to create the trade without verifying the book data, returning `201 Created` instead of the expected `400 Bad Request` indicating invalid request data.

To correct this, a manual validation check was added inside `createTrade()` to ensure both `bookName` and `counterpartyName` are provided. This correctly triggered the `400 Bad Request` response. 
However, another issue arose because Spring's default validation (`@Valid`) intercepted the request before this manual validation, throwing a `MethodArgumentNotValidException`. Since this exception was not explicitly handled, the returned body response was empty instead of displaying the expected error message `"Book and Counterparty are required"`.This required explicit exception handling to ensure that the custom message was returned to the client.   


- **Solution Implemented:**
A global exception handler was implemented using the `ControllerAdvice` and `ExceptionHandler(MethodArgumentNotValidException.class)` annotations to catch validation errors and return meaningful error messages. This ensures that the custom message, `"Book and Counterparty are required"` is returned whenever either the `bookName` or `counterpartyName` fields is missing from the trade request. 

- **Verification:**
The test was rerun using the command: 
```
mvn -Dtest=TradeControllerTest#testCreateTradeValidationFailure_MissingBook test
```
and it passed successfully. Subsequently, a full build was executed with the command, 
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 8 to 7, and `testCreateTradeValidationFailure_MissingBook` was no longer listed among the failed tests.

However, the stricter validation introduced by this fix generated additional issues in other test cases(`BookServiceTest` and `TradeServiceTest`), which were previously masked by the missing validation. These new errors will be analyzed and addressed in subsequent fixes.

#### iii. `testDeleteTrade`
- **Problem Description:**
The `testDeleteTrade` test in `TradeControllerTest.java` failed because it expected a `204 No Content` response, but the endpoint returned `200 OK` after successfully deleting a trade.

- **Root Cause Analysis:**
According to REST API best practices, a successful resource deletion typically returns `204 No Content` with no response body. 
However, in this project, the `deleteTrade()` method in `TradeController.java` is intentionally designed to return a message body for frontend display purposes. This makes the use of `200 OK` more appropriate, as it allows a confirmation message to be sent in the response.
The test, however, was still expecting `204 No Content`, creating a logical mismatch between the test expectation and the controller's behaviour.
Additionally, the response message `"Trade cancelled successfully"` was misleading, as the actual operation being performed is a deletion, not cancellation.

- **Solution Implemented:**
The expected status on line **223** in the test was updated from `.andExpect(status().isNoContent())` to `.andExpect(status().isOk())` to reflect the actual API behaviour. The controller's response message was updated from `ResponseEntity.ok().body("Trade cancelled successfully")` to  
`ResponseEntity.ok().body("Trade deleted successfully")` on line **144** ensuring the response message accurately describes the action performed. These changes align both the controller logic and the test expectations for consistency.


- **Verification:**
The test was rerun using the command: 
```
mvn -Dtest=TradeControllerTest#testDeleteTrade test
```
and it passed successfully. Subsequently, a full build was executed with the command, 
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 7 to 6, and `testDeleteTrade` was no longer listed among the failed tests.


#### iv. `testCreateTradeValidationFailure_MissingTradeDate`
- **Problem Description:**
The `testCreateTradeValidationFailure_MissingTradeDate` test in `TradeControllerTest.java` failed because it expected the response message, `"Trade date is required"`, but the returned message was an empty string `""`.

- **Root Cause Analysis:**
Spring's default validation (`@Valid`) in the controller implementation of `createTrade()` method of `TradeController.java` intercepted the request before this manual validation, throwing a `MethodArgumentNotValidException`. Since this exception was not explicitly handled, the returned body response was empty instead of displaying the expected errror message `"Trade date is required"`. This made explicit exception handling necessary to ensure that the custom message is returned to the client.   


- **Solution Implemented:**
The global exception handler introduced earlier using the `@ControllerAdvice` and `@ExceptionHandler(MethodArgumentNotValidException.class)` resolved this issue. It ensures that when validation fails, the correct custom error message, `"Trade date is required"`, is returned in the response. 


- **Verification:**
The test was rerun using the command: 
```
mvn -Dtest=TradeControllerTest#testCreateTradeValidationFailure_MissingTradeDate test
```
and it passed successfully. Subsequently, a full build was executed with the command, 
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 6 to 5, and `testCreateTradeValidationFailure_MissingTradeDate` was no longer listed among the failed tests.

#### v. `testUpdateTrade`
- **Problem Description:**
The `testUpdateTrade` test in `TradeControllerTest.java` failed because it expected a `tradeId` in the JSON response after updating a trade, but the response body did not contain it.

- **Root Cause Analysis:**
The `updateTrade()` method in `TradeController.java` was calling `amendTrade()` directly, while the service layer had introduced a new method, `saveTrade()` for controller compatibility. This `saveTrade()` method internally determines whether to call `createTrade()` or `amendTrade()` based on the presence of a trade ID.
This caused a logical mismatch between the controller and the test where the test was mocking `saveTrade()`, but the controller was invoking `amendTrade()` directly. 
As a result, the expected `tradeId` was not returned, since the mocked method did not align with the controller's actual behaviour.

- **Solution Implemented:**
The `updateTrade()` method in `TradeController.java` was refactored to call `saveTrade()` instead of `amendTrade()` to maintain controller-service consistency. 
This ensures that both the controller and test follow the same execution flow defined by the service layer.
 Specifically, this part of the `updateTrade()` method was updated:
  ```
            tradeDTO.setTradeId(id); // Ensure the ID matches
            Trade trade = tradeMapper.toEntity(tradeDTO);
            trade.setId(id);// Ensure entity has ID for amendment
            tradeService.populateReferenceDataByName(trade, tradeDTO);
            Trade savedTrade = tradeService.saveTrade(trade, tradeDTO);
  ```
This change ensures that the mocked service behaviour in the test aligns with the actual controller flow and that the correct JSON response containing the `tradeId` is returned. 

- **Verification:**
The fix was confirmed by rerunning the previously failing test using: 
```
mvn -Dtest=TradeControllerTest#testUpdateTrade test
```
and it passed successfully. Subsequently, a full build was executed with: 
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 5 to 4, and `testUpdateTrade` was no longer listed among the failed tests.


#### vi. `testUpdateTradeIdMismatch`
- **Problem Description:**
The `testUpdateTradeIdMismatch` test in `TradeControllerTest.java` failed because it expected a `400 Bad Request` response, but the endpoint returned `200 OK` after updating a trade without detecting mismatch between the path variable,`id`, and the `tradeId` in the request body.
- **Root Cause Analysis:**
The test is designed to ensure that any discrepancy between the path variable `id` in the `updateTrade` URL and the `tradeId` in the request body triggers a validation error. However, the `updateTrade()` method contained the following line(**119**):
```
tradeDTO.setTradeId(id);
```
This line executed before any validation occurred, which meant that if a client or test provided mismatched IDs, the controller logic silently overwrote the request body's value with the path variable value. Consequently, input was treated as valid and passed through to the service layer, preventing the expected `400 Bad Request` from being returned. 
- **Solution Implemented:**
The controller method, `updateTrade()` was refactored to validate the exactness between the path variable `id` and the `tradeId` in the request body before proceeding with the update. If a mismatch is detected, the method now returns a `400 Bad Request` repsonse with a clear error message.
The following validation check was added before the update logic:
```
 if(tradeDTO.getTradeId() != null && !tradeDTO.getTradeId().equals(id)){
    return ResponseEntity.badRequest().body("Trade ID in path must match Trade ID in request body")
 }

```
This ensures that invalid requests with mismatched IDs now produce the correct HTTP response code and a descriptive error message.
- **Verification:**
The fix was verified by rerunning the previously failing test using: 
```
mvn -Dtest=TradeControllerTest#testUpdateTradeIdMismatch test
```
The test passed successfully, confirming that the controller now returns a `400 Bad Request` when the path and body trade IDs differ. Subsequently, a full build was executed with: 
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 4 to 3, and `testUpdateTradeIdMismatch` was no longer listed among the failed tests.

### 2. TradeLegControllerTest
**Total Failures: 1**
#### i. `testCreateTradeLegValidationFailure_NegativeNotional`
- **Problem Description:**
The `testCreateTradeLegValidationFailure_NegativeNotional` test in `TradeLegControllerTest.java` failed because it expected the response message, `"Notional must be positive"` , but the returned message was an empty string `""`.
- **Root Cause Analysis:**
Spring's default validation (`@Valid`) in the controller implementation of `createTradeLeg()` method of `TradeLegController.java` intercepted the request before this manual validation, throwing a `MethodArgumentNotValidException`. Since this exception was not explicitly handled, the returned body response was empty instead of displaying the expected errror message `"Notional must be positive"`. This made explicit exception handling necessary to ensure that the custom message is returned to the client.   
- **Solution Implemented:**
The global exception handler introduced earlier in fix 2 using the `@ControllerAdvice` and `@ExceptionHandler(MethodArgumentNotValidException.class)` resolved this issue. It ensures that when validation fails, the correct custom error message, `"Notional must be positive"`, is returned in the response. 
- **Verification:**
The test was rerun using the command: 
```
mvn -Dtest=TradeLegControllerTest#testCreateTradeLegValidationFailure_NegativeNotional test
```
and it passed successfully. Subsequently, a full build was executed with the command, 
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 3 to 2, and `testCreateTradeLegValidationFailure_NegativeNotional` was no longer listed among the failed tests.


### 3. TradeServiceTest
**Total Failures: 2**
#### i. `testCashflowGeneration_MonthlySchedule`
- **Problem Description:**
The `testCashflowGeneration_MonthlySchedule` test in `TradeServiceTest.java` was initially incomplete and failed due to a placeholder assertion and missing dependencies. The test had no valid reference data setup, no call to the cashflow generation method, and contained a hardcoded failing assertion,`assertEquals(1, 12)`, which caused it to always fail.  

- **Root Cause Analysis:**
The cashflow generation method, `generateCashflows()` in `TradeService.java` is private. To call it, the `TradeService.createTrade()` method has to be invoked. However, `createTrade()` depends on several reference data repositories(`BookRepository`, `CounterpartyRepository`,`TradeStatusRepository` and `ScheduleRepository`) to populate and validate a trade before generating cashflows.
These dependencies were not mocked in the original test, and the trade was never configured with relevant reference data. Similarly, the trade legs were missing a valid schedule. Consequently, the test threw runtime exceptions like:
```
Runtime Exception: Book not found or not set
```
Additionally, the cashflow generation logic did not attach generated cashflows to the trade legs,leaving the test logically incomplete and causing null pointer exceptions in assertions.
- **Solution Implemented:**
The `testCashflowGeneration_MonthlySchedule` test was fully implemented and refactored, and the service logic was slightly enhaced to make it testable:
- 1. **Test Changes:**
   - Created and configured valid `TradeDTO`, `TradeLegDTO`  and reference data 
      (Book, Counterparty, TradeStatus and Schedule) objects.
   - Assigned a monthly schedule `("1M")` to both trade leg DTOs.
   - Mocked all necessary repository lookups used during 
      `populateReferenceDataByName()` and `populateLegReferenceData()` execution.
   - Called `tradeService.createTrade()` to trigger actual cashflow generation. 
   - Asserted that each trade leg of the created trade had 12 cashflows each for a monthly schedule.

- 2. **Service changes:**
   - In `TradeService.generateCashflows()`, after creating each `Cashflow` and saving it, this was added:
   ```
   leg.getCashflows().add(cashflow);
   ```
   to ensure that each `TradeLeg` object holds all generated cashflows, allowing the test to assert their count.
   - In `TradeService.createTradeLegsWithCashflows()`, after saving the trade legs and generating their corresponding cashflows, this was added:
   ```
   savedTrade.setTradeLegs(legs);
   ```  
   to ensure that the saved trade returned by the service has the populated trade legs, enabling assertions on cashflows.

- **Verification:**
The fix was verified by rerunning the test using:
```
mvn -Dtest=TradeServiceTest#testCashflowGeneration_MonthlySchedule test
```
The test passed successfully, confirming correct monthly cashflow generation.
Subsequently, a full build was executed with:
```
mvn clean install
```
which confirmed the fix. `testCashflowGeneration_MonthlySchedule` was no longer listed among the failing tests and the total test failure count reduced from 2 to 1.


#### ii. `testCreateTrade_InvalidDates_ShouldFail`
- **Problem Description:**
The `testCreateTrade_InvalidDates_ShouldFail` test in `TradeServiceTest.java` failed because it expected the error message, `"Wrong error message"` but the actual message returned by the service was `"Start date cannot be before trade date"`
- **Root Cause Analysis:**
The test contained a placeholder assertion that did not match the actual validation message produced by the `TradeService.createTrade()` method. The method correctly throws an exception when the start date is earlier than the trade date, but the test had not been updated to reflect the intended validation rule.
- **Solution Implemented:**
The assertion in test was updated to match the actual expected validation message:
```
assertEquals("Start date cannot be before trade date", startDateException.getMessage());
```
Additionally, a change was made to the test logic to improve the date validation for both start date and maturity dates, which enforces the business rule. The check for maturity date was introduced to the test as follows:
```
assertEquals("Maturity date cannot be before start date", maturityDateException.getMessage());
```
- **Verification:**
The test was rerun using:
```
mvn -Dtest=TradeServiceTest#testCreateTrade_InvalidDates_ShouldFail test
```
The test now passes successfully, confirming correct date validation behaviour.

The overall build was verified using:
```
mvn clean install
```
The total test failure count reduced from 1 to 0 indicating that all the test failures have been resolved successfully including `testCreateTrade_InvalidDates_ShouldFail`.


### Errors
This section documents all **2** test errors identified from running the first build, and **3** test errors that arose after fixing some test failures earlier, and how each was fixed.
**Total errors: 5**
### 1. BookServiceTest
**Total errors from first build: 2**
#### i. `testFindBookById`
- **Problem Description:**
The `testFindBookById` test in `BookServiceTest.java` failed with a `NullPointerException` at line **29** when calling the `getBookById()` method in `BookService.java`.
- **Root Cause Analysis:**
Inside `BookService.getBookById()` on line **37**, the method:
```
return bookRepository.findById(id).map(bookMapper::toDto);
```
uses `bookMapper` to convert a Book entity to a DTO.
In the test, while the `bookRepository` was properly mocked, `bookMapper` was never mocked, leading to a null reference when the method tried to call `bookMapper.toDto()`.

- **Solution Implemented:**
The test was refactored to:
   - Create a valid BookDTO mock object:
     ```
        BookDTO bookDTO = new BookDTO();
        bookDTO.setId(1L);
        bookDTO.setBookName("Book1");
     ```
   - Mock bookMapper behaviour:
     ```
     when(bookMapper.toDto(book)).thenReturn((bookDTO));
     ```
   - Add extra assertion to validate results:
     ```
     assertEquals("Book1", found.get().getBookName());
     ```
   - Verify mock interactions:
     ```
     verify(bookRepository).findById(1L);
     verify(bookMapper).toDto(book);
     ``` 
This ensures that all dependencies are properly mocked and tested through the service layer.   
   
- **Verification:**
The fix was verified by rerunning the test:
```
mvn -Dtest=BookServiceTest#testFindBookById test
```
The test passed successfully, confirming that the `bookMapper` was correctly injected and mocked.
A full build was also executed using:
```
mvn clean install
```
where the total test error count reduced from 5 to 4, confirming no new test failures were introduced.

#### ii. `testFindBookByNonExistentId`
- **Problem Description:**
The `testFindBookByNonExistentId` test in `BookServiceTest.java` failed with a `NullPointerException`. The test expected an empty `Optional` when no book was found, but the service threw an exception.
- **Root Cause Analysis:**
The `BookService.getBookById()` method uses:
```
return bookRepository.findById(id).map(bookMapper::toDto);
```
The `bookMapper` was not mocked in the test so mapping resulted in a null reference hence triggering the `NullPointerException`.
- **Solution Implemented:**
The earlier fix for `testFindBookById` included properly mocking `bookMapper.toDto()`. With that setup, the service now safely returns `Optional.empty()` safley for non-existent IDs. No additional changes were required for this test.
- **Verification:**
Rerunning the test using:
```
mvn -Dtest=BookServiceTest#testFindBookBNonExistentId test
```
confirms the test passes, and `BookService.getBookById()` safely handles non-existent books. A full build (`mvn clean install`) shows no new failures and the total error count reduced from 4 to 3.

#### iii. `testSaveBook`
- **Problem Description:**
The `testSaveBook` test was failing with a `NullPointerException`:
```
NullPointer Cannot invoke "com.technicalchallenge.mapper.BookMapper.toEntity(com.technicalchallenge.dto.BookDTO)" because "<local3>.bookMapper" is null
```
This occurred when attempting to call `bookService.save(bookDTO)`.
- **Root Cause Analysis:**
The `BookService.saveBook()` method internally calls:
```
Book book = bookMapper.toEntity(bookDTO);
```
However, in the test setup, the `bookMapper` dependency was not mocked. As a result, `bookMapper` was null during execution, leading to a `NullPointerException`.
- **Solution Implemented:**
The fix involved properly mocking the `bookMapper` interactions within the test:
```
when(bookMapper.toEntity(bookDTO)).thenReturn(book);
when(bookMapper.toDTO(savedBook)).thenReturn(savedBookDTO);
```
This ensures that the service can convert entity and DTO without encountering null references.
- **Verification:**
The fix was verified by running:
```
mvn -Dtest=BookServiceTest#testSaveBook test
```
The test passed successfully. A successful full build `mvn clean install` confirmed that the `NullPointerException` no longer occurs and no new test failures were introduced. All the total error count reduced from 3 to 2.



### 2. TradeServiceTest
**Total errors: 3**
#### i. `testAmendTrade_Success`
- **Problem Description:**
The `testAmendTrade_Success` test failed with a `NullPointerException`:
```
NullPointer Cannot invoke "java.lang.Integer.intValue()" because the return value of "com.technicalchallenge.model.Trade.getVersion()" is null
```
This occurred during the execution of `amendedTrade.setVersion(existingTrade.getVersion() + 1);` on line **279** of the `amendTrade()` method in `TradeService.java`.
- **Root Cause Analysis:**
The `amendTrade()` method follow these steps when ammending a trade:
   - 1.  Find the existing trade by its `tradeId`
   - 2.  Deactivate the existing trade (set `active` to `false` and record the deactivated date)
   - 3.  Save the deactivated trade
   - 4.  Create a new version of the trade by incrementing the version by 1.
   - 5.  Set the trade status to "AMENDED"
   - 6.  Save the amended trade
   - 7.  Create new trade legs and generate corresponding cashflows 
The failure occurred at `step 4` because the test setup created a trade without a version. As a result, `getVersion()` returned null, and the call to increment it(`+ 1`) triggered a `NullPointerException`.  
After fixing this, a second error occurred:
```
NullPointer Cannot invoke "com.technicalchallenge.model.TradeLeg.getLegId()" because "leg" is null
```
This happened at line **472** in the `generateCashflows()` method of `TradeService.java`:
```
logger.info("Generating cashflows for leg {} from {} to {}", leg.getLegId(), startDate, maturityDate);
```
The test had not mocked the `TradeLegRepository` to create trade legs with assigned IDs, leading to a null `leg` reference during cashflow generation. 
- **Solution Implemented:**
The test was updated as follows:
  -  Set `trade.setVersion(1)` to initialize the version for new trades in the test setup.
  -  Mocked the `TradeLegRepository` to return the saved `TradeLeg` object when saving. 
These changes ensured that the trade version was set and trade legs were correctly generated with valid IDs.
- **Verification:**
The fix was verified by running:
```
mvn -Dtest=TradeServiceTest#testAmendTrade_Success test
```
The test passed successfully. 
A full build (`mvn clean install`) confirmed the fix, with no new test failures were introduced. 
The total error count decreased from 2 to 1, confirming the resolution of the `NullPointerException`.

#### ii. `testCreateTrade_Success`
- **Problem Description:**
The `testCreateTrade_Success` test in `TradeServiceTest.java` initially failed with two sequential errors during execution of `tradeService.createTrade(tradeDTO)`:
   1.
      ```
      Runtime Book not found or not set
      ```
   This occurred because the service could not resolve the required reference data(`Book`, `Counterparty`, `TradeStatus`) when populating the trade.   
   2.
      ```
      NullPointer Cannot invoke "com.technicalchallenge.model.TradeLeg.getLegId()" because "leg" is null
      ```
   This happened after resolving the first issue, when generating cashflows for trade legs, the service expected valid `TradeLeg`s with assigned IDs, but the mocked repository returned null.
- **Root Cause Analysis:**
The first failure (`Book not found or set`) occurred because the test setup did not mock the reference data repositories used inside `populateReferenceDataByName()`.
The second failure (`NullPointerException`) occurred because `tradeLegRepository.save(any(TradeLeg.class)` was not mocked to return valid `TradeLeg`s.
Consequently, the service received a `null` `TradeLeg` and crashed when trying to access its `legId` during cashflow generation.
- **Solution Implemented:**
The test setup was enhanced to fully prepare valid and consistent data for the service:
   - Created valid `Book`, `Counterparty`, and `TradeStatus` entities, and configured the `tradeDTO` with their corresponding names.
   - Mocked repository lookups:
     ```
      when(bookRepository.findByBookName(any(String.class))).thenReturn(Optional.of(new Book()));
      when(counterpartyRepository.findByName(any(String.class))).thenReturn(Optional.of(new Counterparty()));
      when(tradeStatusRepository.findByTradeStatus(any(String.class))).thenReturn(Optional.of(new TradeStatus()));
     ```
   - Mocked the `TradeLegRepository` to produce valid `TradeLeg`s attached to the created trade:
      ```
      when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(invocation -> invocation.getArgument(0));
      ```  
These updates ensured all dependencies required by `TradeService.createTrade()` were properly satisfied, preventing both runtime and null pointer exceptions.      
- **Verification:**
The fix was verified by executing:
```
mvn -Dtest=TradeServiceTest#testCreateTrade_Success test
```
The test passed successfully. 
A full build (`mvn clean install`) confirmed no new test failures. 
The total number of errors decreased from 1 to 0 as both exceptions were fully resolved.


## Conclusion
After successfully fixing all the test failures and errors, the `TradeServiceTest` class was refactored to improve maintainability and adhere to the `DRY(Don't Repeat Yourself)` software engineering principle.

During debugging, it was observed that several repository mocks were repeatedly created  in `testCreateTrade_Success` and `testCashflowGeneration_MonthlySchedule`. These included mocks for:
   - BookRepository,
   - CounterpartyRepository
   - TradeStatusRepository 
   - TradeRepository
   - TradeLegRepository 

To eliminate this redundancy, a helper method named `createTradeMocks()` was introduced. This method covers the setup of common mocks. By calling this method within both tests, it eliminated duplicate setup code, simplified the test structure and improved overall readability.

This marks the successful completion of Step 2 of the project. All test failures and errors were resolved and the test suites now run successfully.



## Step 3 (Implement Missing Functionality) Documentation

### Overview
This documentation highlights the technical decisions and design considerations that were adopted in implementing the enhancements stated in step 3.

### Enhancement 1: Advanced Trade Search 
The implementation of the **Advanced Trade Search** aims to improve the user experience for traders by enabling efficient and flexible trade retrieval. This enhancement direcly addresses the business requirement to allow traders to quickly find trades using the multiple search criteria.

#### Implementation Details
The following REST endpoints were implemented to support multi-search, paginated and advanced query searches:

- `@GetMapping("/search")`: 
Provides the multi-search criteria functionality using parameters such as  **counterparty**, **book**, **trader**, **status** and **date ranges**.

- `@GetMapping("/filter")`: Enables pagination support for high-volume result sets, allowing traders to navigating through trade lists efficiently. 

- `@GetMapping("/rsql")`: Provides advanced search capabilities using **RSQL (RESTful Service Query Language)** for power users who require complex and dynamic querying.

### Technical Approach

#### Design Decisions
A single endpoint `/search` was implemented with optional query parameters rather than multiple dedicated endpoints. This design was chosen for the following reasons:
- Extensibility: New search parameters can be added easily without changing the endpoint structure.
- Maintainability: Reduces repetitive controller and repository code.
- REST Compliance: Keeps the API resource-centric (`trades/search`) rather than action-centric e.g. (`/searchByCounterparty`, `searchByBook` etc).

A single endpoint `/filter` was implemented to support high-volume trade retrieval. Pagination was adopted to this effect with the use of the `Pageable` class.

The endpoint `rsql` was used for advanced querying with RSQL. The `RSQLParser` was added to the `pom.xml` file


#### Controller Design Considerations
Two main approaches were evaluated for handling multiple query parameters in `TradeController`:
- `@RequestParam` - simple for few filters(1-3) but cumbersome with many(4+ filters).
- `@ModelAttribute` - clean and extensible for multiple optional filters but requires a request DTO class.

#### Decision
The `@ModelAttribute` was used with a `TradeSearchDTO` as its request class.

#### Justification
It encapsulates all the search criteria in a single request object resulting in cleaner and more maintainable controller code. It also makes it easy to extend the search endpoint with new parameters as updates will be made only in the `TradeSearchDTO` class and avoids multiple `@RequestParam` definitions.

#### Repository/Service Design Considerations
Three approaches were evaluated to integrate the multi-search in `TradeRepository`:
- `Multiple repository methods` - each filter combination will require a method created and for 6 parameters, that would mean about 15 methods.
- `Spring Data JPA Specification` - it has support for dynamic filtering and pagination
- `QueryDSL` - it is powerful but complex.

#### Decision
The `Spring Data JPA Specification` was adopted for dynamic multi criteria searchwith a `TradeSearchDTO` as its request class.

#### Justification
- It is scalable and maintainable for multiple optional filters
- It works seamlessly with `Pageable` for pagination
- It is supported natively in Spring Data(no external dependency required).


### Frontend Adjustments
The following adjustments were made to the frontend to integrate the multi-search endpoint (`/search`):
 - Added an Advanced search form implemented with a tab design for users to switch seamlessly between basic and advanced search modes. This form was bulit with 6 input fields each for the parameters(Counterparty, Book, Status, Trader, Start Date and End Date) involved where the user can type in the input field of the parameter that they want to search the trades with and hit the search button to call the `/search` API.
 - Added an RSQL mode toggle for power users to input advanced query strings which enables the calling of the `/rsql` endpoint.
 - Implemented pagination controls on the trade table for efficient navigation of large datasets which enables the calling of the `/filter` endpoint.
 

### Enhancement 2: Trade Validation Engine
This enhancement ensures that all trades created or amended in the system strictly comply with business and operational rules. It prevents invalid or unauthorized trades from being processed, thereby maintaining dat integrity and regulatory compliance. The validation engine covers four key areas:
1. Trade Business Rules Validation 
2. Trade Leg Consistency Validation
3. User Privilege Validation
4. Entity and Reference Data Validation

The required validation methods include: 
```
public ValidationResult validateTradeBusinessRules(TradeDTO tradeDTO)

public boolean validateUserPrivileges(String userId, String operation, TradeDTO tradeDTO)

public ValidationResult validateTradeLegConsistency(List<TradeLegDTO> legs)
```

#### ValidationResult Structure 
A reusable `ValidationResult` was designed to standardize responses from all validation methods.

**Structure**
- `isValid`: a boolean indicating whether the trade or trade legs passed validation.
- `errors`: a list of descriptive error messages specifying each validation failure.

**Design Rationale:**
Returning a comprehensive list of all failed checks improves the user experience and efficiency because users can correct all issues in one step instead of discovering them sequentially. This approach also enhances usability during trade operations such as create, amend, cancel and terminate.


#### Authorization Design Considerations
The system initially lacked any form of user authorization in the backend. To support privilege validation, users needed to be identified via their loginId(used as `userId` in validation method).


 The following options were considered to implement authorization: 
- Request Headers: This involves the client passing the userId which is the loginId as a request header after a user successfully logs into the application.
- Spring Security: This involvoves the use of Spring Security library to enforce authorization.

#### Decision
Request Headers was adopted for implementing the authorization.

#### Justification
The Request Headers approach offers a practical balance between simplicity, compatibility, and future extensibility within the existing system.

It aligns seamlessly with the existing simple username-password authentication mechanism, where users are identified by their loginId.

It allows the frontend interceptor to automatically attach the userId (loginId) in every API request header — enabling consistent backend authorization checks.

It avoids intrusive changes to the existing authentication codebase; authorization logic can be added without reworking existing login functionality.

The design supports future upgrades — if Spring Security is later introduced, the same request header and interceptor setup can be leveraged within the Spring Security filter chain.

Overall, this approach satisfies the current authorization requirement, keeps the codebase stable, and preserves forward compatibility for future security enhancements.




### Enhancement 3: Trader Dashboard and Blotter System
The enhancement enables traders to see crucial data and metrics that facilitate theur operations.

#### Implementation Details
The following REST endpoints were implemented to support multi-search, paginated and advanced query searches:

- `@GetMapping("/my-trades")`: 
The endpoint serves to display only a trader's trades to ensure proper access control and encapsulation.

- `@GetMapping("/book/{id}/trades")`: Shows all trades for a specific book. 

- `@GetMapping("/summary")`: Shows summary of a trader's trading activity.

- `@GetMapping("/daily-summary")`: Shows summary of today's trading activity with historical comparison.


## Step 4 (Bug Investigation and Fix) Documentation
This documentation entails a comprehensive report of the root cause analysis of the cashflow calculation bug investigated and fixed in step 4. The following criteria will be covered in this report: Executive Summary, Technical Investigation, Root Cause Details and Proposed Solution.

### Executive Summary
The bug identified as `TRD-2025-001` involves a critical error in the cashflow calculation logic used for fixed-leg trades. The issue causes fixed-leg cashflows to be approximately 100 times larger than expected, significantly overstating payment amounts (e.g., a $10M trade at a 3.5% rate generating ~$875,000 instead of $87,500 per quarter).

This bug has a high business impact, as it directly affects production trading operations and can lead to incorrect settlements and financial misstatements, particularly in P&L and other related financial reports. Additionally, minor precision issues in the calculation introduce small variations between trades. Prompt resolution is essential to prevent financial exposure and maintain the integrity of trading analytics and risk reporting and financial controls.

### Technical Investigation
The debugging process began with a thorough examination of the cashflow calculation logic implemented in the `calculateCashflowValue()` method within `TradeService.java`. Since the issue affected fixed-leg cashflows, the investigation focused primarily on the formula used to derive the interest amount. reviewed to ensure that it is right for calculating the interest amounts which is the simple interest formula, `(Notional × Rate × Months) ÷ 12`. Next, the data type used in each parameter of the formula was checked to verify that it conforms with the standard data type used for monetary calculations. Subsequently, the rate percentage handling was checked to see if the rate percentage was handled by converting to a decimal.

It was discovered that the rate percentage handling was not implemented and some inappropriate data types like `double` were used in the fixed-leg cashflow amount calculation after reviewing the cashflow calculation logic.

#### Debugging Methodology
- 1. Recreation of the error: The fixed-leg scenario that was cited in the problem statement was recreated ($10M trade at a 3.5% rate generating ~$875,000) to verify if the result is true. I created a test for the `cashflowValue()` method and the result generated was `8,750,000` instead of the `875,000` mentioned in the problem statement.

- 2. Formula Review
The formula used for calculating fixed-leg cashflows, `(Notional * Rate * Months) ÷ 12`, was reviewed to confirm its correctness for computing simple interest.
- 3. Data Type Validation
Each variable used in the calculation `(notional, rate, and months)` was checked to ensure appropriate data types were being used, particularly for monetary calculations where precision is critical.
- 4. Rate Percentage Handling Check
A further review was made to how the interest rate was being processed, specifically whether the rate (expressed as a percentage e.g. 3.5%) was correctly converted into decimal form(0.035) before being applied in the calculation.

#### Findings
The investigation revealed two key issues contributing to the incorrect cashflow values:
- 1. Rate Misinterpretation
The rate percentage was being used directly in its percentage form (e.g. 3.5 instead of 0.035), leading to fixed-leg cahsflows that were approximately 100 larger than the expected values.
- 2. Use of Inappropriate Data Types
The calculation used the `double` primitive type for monetary values instead of the more orecise `BigDecimal`, introducing floating-point precision errors and minor variations between trades.

These combined issues resulted in overstated fixed-leg cashflows and inconsistent precision across calculations.

### Root Cause Details
The following bugs were identified after a thorough technical investigation:
- 1. **Rate Percentage Misinterpretation**
   - **Description**
   The `rate` value in the `calculateCashflowValue()` method was used directly as entered (e.g. `3.5` for 3.5%) instead of converting it to its decimal equivalent (`0.035`).
   - **Why It Occurred**
   The implementation assuemd that the rate was already in decimal form when passed to the calculation logic. However, in the trade capture process, rates are stored and presented as percentages for user readability. The mismatch between data representation (percentage vs decimal) caused the interest calculation to be inflated by a factor of approximately 100.

- 2. **Inappropriate use of Floating-Point Data Types**
   - **Description**
   The method used primitive data type `double` for performing arithmetic operations involving notional amounts and rates before converting the final result to `BigDecimal`.
   - **Why It Occurred**
   The `double` data type was likely used for convenience without realizing that floating-point arithmetic introduces rounding and precision errors whcih should be avoided especially in financial calculations where even small discrpeancies matter.

- 3. **Lack of Validation and Testing**
- **Description**
The absence of validation checks and unit test for the `calculateCashflowValue()` allowed the incorrect rate percentage handling and data type usage to go undetected.
- **Why It Occurred**
The test coverage likely focused on functional corretcness (the method runs without errors) rather than verifying financail accuracy and consistency across different rate ans notional scenarios.

### Proposed Solution
The simple interest formula was refactored accordingly to make use of the approrpriate data type, `double` for every variable in the `calculateCashflowValue()` method to prevent floating-point errors and inflated cashflow values were prevented by implementing proper rate handling by dividing by 100.

The fix was verified using the `testCashflowValue_QuarterlySchedule` method which correctly generated a value of 87500.00 for a $10M trade at a 3.5% rate. Other schedules(e.g. monthly, yearly etc) were tested successfully.


## Step 5 (Full-Stack Feature Implementation) Documentation
This documentation covers the design considerations and justifications applied while implementing the settlement instructions feature on the backend and frontend side of the application.

### Design Considerations

#### Additional Info vs Direct Table
The additional info extensibility and direct table adjsutment options were presented to implement the settlement instructions feature.

**Decision**
I adopted the additional info method to implement settlement instructions.

- **Justification**
Fully extensible — supports future settlement-related attributes.

No schema changes required (uses existing AdditionalInfo structure).

Follows enterprise-grade design patterns used in scalable trade systems.

Enables reuse of existing AdditionalInfo service/repository logic


**Data Validation**
The annotations @NotNull, @Size, and @Pattern were used to enforce the data validation rules regarding 10–500 characters and unsafe/special characters.


**Frontend Integration Requirements**
Trade Booking Form Enhancement

- Added a Settlement Instructions input field in the Trade Booking Modal.

- Displayed settlement instructions in the trade detail view.

- Added a “Settlement Instructions” column to the trade blotter/grid.




