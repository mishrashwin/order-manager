package com.example.ordermanager.company.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.client.repository.ClientRepository;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.order.repository.OrderRepository;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.example.ordermanager.client.service.ClientService;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.vendor.service.VendorService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.entity.VerificationToken;
import com.example.ordermanager.user.repository.UserRepository;
import com.example.ordermanager.user.repository.VerificationTokenRepository;
import com.example.ordermanager.user.service.EmailService;
import com.example.ordermanager.user.service.RegistrationService;
import com.example.ordermanager.user.service.UserService;
import com.example.ordermanager.utils.Helper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanyLifecycleWorkflowTest {

  @Mock
  private CompanyRepository companyRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private VerificationTokenRepository verificationTokenRepository;
  @Mock
  private EmailService emailService;
  @Mock
  private ClientRepository clientRepository;
  @Mock
  private VendorRepository vendorRepository;
  @Mock
  private OrderRepository orderRepository;

  private RegistrationService registrationService;
  private UserService userService;
  private CompanyService companyService;
  private ClientService clientService;
  private VendorService vendorService;
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    registrationService = new RegistrationService(userRepository, verificationTokenRepository,
        emailService, companyRepository);
    ReflectionTestUtils.setField(registrationService, "baseUrl", "http://localhost:8080");

    userService = new UserService(userRepository, registrationService, companyRepository);
    companyService = new CompanyService(companyRepository, userService, emailService,
        "owner@example.com", "http://localhost:8080");

    Helper helper = new Helper();
    clientService = new ClientService(clientRepository, orderRepository, companyService, helper);
    vendorService = new VendorService(vendorRepository, companyService, helper);
    orderService = new OrderService(orderRepository, companyService, helper);
  }

  @Test
  void automatesCompanyToOrderLifecycleWorkflow() {
    AtomicReference<Company> savedCompanyRef = new AtomicReference<>();
    AtomicReference<User> savedAdminRef = new AtomicReference<>();
    AtomicReference<VerificationToken> verificationTokenRef = new AtomicReference<>();

    Company company = new Company();
    company.setName("ALPHA TRADERS");

    User admin = new User();
    admin.setFirstName("Alice");
    admin.setLastName("Admin");
    admin.setUsername("adminalpha");
    admin.setEmail("admin@alpha.com");
    admin.setMobileNumber("+91 99999 99999");
    admin.setPassword("Pass@123");

    when(companyRepository.findByName("ALPHA TRADERS")).thenReturn(Optional.empty());
    when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
      Company toSave = invocation.getArgument(0);
      if (toSave.getId() == null) {
        toSave.setId(10L);
      }
      savedCompanyRef.set(toSave);
      return toSave;
    });
    when(companyRepository.findById(10L))
        .thenAnswer(invocation -> Optional.ofNullable(savedCompanyRef.get()));

    when(userRepository.findByUsername("adminalpha")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("admin@alpha.com")).thenReturn(Optional.empty());
    when(userRepository.findByMobileNumber("919999999999")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("manager1")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("manager1@alpha.com")).thenReturn(Optional.empty());
    when(userRepository.findByMobileNumber("918888888888")).thenReturn(Optional.empty());

    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User toSave = invocation.getArgument(0);
      if (toSave.getId() == null) {
        if ("adminalpha".equals(toSave.getUsername())) {
          toSave.setId(100L);
          savedAdminRef.set(toSave);
        } else {
          toSave.setId(101L);
        }
      }
      return toSave;
    });

    when(verificationTokenRepository.findByUser(any(User.class))).thenReturn(Optional.empty());
    when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> {
      VerificationToken token = invocation.getArgument(0);
      verificationTokenRef.set(token);
      return token;
    });

    doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());
    doNothing().when(emailService).sendNewCompanyRegistrationNotification(anyString(), any(), any(),
        anyString());
    doNothing().when(emailService).sendCompanyApprovedWelcomeEmail(anyString(), any(), any(),
        anyString());

    Company savedCompany = companyService.registerCompanyWithAdmin(company, admin);

    assertThat(savedCompany.getId()).isEqualTo(10L);
    assertThat(savedCompany.getApprovalStatus()).isEqualTo(CompanyApprovalStatus.PENDING);
    assertThat(savedCompany.isActive()).isTrue();
    assertThat(savedAdminRef.get().isEnabled()).isFalse();
    assertThat(savedAdminRef.get().getRole()).isEqualTo("ADMIN");
    assertThat(savedAdminRef.get().getPassword()).isNotEqualTo("Pass@123");

    VerificationToken token = verificationTokenRef.get();
    assertThat(token).isNotNull();
    when(verificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

    boolean verified = registrationService.verifyToken(token.getToken());
    assertThat(verified).isTrue();
    assertThat(savedAdminRef.get().isEnabled()).isTrue();
    verify(verificationTokenRepository).delete(token);

    when(userRepository.findFirstByCompanyIdAndRoleOrderByIdAsc(10L, "ADMIN"))
        .thenReturn(Optional.of(savedAdminRef.get()));

    Company approvedCompany = companyService.approveCompany(10L, "owner.user");
    assertThat(approvedCompany.getApprovalStatus()).isEqualTo(CompanyApprovalStatus.APPROVED);
    assertThat(approvedCompany.getApprovedBy()).isEqualTo("owner.user");
    assertThat(companyService.canUsersLogin(approvedCompany)).isTrue();

    User manager = new User();
    manager.setFirstName("Mark");
    manager.setLastName("Manager");
    manager.setUsername("manager1");
    manager.setEmail("manager1@alpha.com");
    manager.setMobileNumber("+91 88888 88888");
    manager.setPassword("Pass@456");
    manager.setRole("MANAGER");

    User createdManager = userService.createUserByAdmin(manager, 10L);
    assertThat(createdManager.getId()).isEqualTo(101L);
    assertThat(createdManager.getCompany().getId()).isEqualTo(10L);
    assertThat(createdManager.isEnabled()).isFalse();

    Client client = new Client();
    client.setName("acme retail");
    client.setContactPerson("john DOE");
    client.setEmail("client@acme.com");

    when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
      Client toSave = invocation.getArgument(0);
      if (toSave.getId() == null) {
        toSave.setId(501L);
      }
      return toSave;
    });

    clientService.saveClientWithCompany(client, 10L);
    assertThat(client.getName()).isEqualTo("ACME RETAIL");
    assertThat(client.getContactPerson()).isEqualTo("John Doe");
    assertThat(client.getCompany().getId()).isEqualTo(10L);

    Vendor vendor = new Vendor();
    vendor.setCompanyName("beta supply");
    vendor.setContactPerson("jane SMITH");

    when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
      Vendor toSave = invocation.getArgument(0);
      if (toSave.getId() == null) {
        toSave.setId(601L);
      }
      return toSave;
    });

    Vendor savedVendor = vendorService.saveVendorWithCompany(vendor, 10L);
    assertThat(savedVendor.getCompanyName()).isEqualTo("BETA SUPPLY");
    assertThat(savedVendor.getContactPerson()).isEqualTo("Jane Smith");
    assertThat(savedVendor.getCompany().getId()).isEqualTo(10L);

    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
      Order toSave = invocation.getArgument(0);
      if (toSave.getId() == null) {
        toSave.setId(900L);
      }
      return toSave;
    });

    Order order = new Order();
    order.setClient(client);
    order.setProductName("steel rods");
    order.setQuantity(10);
    order.setTotalAmount(4200.0);
    order.setStatus(OrderStatus.CREATED);
    order.setOrderDate(LocalDateTime.now().toLocalDate());

    Order createdOrder = orderService.createOrderWithCompany(order, 10L);
    assertThat(createdOrder.getId()).isEqualTo(900L);
    assertThat(createdOrder.getCompany().getId()).isEqualTo(10L);
    assertThat(createdOrder.getCustomerName()).isEqualTo("ACME RETAIL");
    assertThat(createdOrder.getProductName()).isEqualTo("Steel Rods");

    when(orderRepository.findById(900L)).thenReturn(Optional.of(createdOrder));

    Order patch = new Order();
    patch.setProductName("premium steel rods");
    patch.setStatus(OrderStatus.DISPATCHED);
    patch.setOrderNote("priority dispatch");

    Order updatedOrder = orderService.patchOrder(900L, patch);
    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.DISPATCHED);
    assertThat(updatedOrder.getProductName()).isEqualTo("Premium Steel Rods");
    assertThat(updatedOrder.getOrderNote()).isEqualTo("priority dispatch");
  }
}

