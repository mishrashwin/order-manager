package com.example.ordermanager.payment.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.payment.entity.Payment;
import com.example.ordermanager.payment.repository.PaymentRepository;
import com.example.ordermanager.user.service.EmailService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  /** Number of days before the start of the next month when the payment reminder activates. */
  static final int REMINDER_DAYS_BEFORE_NEXT_MONTH = 5;

  private final PaymentRepository paymentRepository;
  private final CompanyRepository companyRepository;
  private final EmailService emailService;
  private final String ownerEmail;

  public PaymentService(PaymentRepository paymentRepository, CompanyRepository companyRepository,
      EmailService emailService, @Value("${app.owner.email}") String ownerEmail) {
    this.paymentRepository = paymentRepository;
    this.companyRepository = companyRepository;
    this.emailService = emailService;
    this.ownerEmail = ownerEmail;
  }

  @Transactional
  public Payment addPayment(Long companyId, int paymentMonth, int paymentYear, String paymentMsg,
      MultipartFile screenshot) throws IOException {
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company not found"));

    Payment payment = new Payment();
    payment.setCompany(company);
    payment.setPaymentMonth(paymentMonth);
    payment.setPaymentYear(paymentYear);
    payment.setPaymentMsg(paymentMsg);

    if (screenshot != null && !screenshot.isEmpty()) {
      payment.setPaymentSsFilename(screenshot.getOriginalFilename());
      payment.setPaymentSsData(screenshot.getBytes());
      payment.setPaymentSsContentType(screenshot.getContentType());
    }

    Payment savedPayment = paymentRepository.save(payment);

    notifyOwnerOfNewPayment(savedPayment, company);

    return savedPayment;
  }

  public List<Payment> getPaymentsByCompany(Long companyId) {
    return paymentRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
  }

  public List<Payment> getAllPayments() {
    return paymentRepository.findAllByOrderByCreatedAtDesc();
  }

  public Payment getPaymentByIdAndCompany(Long paymentId, Long companyId) {
    return paymentRepository.findByIdAndCompanyId(paymentId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found or access denied"));
  }

  public Payment getPaymentById(Long paymentId) {
    return paymentRepository.findById(paymentId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
  }

  public String getMonthYearLabel(int month, int year) {
    return Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
        + " " + year;
  }

  /**
   * Determines whether the payment reminder ticker tape should be shown on the admin dashboard. The
   * reminder activates during the last 5 days of the current month (i.e., when the upcoming month
   * is within 5 days) and stays active until payment for the upcoming month is submitted.
   *
   * @param companyId the tenant company ID
   * @return a {@link PaymentReminderInfo} record with display details, or one where {@code active}
   *         is {@code false} if no reminder is needed
   */
  public PaymentReminderInfo getPaymentReminderInfo(Long companyId) {
    LocalDate today = LocalDate.now();
    LocalDate nextMonthStart = today.withDayOfMonth(1).plusMonths(1);
    long daysUntilNextMonth = ChronoUnit.DAYS.between(today, nextMonthStart);

    if (daysUntilNextMonth > REMINDER_DAYS_BEFORE_NEXT_MONTH) {
      return new PaymentReminderInfo(false, 0, 0, null, null);
    }

    int targetMonth = nextMonthStart.getMonthValue();
    int targetYear = nextMonthStart.getYear();
    boolean paymentAlreadyMade = paymentRepository
        .existsByCompanyIdAndPaymentMonthAndPaymentYear(companyId, targetMonth, targetYear);

    if (paymentAlreadyMade) {
      return new PaymentReminderInfo(false, targetMonth, targetYear, null, null);
    }

    Company company = companyRepository.findById(companyId).orElse(null);
    BigDecimal fee = company != null ? company.getMonthlyFee() : null;
    String monthYearLabel = getMonthYearLabel(targetMonth, targetYear);
    return new PaymentReminderInfo(true, targetMonth, targetYear, monthYearLabel, fee);
  }

  /**
   * Holds the result of a payment reminder check for the admin dashboard ticker tape.
   *
   * @param active whether the ticker should be displayed
   * @param targetMonth the month number for which payment is due
   * @param targetYear the year for which payment is due
   * @param monthYearLabel human-readable month+year string (e.g. "April 2026")
   * @param monthlyFee the configured subscription fee for the company (may be {@code null})
   */
  public record PaymentReminderInfo(boolean active, int targetMonth, int targetYear,
      String monthYearLabel, BigDecimal monthlyFee) {}

  private void notifyOwnerOfNewPayment(Payment payment, Company company) {
    if (ownerEmail == null || ownerEmail.isBlank()) {
      log.warn("Owner email not configured; skipping payment notification for company: {}",
          company.getName());
      return;
    }
    try {
      emailService.sendPaymentNotificationEmail(ownerEmail, payment, company);
    } catch (Exception ex) {
      log.warn("Payment saved but owner notification email failed for company: {}",
          company.getName(), ex);
    }
  }
}
