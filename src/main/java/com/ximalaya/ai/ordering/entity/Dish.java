
package com.ximalaya.ai.ordering.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dish")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "spicy_level")
    private Integer spicyLevel = 0;

    @Column(name = "rating")
    private Double rating = 0.0;

    @Column(name = "sales_count")
    private Integer salesCount = 0;

    public Dish() {}

    public Dish(Long id, String name, String description, BigDecimal price, String category,
                String imageUrl, Boolean isAvailable, Integer spicyLevel, Double rating, Integer salesCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable != null ? isAvailable : true;
        this.spicyLevel = spicyLevel != null ? spicyLevel : 0;
        this.rating = rating != null ? rating : 0.0;
        this.salesCount = salesCount != null ? salesCount : 0;
    }

    public static DishBuilder builder() {
        return new DishBuilder();
    }

    public static class DishBuilder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private String imageUrl;
        private Boolean isAvailable = true;
        private Integer spicyLevel = 0;
        private Double rating = 0.0;
        private Integer salesCount = 0;

        public DishBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DishBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DishBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DishBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public DishBuilder category(String category) {
            this.category = category;
            return this;
        }

        public DishBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public DishBuilder isAvailable(Boolean isAvailable) {
            this.isAvailable = isAvailable;
            return this;
        }

        public DishBuilder spicyLevel(Integer spicyLevel) {
            this.spicyLevel = spicyLevel;
            return this;
        }

        public DishBuilder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public DishBuilder salesCount(Integer salesCount) {
            this.salesCount = salesCount;
            return this;
        }

        public Dish build() {
            return new Dish(id, name, description, price, category, imageUrl, isAvailable, spicyLevel, rating, salesCount);
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