package com.example.ordermanager.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class MethodLoggingAspectTest {

  private ListAppender<ILoggingEvent> logEvents;
  private LoggingTestService proxy;
  private Logger targetLogger;
  private Level originalLevel;
  private boolean originalAdditivity;

  @BeforeEach
  void setUp() {
    MethodLoggingAspect aspect = new MethodLoggingAspect(new LogValueFormatter());
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new LoggingTestService());
    proxyFactory.addAspect(aspect);
    proxy = proxyFactory.getProxy();

    targetLogger = (Logger) LoggerFactory.getLogger(LoggingTestService.class);
    originalLevel = targetLogger.getLevel();
    originalAdditivity = targetLogger.isAdditive();
    targetLogger.setLevel(Level.DEBUG);
    logEvents = new ListAppender<>();
    logEvents.start();
    targetLogger.addAppender(logEvents);
  }

  @AfterEach
  void tearDown() {
    if (targetLogger != null && logEvents != null) {
      targetLogger.detachAppender(logEvents);
      logEvents.stop();
    }
    if (targetLogger != null) {
      targetLogger.setLevel(originalLevel);
      targetLogger.setAdditive(originalAdditivity);
    }
  }

  @Test
  void logsMethodStartAndFinishWithArgumentsAndResult() {
    String result = proxy.greet("Alice");

    assertThat(result).isEqualTo("Hello Alice");

    List<String> messages =
        logEvents.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    assertThat(messages).hasSize(2);
    assertThat(messages.get(0)).contains("LoggingTestService.greet(..) started")
        .contains("input=[name=\"Alice\"]");
    assertThat(messages.get(1)).contains("LoggingTestService.greet(..) finished")
        .contains("result=\"Hello Alice\"");
  }

  @Test
  void masksSensitiveInputValues() {
    String result = proxy.updatePassword("alice", "super-secret");

    assertThat(result).isEqualTo("alice updated (length=12)");

    List<String> messages =
        logEvents.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    assertThat(messages.get(0)).contains("username=\"alice\"").contains("password=***");
  }

  @Test
  void logsFailuresWithMaskedSensitiveInputs() {
    assertThatThrownBy(() -> proxy.fail("temporary-token"))
        .isInstanceOf(IllegalStateException.class).hasMessage("boom");

    assertThat(logEvents.list).hasSize(2);
    ILoggingEvent errorEvent = logEvents.list.get(1);
    assertThat(errorEvent.getLevel()).isEqualTo(Level.ERROR);
    assertThat(errorEvent.getFormattedMessage()).contains("LoggingTestService.fail(..) failed")
        .contains("token=***");
  }

  @Test
  void skipsLoggingForAnnotatedSensitiveMethods() {
    String result = proxy.sendVerificationEmail("alice@example.com",
        "http://localhost:8080/verify?token=secret-token");

    assertThat(result).isEqualTo("email queued");
    assertThat(logEvents.list).isEmpty();
  }

  static class LoggingTestService {

    public String greet(String name) {
      return "Hello " + name;
    }

    public String updatePassword(String username, String password) {
      return username + " updated (length=" + password.length() + ")";
    }

    public void fail(String token) {
      if (token.isBlank()) {
        throw new IllegalStateException("blank");
      }
      throw new IllegalStateException("boom");
    }

    @SkipMethodLogging
    public String sendVerificationEmail(String to, String verificationUrl) {
      return "email queued";
    }
  }
}


