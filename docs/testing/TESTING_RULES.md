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
- Retry-sensitive writes must include database-backed tests for atomic claims, concurrent requests, tenant scoping, and replayed terminal responses.

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

Spreadsheet upload tests should generate in-memory workbooks for required-sheet/header checks, malformed cell and formula handling, row limits, and empty data. Test both multipart metadata limits and actual OOXML parsing so a spoofed filename or MIME type cannot bypass content validation.

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
# Vendor PO lifecycle safety tests

- Test that DRAFT Vendor POs remain editable and every other `VendorPoStatus` is rejected before persistence.
- Test that update lookup is tenant-scoped and a PO absent from the authenticated company's lookup is not saved.
- Test that MVC update reports a service rejection and never calls `changeStatus(..., DRAFT)` as part of editing.

# Company Address management tests

- Verify new addresses are assigned to the authenticated Company and cannot change Company ownership through an edit payload.
- Verify default role changes serialize under a Company lock, active defaults remain unique, and one address may carry both roles.
- Verify duplicate GSTIN values are accepted, inactive addresses cannot become defaults, and defaults must be reassigned before deactivation.
- Verify cross-company reads and mutations fail using Company-scoped address lookups; include default and inactive-address failures.
- Verify migration backfill preserves legacy Company address/GSTIN/state and creates active Bill To and Ship To defaults.
- Verify activation preserves address/GSTIN/Drug License fields and does not assign a default; dual-default deactivation fails until both explicit replacements are active.
- Verify Company Details reads the current active Bill To only, displays its GSTIN with legacy Company GSTIN fallback, and does not expose inactive addresses.
- Verify the additive Drug License migration and form-to-entity persistence; no PO/PDF change is expected in this scope.

## Vendor PO financial and lifecycle coverage
- Cover explicit Included/Excluded and Taxable/Non-taxable charge choices, taxable-rate validation, intra/inter-state split, decimal rounding, and amount-in-words cents.
- Verify cross-tenant ID rejection, snapshot stability after source edits, immutable issued PDF/revision history, and DRAFT-only edits.
