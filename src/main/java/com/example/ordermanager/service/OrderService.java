package com.example.ordermanager.service;

import com.example.ordermanager.entity.Order;
import com.example.ordermanager.exception.OrderNotFoundException;
import com.example.ordermanager.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Order patchOrder(Long id, Order partialOrder) {
        return orderRepository.findById(id)
                .map(existingOrder -> {
                    if (partialOrder.getCustomerName() != null)
                        existingOrder.setCustomerName(partialOrder.getCustomerName());

                    if (partialOrder.getProductName() != null)
                        existingOrder.setProductName(partialOrder.getProductName());

                    if (partialOrder.getQuantity() != null)
                        existingOrder.setQuantity(partialOrder.getQuantity());

                    if (partialOrder.getTotalAmount() != null)
                        existingOrder.setTotalAmount(partialOrder.getTotalAmount());

                    if (partialOrder.getStatus() != null)
                        existingOrder.setStatus(partialOrder.getStatus());

                    if (partialOrder.getOrderDate() != null)
                        existingOrder.setOrderDate(partialOrder.getOrderDate());

                    if (partialOrder.getDeliveryDate() != null)
                        existingOrder.setDeliveryDate(partialOrder.getDeliveryDate());

                    return orderRepository.save(existingOrder);
                })
                .orElseThrow(() -> new OrderNotFoundException(id));
    }


    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
    }

}
