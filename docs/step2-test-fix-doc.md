# Step 2 Test Fix Documentation

## Overview
This is a comprehensive documentation of all the failing backend tests that were identified after setting up and running the project successfully. In this documentation, each test case will be analyzed based on the following criteria: 
- **Problem Description**
- **Root Cause Analysis**
- **Solution Implemented**
- **Verification** 

The aim of this documentation is to provide a clear understsnding of the causes of test failures and errors, the reasoning behind each fix, and the verification process that ensures that all the issues were properly resolved.

## Summary of Test Results
After running `mvn clean install`, a total of **11 failing test cases** were identified. These test cases are grouped into two main categories:
- **Failures**: Tests that executed but failed due to incorrect logic (9 total).
- **Errors**: Tests that could not execute successfully due to thrown exceptions (2 total).


## Test Failures
This section documents all test failures and how each was fixed.
### 1. TradeControllerTest
**Total Failures: 6**

#### i. `testCreateTrade`
- **Problem Description:**
The `testCreateTrade` test in TradeController.java failed because it expected a `200 OK` response, but the endpoint returned `201 Created` after successfully creating a trade.

- **Root Cause Analysis:**
According to REST API best practices, a successful resource creation should return `201 Created`, not `200 OK`. The controller implementation in the `createTrade()` method of `TradeController.java` correctly returns `201`, but the test was incorrectly written to expect `200`, resulting in a logical mismatch.

- **Solution Implemented:**
The test expectation on line **138** in the `testCreateTrade` method was updated from `.andExpect(status().isOk())` to `andExpect(status().isCreated())`.
This ensures consistency between the test and the actual behaviour of the `createTrade()` method.

- **Verification:**
The test was rerun using the command: 
```
mvn -Dtest=TradeControllerTest#testCreateTrade test
```
and it passed successfully. Subsequently, a full build was executed with the command, 
```
mvn clean install
``` 
which confirmed the fix. The total test failures reduced from 9 to 8, and `testCreateTrade` was no longer listed among the failed tests.

#### ii. `testCreateTradeValidationFailure_MissingBook`
- **Problem Description:**
The `testCreateTradeValidationFailure_MissingBook` test in `TradeController.java` failed because it expected a `400 Bad Request` response, but the endpoint returned `201 Created` after creating a trade without a book.

- **Root Cause Analysis:**
In Spring Boot, required fields should be annotated with validation annotations such as `@NotNull`or `@NotBlank`, to ensure they are validated during request binding.
The `bookName` field in `TradeDTO.java` (around line **47**) did not have a `@NotNull` annotation. As a result, Spring Boot did not perform validation on this field, and the controller proceeded to create the trade without verifying the book data, returning `201 Created` instead of the expected `400 Bad Request` indicating invalid request data.

To correct this, a manual validation check was added inside `createTrade()` to ensure both `bookName` and `counterpartyName` are provided. This correctly triggered the `400 Bad Request` response. 
However, another issue arose because Spring's default validation (`@Valid`) intercepted the request before this manual validation, throwing a `MethodArgumentNotValidException`. Since this exception was not explicitly handled, the returned body response was empty instead of displaying the expected errror message `"Book and Counterparty are required"`.This required explicit exception handling to ensure that the custom message was returned to the client.   


- **Solution Implemented:**
A global exception handler was implemented using the `ControllerAdvice` and `ExceptionHandler(MethodArgumentNotValidException.class)` annotations to catch validation errors amd return meaningful error messages. This ensures that the custom message, `"Book and Counterparty are required"` is returned whenever either the `bookName` or `counterpartyName` fields is missing from the trade request. 

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
The `testDeleteTrade` test in `TradeController.java` failed because it expected a `204 No Content` response, but the endpoint returned `200 OK` after successfully deleting a trade.

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
which confirmed the fix. The total test failures reduced from 8 to 7, and `testDeleteTrade` was no longer listed among the failed tests.



#### iv. `testCreateTradeValidationFailure_MissingTradeDate`
- **Problem Description:**
The `testCreateTradeValidationFailure_MissingTradeDate` test in `TradeController.java` failed because it expected the response message, `"Trade date is required"`, but the returned message was an empty string `""`.

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
which confirmed the fix. The total test failures reduced from 7 to 6, and `testCreateTradeValidationFailure_MissingTradeDate` was no longer listed among the failed tests.






### Errors
There are 2 errors generated which are present in the following file:
- BookServiceTest: 2 errors were identified.