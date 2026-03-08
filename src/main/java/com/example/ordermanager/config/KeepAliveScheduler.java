package com.example.ordermanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

/**
 * Scheduler to keep the application alive by hitting an endpoint every 10 minutes. Useful for
 * preventing the application from being paused on platforms like Render.
 *
 * Can be controlled via the property: app.keepalive.enabled (default: true)
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.keepalive.enabled", havingValue = "true", matchIfMissing = true)
public class KeepAliveScheduler {

  private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);

  @Value("${APP_BASE_URL:http://localhost:8080}")
  private String appBaseUrl;

  private final RestTemplate restTemplate;

  public KeepAliveScheduler(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Scheduled task that runs every 10 minutes (600000 milliseconds). Hits the application's health
   * endpoint to keep it active.
   */
  @Scheduled(fixedRate = 600000, initialDelay = 60000)
  public void keepApplicationAlive() {
    try {
      String healthUrl = appBaseUrl + "/actuator/health";
      logger.info("🔄 Keep-Alive Cron: Hitting URL: {}", healthUrl);

      String response = restTemplate.getForObject(healthUrl, String.class);

      logger.info("✅ Keep-Alive Cron: Successfully pinged application. Response: {}", response);
    } catch (RestClientException e) {
      logger.warn("⚠️ Keep-Alive Cron: Failed to ping application at {}. Error: {}", appBaseUrl,
          e.getMessage());
    } catch (Exception e) {
      logger.error("❌ Keep-Alive Cron: Unexpected error while keeping application alive", e);
    }
  }
}

