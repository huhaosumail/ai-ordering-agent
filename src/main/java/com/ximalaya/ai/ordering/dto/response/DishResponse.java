
package com.ximalaya.ai.ordering.dto.response;

import java.math.BigDecimal;

public class DishResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private Boolean isAvailable;
    private Integer spicyLevel;
    private Double rating;
    private Integer salesCount;

    public DishResponse() {}

    public DishResponse(Long id, String name, String description, BigDecimal price, String category,
                       String imageUrl, Boolean isAvailable, Integer spicyLevel, Double rating, Integer salesCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
        this.spicyLevel = spicyLevel;
        this.rating = rating;
        this.salesCount = salesCount;
    }

    public static DishResponseBuilder builder() {
        return new DishResponseBuilder();
    }

    public static class DishResponseBuilder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private String imageUrl;
        private Boolean isAvailable;
        private Integer spicyLevel;
        private Double rating;
        private Integer salesCount;

        public DishResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DishResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DishResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DishResponseBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public DishResponseBuilder category(String category) {
            this.category = category;
            return this;
        }

        public DishResponseBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public DishResponseBuilder isAvailable(Boolean isAvailable) {
            this.isAvailable = isAvailable;
            return this;
        }

        public DishResponseBuilder spicyLevel(Integer spicyLevel) {
            this.spicyLevel = spicyLevel;
            return this;
        }

        public DishResponseBuilder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public DishResponseBuilder salesCount(Integer salesCount) {
            this.salesCount = salesCount;
            return this;
        }

        public DishResponse build() {
            return new DishResponse(id, name, description, price, category, imageUrl, isAvailable, spicyLevel, rating, salesCount);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
}