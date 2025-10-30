package com.example.ordermanager.service;

import com.example.ordermanager.entity.Order;
import com.example.ordermanager.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public Order updateOrder(Long id, Order updatedOrder) {
        return orderRepository.findById(id).map(order -> {
            order.setClientName(updatedOrder.getClientName());
            order.setVendor(updatedOrder.getVendor());
            order.setProduct(updatedOrder.getProduct());
            order.setStatus(updatedOrder.getStatus());
            order.setOrderDate(updatedOrder.getOrderDate());
            order.setDeliveryDate(updatedOrder.getDeliveryDate());
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}
