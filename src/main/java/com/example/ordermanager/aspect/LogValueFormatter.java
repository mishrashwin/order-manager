package com.example.ordermanager.aspect;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Principal;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
public class LogValueFormatter {

  private static final int MAX_DEPTH = 2;
  private static final int MAX_COLLECTION_ITEMS = 5;
  private static final int MAX_FIELDS = 8;
  private static final int MAX_STRING_LENGTH = 120;
  private static final String MASKED_VALUE = "***";

  public String formatArguments(String[] parameterNames, Object[] args) {
    if (args == null || args.length == 0) {
      return "[]";
    }

    List<String> parts = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      String parameterName = resolveParameterName(parameterNames, i);
      parts.add(parameterName + "=" + formatValue(args[i], parameterName, 0,
          Collections.newSetFromMap(new IdentityHashMap<>())));
    }
    return "[" + String.join(", ", parts) + "]";
  }

  public String formatResult(Object result) {
    return formatValue(result, "result", 0, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  private String resolveParameterName(String[] parameterNames, int index) {
    if (parameterNames != null && index < parameterNames.length && parameterNames[index] != null
        && !parameterNames[index].isBlank()) {
      return parameterNames[index];
    }
    return "arg" + index;
  }

  private String formatValue(Object value, String fieldName, int depth, Set<Object> visited) {
    if (isSensitiveName(fieldName)) {
      return MASKED_VALUE;
    }

    if (value == null) {
      return "null";
    }

    if (value instanceof String stringValue) {
      return '"' + truncate(stringValue) + '"';
    }

    if (value instanceof Authentication authentication) {
      return "Authentication(name=" + authentication.getName() + ")";
    }

    if (value instanceof Principal principal) {
      return "Principal(name=" + principal.getName() + ")";
    }

    if (value instanceof RedirectAttributes) {
      return "RedirectAttributes";
    }

    if (value instanceof Model model) {
      return "Model(keys=" + model.asMap().keySet() + ")";
    }

    if (value instanceof HttpSession session) {
      return "HttpSession(id=" + session.getId() + ")";
    }

    if (value instanceof Errors errors) {
      return "Errors(count=" + errors.getErrorCount() + ")";
    }

    if (value instanceof MultipartFile file) {
      return "MultipartFile(name=" + truncate(file.getOriginalFilename()) + ", size="
          + file.getSize() + ")";
    }

    if (value instanceof ServletRequest || value instanceof ServletResponse) {
      return ClassUtils.getUserClass(value).getSimpleName();
    }

    Class<?> userClass = ClassUtils.getUserClass(value);
    if (isSimpleValueType(userClass, value)) {
      return truncate(String.valueOf(value));
    }

    if (value instanceof Optional<?> optional) {
      return optional
          .map(inner -> "Optional[" + formatValue(inner, fieldName, depth + 1, visited) + "]")
          .orElse("Optional.empty");
    }

    if (userClass.isArray()) {
      return formatArray(value, fieldName, depth, visited);
    }

    if (value instanceof Collection<?> collection) {
      return formatCollection(collection, fieldName, depth, visited);
    }

    if (value instanceof Map<?, ?> map) {
      return formatMap(map, depth, visited);
    }

    if (depth >= MAX_DEPTH) {
      return userClass.getSimpleName();
    }

    if (!visited.add(value)) {
      return userClass.getSimpleName() + "(circular)";
    }

    try {
      return formatObject(value, userClass, depth, visited);
    } finally {
      visited.remove(value);
    }
  }

  private boolean isSimpleValueType(Class<?> type, Object value) {
    return type.isPrimitive() || ClassUtils.isPrimitiveOrWrapper(type) || value instanceof Number
        || value instanceof Boolean || value instanceof Character || value instanceof Enum<?>
        || value instanceof Temporal || value instanceof Date;
  }

  private String formatArray(Object array, String fieldName, int depth, Set<Object> visited) {
    int length = Array.getLength(array);
    List<String> items = new ArrayList<>();
    for (int i = 0; i < Math.min(length, MAX_COLLECTION_ITEMS); i++) {
      items.add(formatValue(Array.get(array, i), fieldName, depth + 1, visited));
    }
    if (length > MAX_COLLECTION_ITEMS) {
      items.add("...");
    }
    return array.getClass().getComponentType().getSimpleName() + "Array(length=" + length
        + ", values=" + items + ")";
  }

  private String formatCollection(Collection<?> collection, String fieldName, int depth,
      Set<Object> visited) {
    List<String> items = new ArrayList<>();
    int index = 0;
    for (Object item : collection) {
      if (index++ == MAX_COLLECTION_ITEMS) {
        items.add("...");
        break;
      }
      items.add(formatValue(item, fieldName, depth + 1, visited));
    }
    return collection.getClass().getSimpleName() + "(size=" + collection.size() + ", values="
        + items + ")";
  }

  private String formatMap(Map<?, ?> map, int depth, Set<Object> visited) {
    List<String> entries = new ArrayList<>();
    int index = 0;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (index++ == MAX_COLLECTION_ITEMS) {
        entries.add("...");
        break;
      }

      String key = truncate(String.valueOf(entry.getKey()));
      entries.add(key + "=" + formatValue(entry.getValue(), key, depth + 1, visited));
    }
    return "Map(size=" + map.size() + ", entries=" + entries + ")";
  }

  private String formatObject(Object value, Class<?> userClass, int depth, Set<Object> visited) {
    List<String> fields = new ArrayList<>();
    int fieldCount = 0;

    for (Field field : getAllFields(userClass)) {
      if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
        continue;
      }

      if (fieldCount++ == MAX_FIELDS) {
        fields.add("...");
        break;
      }

      if (!field.trySetAccessible()) {
        fields.add(field.getName() + "=<inaccessible>");
        continue;
      }
      try {
        fields.add(field.getName() + "="
            + formatValue(field.get(value), field.getName(), depth + 1, visited));
      } catch (IllegalAccessException | RuntimeException ex) {
        fields.add(field.getName() + "=<inaccessible>");
      }
    }

    return userClass.getSimpleName() + "{" + String.join(", ", fields) + "}";
  }

  private List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    Class<?> current = type;
    while (current != null && current != Object.class) {
      Collections.addAll(fields, current.getDeclaredFields());
      current = current.getSuperclass();
    }
    return fields;
  }

  private boolean isSensitiveName(String name) {
    if (name == null) {
      return false;
    }

    String normalizedName = name.toLowerCase();
    return normalizedName.contains("password") || normalizedName.contains("token")
        || normalizedName.contains("secret") || normalizedName.contains("credential")
        || normalizedName.contains("authorization") || normalizedName.contains("api-key")
        || normalizedName.contains("apikey") || normalizedName.contains("resetcode")
        || normalizedName.contains("verificationcode") || normalizedName.equals("code");
  }

  private String truncate(String value) {
    if (value == null) {
      return "null";
    }

    if (value.length() <= MAX_STRING_LENGTH) {
      return value;
    }

    return value.substring(0, MAX_STRING_LENGTH) + "...(len=" + value.length() + ")";
  }
}

