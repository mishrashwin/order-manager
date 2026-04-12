package com.example.ordermanager.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndianStateTest {

  @Test
  void fromCode_validCode_shouldReturnCorrectState() {
    IndianState maharashtra = IndianState.fromCode("27");
    assertEquals(IndianState.MAHARASHTRA, maharashtra);
    assertEquals("27", maharashtra.getCode());
    assertEquals("Maharashtra", maharashtra.getDisplayName());
  }

  @Test
  void fromCode_invalidCode_shouldReturnNull() {
    IndianState state = IndianState.fromCode("99");
    assertNull(state);
  }

  @Test
  void fromCode_nullCode_shouldReturnNull() {
    IndianState state = IndianState.fromCode(null);
    assertNull(state);
  }

  @Test
  void fromDisplayName_validName_shouldReturnCorrectState() {
    IndianState maharashtra = IndianState.fromDisplayName("Maharashtra");
    assertEquals(IndianState.MAHARASHTRA, maharashtra);

    IndianState karnataka = IndianState.fromDisplayName("Karnataka");
    assertEquals(IndianState.KARNATAKA, karnataka);
  }

  @Test
  void fromDisplayName_caseInsensitive_shouldReturnCorrectState() {
    IndianState state1 = IndianState.fromDisplayName("maharashtra");
    assertEquals(IndianState.MAHARASHTRA, state1);

    IndianState state2 = IndianState.fromDisplayName("MAHARASHTRA");
    assertEquals(IndianState.MAHARASHTRA, state2);

    IndianState state3 = IndianState.fromDisplayName("Maharashtra");
    assertEquals(IndianState.MAHARASHTRA, state3);
  }

  @Test
  void fromDisplayName_invalidName_shouldReturnNull() {
    IndianState state = IndianState.fromDisplayName("Invalid State");
    assertNull(state);
  }

  @Test
  void fromDisplayName_nullName_shouldReturnNull() {
    IndianState state = IndianState.fromDisplayName(null);
    assertNull(state);
  }

  @Test
  void allStates_shouldHaveUniqueCodes() {
    int size = IndianState.values().length;
    assertEquals(37, size, "Should have 37 Indian states/union territories");
  }

  @Test
  void maharashtra_shouldHaveCorrectCode() {
    assertEquals("27", IndianState.MAHARASHTRA.getCode());
    assertEquals("Maharashtra", IndianState.MAHARASHTRA.getDisplayName());
  }

  @Test
  void gujarat_shouldHaveCorrectCode() {
    assertEquals("24", IndianState.GUJARAT.getCode());
    assertEquals("Gujarat", IndianState.GUJARAT.getDisplayName());
  }

  @Test
  void karnataka_shouldHaveCorrectCode() {
    assertEquals("29", IndianState.KARNATAKA.getCode());
    assertEquals("Karnataka", IndianState.KARNATAKA.getDisplayName());
  }

  @Test
  void delhi_shouldHaveCorrectCode() {
    assertEquals("07", IndianState.DELHI.getCode());
    assertEquals("Delhi", IndianState.DELHI.getDisplayName());
  }
}
