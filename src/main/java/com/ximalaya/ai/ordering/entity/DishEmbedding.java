package com.ximalaya.ai.ordering.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("dish_embedding")
public class DishEmbedding implements Persistable<Long> {

    @Id
    @Column("dish_id")
    private Long dishId;

    @Transient
    private boolean newRow = true;

    @Column("content_text")
    private String contentText;

    @Column("content_hash")
    private String contentHash;

    @Column("embedding_json")
    private String embeddingJson;

    @Column("dimension")
    private Integer dimension;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return dishId;
    }

    @Override
    public boolean isNew() {
        return newRow;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public void markNotNew() {
        this.newRow = false;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
