
package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOrderBySortOrderAsc();

    Category findByName(String name);
}