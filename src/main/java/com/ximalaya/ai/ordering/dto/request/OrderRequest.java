
package com.ximalaya.ai.ordering.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OrderRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private String tableNo;

    @NotEmpty(message = "订单项不能为空")
    private List<OrderItem> items;

    private String remark;

    public OrderRequest() {}

    public OrderRequest(Long userId, String tableNo, List<OrderItem> items, String remark) {
        this.userId = userId;
        this.tableNo = tableNo;
        this.items = items;
        this.remark = remark;
    }

    public static OrderRequestBuilder builder() {
        return new OrderRequestBuilder();
    }

    public static class OrderRequestBuilder {
        private Long userId;
        private String tableNo;
        private List<OrderItem> items;
        private String remark;

        public OrderRequestBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public OrderRequestBuilder tableNo(String tableNo) {
            this.tableNo = tableNo;
            return this;
        }

        public OrderRequestBuilder items(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        public OrderRequestBuilder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public OrderRequest build() {
            return new OrderRequest(userId, tableNo, items, remark);
        }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTableNo() { return tableNo; }
    public void setTableNo(String tableNo) { this.tableNo = tableNo; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static class OrderItem {
        @NotNull(message = "菜品ID不能为空")
        private Long dishId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;

        public OrderItem() {}

        public OrderItem(Long dishId, Integer quantity) {
            this.dishId = dishId;
            this.quantity = quantity;
        }

        public static OrderItemBuilder builder() {
            return new OrderItemBuilder();
        }

        public static class OrderItemBuilder {
            private Long dishId;
            private Integer quantity;

            public OrderItemBuilder dishId(Long dishId) {
                this.dishId = dishId;
                return this;
            }

            public OrderItemBuilder quantity(Integer quantity) {
                this.quantity = quantity;
                return this;
            }

            public OrderItem build() {
                return new OrderItem(dishId, quantity);
            }
        }

        public Long getDishId() { return dishId; }
        public void setDishId(Long dishId) { this.dishId = dishId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}