package com.example.ordermanager.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HelperTest {

  private Helper helper;

  @BeforeEach
  void setUp() {
    helper = new Helper();
  }

  // --- happy paths ---

  @Test
  void toTitleCase_singleWord_capitaliseFirstLetter() {
    assertThat(helper.toTitleCase("hello")).isEqualTo("Hello");
  }

  @Test
  void toTitleCase_multipleWords_capitaliseEachWord() {
    assertThat(helper.toTitleCase("john doe")).isEqualTo("John Doe");
  }

  @Test
  void toTitleCase_mixedCase_normalisesCorrectly() {
    assertThat(helper.toTitleCase("jOhN dOe")).isEqualTo("John Doe");
  }

  @Test
  void toTitleCase_extraWhitespace_collapsesSpaces() {
    assertThat(helper.toTitleCase("  steel   rods  ")).isEqualTo("Steel Rods");
  }

  // --- edge / failure paths ---

  @Test
  void toTitleCase_emptyString_returnsEmptyStringWithoutException() {
    assertThat(helper.toTitleCase("")).isEqualTo("");
  }

  @Test
  void toTitleCase_blankString_returnsOriginalWithoutException() {
    assertThat(helper.toTitleCase("   ")).isEqualTo("   ");
  }

  @Test
  void toTitleCase_nullInput_returnsEmptyStringWithoutException() {
    assertThat(helper.toTitleCase(null)).isEqualTo("");
  }

  // --- extractInitials tests ---

  @Test
  void extractInitials_singleWord_returnsFirstLetter() {
    assertThat(helper.extractInitials("Test")).isEqualTo("T");
  }

  @Test
  void extractInitials_multipleWords_returnsFirstLetterOfEachWord() {
    assertThat(helper.extractInitials("Test Company")).isEqualTo("TC");
  }

  @Test
  void extractInitials_threeWords_returnsFirstLetterOfEachWord() {
    assertThat(helper.extractInitials("Acme Manufacturing Company")).isEqualTo("AMC");
  }

  @Test
  void extractInitials_extraWhitespace_stillExtractsCorrectly() {
    assertThat(helper.extractInitials("  Test   Company  ")).isEqualTo("TC");
  }

  @Test
  void extractInitials_emptyString_returnsEmptyString() {
    assertThat(helper.extractInitials("")).isEqualTo("");
  }

  @Test
  void extractInitials_blankString_returnsEmptyString() {
    assertThat(helper.extractInitials("   ")).isEqualTo("");
  }

  @Test
  void extractInitials_nullInput_returnsEmptyString() {
    assertThat(helper.extractInitials(null)).isEqualTo("");
  }
}

