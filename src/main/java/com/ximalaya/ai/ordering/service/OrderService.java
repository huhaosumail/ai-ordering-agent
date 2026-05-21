
package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByNo(String orderNo);

    List<OrderResponse> getOrdersByUserId(Long userId);

    List<OrderResponse> getOrdersByStatus(String status);

    OrderResponse updateOrderStatus(Long id, String status);

    void cancelOrder(Long id);
}