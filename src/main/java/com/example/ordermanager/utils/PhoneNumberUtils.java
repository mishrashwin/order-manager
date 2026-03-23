package com.example.ordermanager.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public final class PhoneNumberUtils {

  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  private PhoneNumberUtils() {}

  public static String normalizeRequiredInternational(String rawPhone, String fieldName) {
    if (rawPhone == null || rawPhone.trim().isEmpty()) {
      throw new IllegalArgumentException(fieldName + " is required.");
    }
    return normalizeInternational(rawPhone, fieldName);
  }

  public static String normalizeOptionalInternational(String rawPhone, String fieldName) {
    if (rawPhone == null || rawPhone.trim().isEmpty()) {
      return null;
    }
    return normalizeInternational(rawPhone, fieldName);
  }

  private static String normalizeInternational(String rawPhone, String fieldName) {
    String cleaned = rawPhone.trim().replaceAll("[\\s()-]", "");
    String candidate = cleaned.startsWith("+") ? cleaned : "+" + cleaned;

    try {
      PhoneNumber parsed = PHONE_NUMBER_UTIL.parse(candidate, null);
      if (!PHONE_NUMBER_UTIL.isValidNumber(parsed)) {
        throw new IllegalArgumentException(
            fieldName + " is invalid. Please provide valid mobile number.");
      }

      // Persist as digits only to stay compatible with current DB column lengths.
      return PHONE_NUMBER_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164).replace("+",
          "");
    } catch (NumberParseException ex) {
      throw new IllegalArgumentException(
          fieldName + " is invalid. Use country code + mobile number (for example +919876543210).",
          ex);
    }
  }
}
