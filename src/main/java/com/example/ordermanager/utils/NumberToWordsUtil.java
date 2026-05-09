package com.example.ordermanager.utils;

public class NumberToWordsUtil {

  private static final String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
      "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
      "Seventeen", "Eighteen", "Nineteen"};

  private static final String[] tens =
      {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

  public static String convertToWords(double amount) {
    long number = (long) amount;

    if (number == 0) {
      return "Rupees Zero Only";
    }

    return "Rupees " + convert(number) + " Only";
  }

  private static String convert(long number) {
    if (number < 20) {
      return units[(int) number];
    }

    if (number < 100) {
      return tens[(int) number / 10] + ((number % 10 != 0) ? " " + units[(int) number % 10] : "");
    }

    if (number < 1000) {
      return units[(int) number / 100] + " Hundred"
          + ((number % 100 != 0) ? " " + convert(number % 100) : "");
    }

    if (number < 100000) {
      return convert(number / 1000) + " Thousand"
          + ((number % 1000 != 0) ? " " + convert(number % 1000) : "");
    }

    if (number < 10000000) {
      return convert(number / 100000) + " Lakh"
          + ((number % 100000 != 0) ? " " + convert(number % 100000) : "");
    }

    return convert(number / 10000000) + " Crore"
        + ((number % 10000000 != 0) ? " " + convert(number % 10000000) : "");
  }
}
