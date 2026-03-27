package com.example.ordermanager.owner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OwnerDashboardTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "owner", "dashboard.html");

  @Test
  void pendingApprovalsTable_showsAdminContactDetailsInsteadOfUserCount() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("<th>Admin Contact</th>");
    assertThat(template).contains("company.adminFirstName");
    assertThat(template).contains("company.adminLastName");
    assertThat(template).contains("company.adminEmail");
    assertThat(template).contains("company.adminMobileNumber");
    assertThat(template).contains("Admin details not available yet");
    assertThat(template).doesNotContain("<th>Users</th>");
  }
}

