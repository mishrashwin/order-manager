# TESTING RULES

## PURPOSE
This document defines the **authoritative testing strategy** for the Order Manager system. It ensures code quality, reliability, and maintainability through comprehensive test coverage.

Violation of any rule → **DO NOT MERGE**

---

# 1. COVERAGE REQUIREMENTS (MANDATORY)

## 1.1 Minimum Coverage
- **Target**: >90% code coverage
- **Enforcement**: All services must have unit tests
- **Critical flows**: Must have integration tests

## 1.2 Mandatory Test Rule
**Every new or changed controller/service method MUST include dedicated automated tests** in the same development cycle:
- At least one happy-path test case
- At least one edge/failure-path test case

---

# 2. TEST LAYERS

## 2.1 Service Layer Testing
**Purpose**: Test business logic, edge cases, and exceptions

**Test Coverage**:
- Business logic validation
- Edge cases (null inputs, boundary values, invalid states)
- Exception handling
- Large dataset handling
- State transitions

**Example Scenarios**:
- Valid PO creation
- Empty item list → should fail
- Invalid GST → should fail
- Invalid state transition
- Large dataset handling
- Null inputs

**Tools**:
- JUnit 5
- Mockito

## 2.2 Controller Testing
**Purpose**: Test HTTP request/response handling

**Test Coverage**:
- API success responses
- Validation failures
- Authorization failures
- Bad requests
- Redirect behavior (for MVC controllers)

**Tools**:
- JUnit 5
- MockMvc
- Mockito

## 2.3 Integration Testing
**Purpose**: Test full flow from Controller → Service → Repository

**Test Coverage**:
- Full request lifecycle
- Database interactions
- Transaction handling
- End-to-end workflows

**Tools**:
- JUnit 5
- Spring Boot Test
- TestContainers (optional for real DB)

## 2.4 Edge Case Testing
**Purpose**: Ensure robustness under unusual conditions

**Must Include**:
- Null inputs
- Boundary values
- Concurrent updates
- Invalid states
- Large payloads

---

# 3. TEST PATTERNS

## 3.1 Service Test Template
```java
@ExtendWith(MockitoExtension.class)
class VendorPoServiceTest {

    @Mock
    private VendorPoRepository vendorPoRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private VendorPoServiceImpl vendorPoService;

    @Test
    void createVendorPo_validInput_shouldReturnCreatedPo() {
        // Given
        Long companyId = 1L;
        when(securityContextHelper.getCompanyIdFromContext()).thenReturn(companyId);
        CreateVendorPoDto dto = createValidDto();
        VendorPo expectedPo = createExpectedPo();
        when(vendorPoRepository.save(any(VendorPo.class))).thenReturn(expectedPo);

        // When
        VendorPo result = vendorPoService.createVendorPo(dto);

        // Then
        assertNotNull(result);
        assertEquals(expectedPo.getId(), result.getId());
        verify(vendorPoRepository).save(any(VendorPo.class));
    }

    @Test
    void createVendorPo_emptyItems_shouldThrowException() {
        // Given
        CreateVendorPoDto dto = new CreateVendorPoDto();
        dto.setItems(Collections.emptyList());

        // When/Then
        assertThrows(ValidationException.class, () -> vendorPoService.createVendorPo(dto));
    }
}
```

## 3.2 Controller Test Template
```java
@SpringBootTest
@AutoConfigureMockMvc
class VendorPoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendorPoService vendorPoService;

    @Test
    void createVendorPo_validInput_shouldReturnCreated() throws Exception {
        // Given
        CreateVendorPoDto dto = createValidDto();
        when(vendorPoService.createVendorPo(any())).thenReturn(createExpectedPo());

        // When/Then
        mockMvc.perform(post("/vendor/po")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createVendorPo_invalidInput_shouldReturnBadRequest() throws Exception {
        // Given
        CreateVendorPoDto dto = new CreateVendorPoDto(); // Invalid - missing required fields

        // When/Then
        mockMvc.perform(post("/vendor/po")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }
}
```

## 3.3 Multi-Tenant Test Pattern
**Critical**: All tenant-facing tests must verify cross-tenant access is blocked

```java
@Test
void getOrderById_crossTenantAccess_shouldThrowException() {
    // Given
    Long companyId = 1L;
    Long otherCompanyId = 2L;
    Long orderId = 100L;
    
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(companyId);
    when(orderRepository.findByIdAndCompanyId(orderId, companyId))
        .thenReturn(Optional.empty());

    // When/Then
    assertThrows(OrderNotFoundException.class, 
        () -> orderService.getOrderByIdAndCompanyId(orderId, companyId));
}
```

---

# 4. TEST DOCUMENTATION

## 4.1 Test Case Documentation
Document test cases in `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md`:
- Use descriptive test case IDs (e.g., VPO-01, ORD-05)
- Describe what the test validates
- Include edge cases and business rules

## 4.2 Test Progress Tracking
Track implementation progress in `docs/testing/TEST_PROGRESS.md`:
- Log completed test suites
- Document coverage improvements
- Note any test infrastructure changes

---

# 5. TESTING TOOLS

## 5.1 Core Dependencies
- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **MockMvc** - Spring MVC testing
- **Spring Boot Test** - Integration testing support
- **TestContainers** (optional) - Real database testing

## 5.2 Test Configuration
- Use `@SpringBootTest` for integration tests
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Configure test profiles in `application-test.properties`

---

# 6. SPECIFIC TESTING RULES

## 6.1 Tenant Isolation Tests
**Mandatory** for all new tenant-facing endpoints:
- Test that users can only access their own company's data
- Test that cross-company access attempts fail
- Test that owner sessions cannot access tenant data

## 6.2 Status Transition Tests
For entities with status enums (e.g., OrderStatus, VendorPoStatus):
- Test all valid transitions
- Test invalid transitions are blocked
- Test `isFinal()` status gates
- Test `isActive()` status behavior

## 6.3 Validation Tests
For all DTOs:
- Test `@NotNull`, `@NotBlank`, `@Size` annotations
- Test custom validation annotations
- Test validation error messages

## 6.4 Exception Tests
For all custom exceptions:
- Test exception is thrown when expected
- Test exception carries correct error details
- Test error handling in controllers

## 6.5 PDF Generation Tests
For PDF services:
- Test PDF generation with valid data
- Test PDF generation with edge cases (null values, empty lists)
- Test concurrent PDF generation
- Test GST calculation accuracy
- Test state-based IGST vs CGST/SGST logic

---

# 7. TEST EXECUTION

## 7.1 Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=VendorPoServiceTest

# Run specific test method
mvn test -Dtest=VendorPoServiceTest#createVendorPo_validInput_shouldReturnCreatedPo
```

## 7.2 Coverage Report
```bash
# Generate coverage report
mvn clean test jacoco:report
```

---

# 8. ENFORCEMENT CHECKLIST

Before merge:

- [ ] New/changed controller methods have tests
- [ ] New/changed service methods have tests
- [ ] Tests include happy-path cases
- [ ] Tests include edge/failure cases
- [ ] Tenant isolation verified for tenant-facing code
- [ ] Status transitions tested for status-based entities
- [ ] Validation tested for all DTOs
- [ ] Coverage >90% for affected code
- [ ] Test cases documented in CONTROLLER_SERVICE_TEST_CASES.md
- [ ] Test progress logged in TEST_PROGRESS.md

---

# FINAL RULE

This document is enforceable.

If any guideline is violated:
👉 REJECT PR
