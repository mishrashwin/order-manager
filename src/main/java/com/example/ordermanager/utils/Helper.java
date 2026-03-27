package com.example.ordermanager.utils;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class Helper {

  public String toTitleCase(String name) {
    if (name == null || name.isBlank()) {
      return name == null ? "" : name;
    }
    return Arrays.stream(name.trim().split("\\s+")).filter(w -> !w.isEmpty())
        .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
        .collect(Collectors.joining(" "));
  }
}
