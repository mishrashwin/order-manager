package com.example.ordermanager.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.payment.repository.PaymentRepository;
import com.example.ordermanager.payment.service.PaymentService.PaymentReminderInfo;
import com.example.ordermanager.user.service.EmailService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PaymentService#getPaymentReminderInfo(Long)}.
 */
@ExtendWith(MockitoExtension.class)
class PaymentReminderInfoTest {

  @Mock
  private PaymentRepository paymentRepository;
  @Mock
  private CompanyRepository companyRepository;
  @Mock
  private EmailService emailService;

  private PaymentService paymentService;

  @BeforeEach
  void setUp() {
    paymentService =
        new PaymentService(paymentRepository, companyRepository, emailService, "owner@test.com");
  }

  @Test
  void reminderActive_whenWithin5DaysOfNextMonthAndNoPayment() {
    // Simulate a date that is 3 days before the next month (within the 5-day window)
    LocalDate lastDayOfMonth = LocalDate.of(2026, 3, 29);
    int nextMonth = 4;
    int nextYear = 2026;

    Company company = new Company();
    company.setId(1L);
    company.setMonthlyFee(999.0);

    try (MockedStatic<LocalDate> mockedDate =
        Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
      mockedDate.when(LocalDate::now).thenReturn(lastDayOfMonth);
      when(
          paymentRepository.existsByCompanyIdAndPaymentMonthAndPaymentYear(1L, nextMonth, nextYear))
          .thenReturn(false);
      when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

      PaymentReminderInfo info = paymentService.getPaymentReminderInfo(1L);

      assertThat(info.active()).isTrue();
      assertThat(info.targetMonth()).isEqualTo(nextMonth);
      assertThat(info.targetYear()).isEqualTo(nextYear);
      assertThat(info.monthYearLabel()).isEqualTo("April 2026");
      assertThat(info.monthlyFee()).isEqualTo(999.0);
    }
  }

  @Test
  void reminderInactive_whenOutside5DayWindow() {
    // Day 20 of March → 9 days until April 1 → outside window
    LocalDate earlyDate = LocalDate.of(2026, 3, 20);

    try (MockedStatic<LocalDate> mockedDate =
        Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
      mockedDate.when(LocalDate::now).thenReturn(earlyDate);

      PaymentReminderInfo info = paymentService.getPaymentReminderInfo(1L);

      assertThat(info.active()).isFalse();
    }
  }

  @Test
  void reminderInactive_whenPaymentAlreadySubmittedForNextMonth() {
    LocalDate lastDayOfMonth = LocalDate.of(2026, 3, 31);
    int nextMonth = 4;
    int nextYear = 2026;

    try (MockedStatic<LocalDate> mockedDate =
        Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
      mockedDate.when(LocalDate::now).thenReturn(lastDayOfMonth);
      when(
          paymentRepository.existsByCompanyIdAndPaymentMonthAndPaymentYear(1L, nextMonth, nextYear))
          .thenReturn(true);

      PaymentReminderInfo info = paymentService.getPaymentReminderInfo(1L);

      assertThat(info.active()).isFalse();
      assertThat(info.targetMonth()).isEqualTo(nextMonth);
    }
  }

  @Test
  void reminderActive_withNullFee_whenCompanyHasNoFeeConfigured() {
    LocalDate lastDayOfMonth = LocalDate.of(2026, 3, 30);
    int nextMonth = 4;
    int nextYear = 2026;

    Company company = new Company();
    company.setId(2L);
    company.setMonthlyFee(null);

    try (MockedStatic<LocalDate> mockedDate =
        Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
      mockedDate.when(LocalDate::now).thenReturn(lastDayOfMonth);
      when(
          paymentRepository.existsByCompanyIdAndPaymentMonthAndPaymentYear(2L, nextMonth, nextYear))
          .thenReturn(false);
      when(companyRepository.findById(2L)).thenReturn(Optional.of(company));

      PaymentReminderInfo info = paymentService.getPaymentReminderInfo(2L);

      assertThat(info.active()).isTrue();
      assertThat(info.monthlyFee()).isNull();
    }
  }
}
