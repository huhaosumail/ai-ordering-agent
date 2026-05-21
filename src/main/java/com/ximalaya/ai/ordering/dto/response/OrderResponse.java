
package com.ximalaya.ai.ordering.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String orderNo;
    private Long userId;
    private String tableNo;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponse() {}

    public OrderResponse(Long id, String orderNo, Long userId, String tableNo, String status,
                        BigDecimal totalAmount, List<OrderItemResponse> items, String remark,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.userId = userId;
        this.tableNo = tableNo;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
        this.remark = remark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderResponseBuilder builder() {
        return new OrderResponseBuilder();
    }

    public static class OrderResponseBuilder {
        private Long id;
        private String orderNo;
        private Long userId;
        private String tableNo;
        private String status;
        private BigDecimal totalAmount;
        private List<OrderItemResponse> items;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrderResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderResponseBuilder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        public OrderResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public OrderResponseBuilder tableNo(String tableNo) {
            this.tableNo = tableNo;
            return this;
        }

        public OrderResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OrderResponseBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public OrderResponseBuilder items(List<OrderItemResponse> items) {
            this.items = items;
            return this;
        }

        public OrderResponseBuilder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public OrderResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public OrderResponse build() {
            return new OrderResponse(id, orderNo, userId, tableNo, status, totalAmount, items, remark, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTableNo() { return tableNo; }
    public void setTableNo(String tableNo) { this.tableNo = tableNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class OrderItemResponse {
        private Long dishId;
        private String dishName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;

        public OrderItemResponse() {}

        public OrderItemResponse(Long dishId, String dishName, BigDecimal price, Integer quantity, BigDecimal subtotal) {
            this.dishId = dishId;
            this.dishName = dishName;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public static OrderItemResponseBuilder builder() {
            return new OrderItemResponseBuilder();
        }

        public static class OrderItemResponseBuilder {
            private Long dishId;
            private String dishName;
            private BigDecimal price;
            private Integer quantity;
            private BigDecimal subtotal;

            public OrderItemResponseBuilder dishId(Long dishId) {
                this.dishId = dishId;
                return this;
            }

            public OrderItemResponseBuilder dishName(String dishName) {
                this.dishName = dishName;
                return this;
            }

            public OrderItemResponseBuilder price(BigDecimal price) {
                this.price = price;
                return this;
            }

            public OrderItemResponseBuilder quantity(Integer quantity) {
                this.quantity = quantity;
                return this;
            }

            public OrderItemResponseBuilder subtotal(BigDecimal subtotal) {
                this.subtotal = subtotal;
                return this;
            }

            public OrderItemResponse build() {
                return new OrderItemResponse(dishId, dishName, price, quantity, subtotal);
            }
        }

        public Long getDishId() { return dishId; }
        public void setDishId(Long dishId) { this.dishId = dishId; }

        public String getDishName() { return dishName; }
        public void setDishName(String dishName) { this.dishName = dishName; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
}