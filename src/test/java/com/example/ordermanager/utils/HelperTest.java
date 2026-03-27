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
}

