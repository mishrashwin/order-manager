package com.example.ordermanager.utils;

import lombok.Getter;

/**
 * Enum representing Indian states with their names and GST state codes.
 * Used for determining intra-state vs inter-state GST calculations (CGST/SGST vs IGST).
 */
@Getter
public enum IndianState {

  ANDAMAN_AND_NICOBAR_ISLANDS("35", "Andaman and Nicobar Islands"),
  ANDHRA_PRADESH("37", "Andhra Pradesh"),
  ARUNACHAL_PRADESH("12", "Arunachal Pradesh"),
  ASSAM("18", "Assam"),
  BIHAR("10", "Bihar"),
  CHANDIGARH("04", "Chandigarh"),
  CHHATTISGARH("22", "Chhattisgarh"),
  DELHI("07", "Delhi"),
  GOA("30", "Goa"),
  GUJARAT("24", "Gujarat"),
  HARYANA("06", "Haryana"),
  HIMACHAL_PRADESH("02", "Himachal Pradesh"),
  JHARKHAND("20", "Jharkhand"),
  KARNATAKA("29", "Karnataka"),
  KERALA("32", "Kerala"),
  LADAKH("38", "Ladakh"),
  MADHYA_PRADESH("23", "Madhya Pradesh"),
  MAHARASHTRA("27", "Maharashtra"),
  MANIPUR("14", "Manipur"),
  MEGHALAYA("17", "Meghalaya"),
  MIZORAM("15", "Mizoram"),
  NAGALAND("13", "Nagaland"),
  ODISHA("21", "Odisha"),
  PUDUCHERRY("34", "Puducherry"),
  PUNJAB("03", "Punjab"),
  RAJASTHAN("08", "Rajasthan"),
  SIKKIM("11", "Sikkim"),
  TAMIL_NADU("33", "Tamil Nadu"),
  TELANGANA("36", "Telangana"),
  TRIPURA("16", "Tripura"),
  UTTARAKHAND("05", "Uttarakhand"),
  UTTAR_PRADESH("09", "Uttar Pradesh"),
  WEST_BENGAL("19", "West Bengal");

  private final String code;
  private final String displayName;

  IndianState(String code, String displayName) {
    this.code = code;
    this.displayName = displayName;
  }

  /**
   * Find state by code (e.g., "27" for Maharashtra)
   */
  public static IndianState fromCode(String code) {
    if (code == null) {
      return null;
    }
    for (IndianState state : values()) {
      if (state.code.equals(code)) {
        return state;
      }
    }
    return null;
  }

  /**
   * Find state by display name (case-insensitive)
   */
  public static IndianState fromDisplayName(String displayName) {
    if (displayName == null) {
      return null;
    }
    for (IndianState state : values()) {
      if (state.displayName.equalsIgnoreCase(displayName)) {
        return state;
      }
    }
    return null;
  }
}
