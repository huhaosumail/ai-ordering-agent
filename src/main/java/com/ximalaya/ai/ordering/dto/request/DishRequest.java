
package com.ximalaya.ai.ordering.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class DishRequest {

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "菜品描述长度不能超过500")
    private String description;

    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.01", message = "菜品价格必须大于0")
    private BigDecimal price;

    @Size(max = 50, message = "菜品分类长度不能超过50")
    private String category;

    @Size(max = 500, message = "图片URL长度不能超过500")
    private String imageUrl;

    private Boolean isAvailable = true;

    private Integer spicyLevel;

    private Double rating;

    public DishRequest() {}

    public DishRequest(String name, String description, BigDecimal price, String category,
                       String imageUrl, Boolean isAvailable, Integer spicyLevel, Double rating) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable != null ? isAvailable : true;
        this.spicyLevel = spicyLevel;
        this.rating = rating;
    }

    public static DishRequestBuilder builder() {
        return new DishRequestBuilder();
    }

    public static class DishRequestBuilder {
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private String imageUrl;
        private Boolean isAvailable = true;
        private Integer spicyLevel;
        private Double rating;

        public DishRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DishRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DishRequestBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public DishRequestBuilder category(String category) {
            this.category = category;
            return this;
        }

        public DishRequestBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public DishRequestBuilder isAvailable(Boolean isAvailable) {
            this.isAvailable = isAvailable;
            return this;
        }

        public DishRequestBuilder spicyLevel(Integer spicyLevel) {
            this.spicyLevel = spicyLevel;
            return this;
        }

        public DishRequestBuilder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public DishRequest build() {
            return new DishRequest(name, description, price, category, imageUrl, isAvailable, spicyLevel, rating);
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Integer getSpicyLevel() { return spicyLevel; }
    public void setSpicyLevel(Integer spicyLevel) { this.spicyLevel = spicyLevel; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}