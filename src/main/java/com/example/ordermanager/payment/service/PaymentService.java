package com.example.ordermanager.payment.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.payment.entity.Payment;
import com.example.ordermanager.payment.repository.PaymentRepository;
import com.example.ordermanager.user.service.EmailService;
import java.io.IOException;
import java.time.Month;
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
