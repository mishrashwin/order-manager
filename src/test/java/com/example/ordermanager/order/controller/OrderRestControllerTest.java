package com.example.ordermanager.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.exception.OrderNotFoundException;
import com.example.ordermanager.order.service.OrderActivityService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrderRestControllerTest {

  @Mock
  private OrderService orderService;
  @Mock
  private OrderActivityService orderActivityService;
  @Mock
  private SecurityContextHelper securityContextHelper;

  private OrderRestController orderRestController;

  @BeforeEach
  void setUp() {
    orderRestController =
        new OrderRestController(orderService, orderActivityService, securityContextHelper);
  }

  @Test
  void patchOrder_whenStatusChanges_returnsStatusPayloadAndLogsAudit() {
    Order oldOrder = new Order();
    oldOrder.setId(7L);
    oldOrder.setStatus(OrderStatus.CREATED);

    Order partialOrder = new Order();
    partialOrder.setStatus(OrderStatus.DISPATCHED);

    Order savedOrder = new Order();
    savedOrder.setId(7L);
    savedOrder.setStatus(OrderStatus.DISPATCHED);

    User actor = new User();
    actor.setUsername("admin1");
    actor.setFirstName("Ava");
    actor.setLastName("Shah");

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(7L, 5L)).thenReturn(oldOrder);
    when(orderService.patchOrderForCompany(7L, partialOrder, 5L)).thenReturn(savedOrder);
    when(securityContextHelper.getUserFromContext()).thenReturn(actor);

    ResponseEntity<Map<String, String>> response = orderRestController.patchOrder(7L, partialOrder);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).containsEntry("status", "DISPATCHED")
        .containsEntry("displayName", "Dispatched").containsEntry("badgeColor", "primary");
    verify(orderActivityService).logStatusChanged(savedOrder, "Created", "Dispatched", "admin1",
        "Ava Shah", 5L);
  }

  @Test
  void patchOrder_whenOrderIsOutsideTenantScope_throwsOrderNotFound() {
    Order partialOrder = new Order();
    partialOrder.setStatus(OrderStatus.DELIVERED);

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(9L, 5L)).thenReturn(null);
    when(orderService.patchOrderForCompany(9L, partialOrder, 5L))
        .thenThrow(new OrderNotFoundException(9L));

    assertThatThrownBy(() -> orderRestController.patchOrder(9L, partialOrder))
        .isInstanceOf(OrderNotFoundException.class);
  }
}

