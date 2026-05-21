
package com.ximalaya.ai.ordering.config;

import com.ximalaya.ai.ordering.entity.Category;
import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.repository.CategoryRepository;
import com.ximalaya.ai.ordering.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;

    public DataInitializer(CategoryRepository categoryRepository, DishRepository dishRepository) {
        this.categoryRepository = categoryRepository;
        this.dishRepository = dishRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            log.info("初始化分类数据...");
            initCategories();
        }

        if (dishRepository.count() == 0) {
            log.info("初始化菜品数据...");
            initDishes();
        }
    }

    private void initCategories() {
        List<Category> categories = Arrays.asList(
                Category.builder().name("热销菜品").description("人气爆款").sortOrder(1).build(),
                Category.builder().name("川菜").description("麻辣鲜香").sortOrder(2).build(),
                Category.builder().name("粤菜").description("清淡鲜美").sortOrder(3).build(),
                Category.builder().name("素菜").description("健康营养").sortOrder(4).build(),
                Category.builder().name("汤品").description("滋补养生").sortOrder(5).build(),
                Category.builder().name("主食").description("饱腹首选").sortOrder(6).build()
        );
        categoryRepository.saveAll(categories);
    }

    private void initDishes() {
        List<Dish> dishes = Arrays.asList(
                Dish.builder()
                        .name("麻辣香锅")
                        .description("精选食材，麻辣鲜香，回味无穷")
                        .price(new BigDecimal("88.00"))
                        .category("热销菜品")
                        .spicyLevel(4)
                        .rating(4.8)
                        .salesCount(1256)
                        .build(),
                Dish.builder()
                        .name("水煮牛肉")
                        .description("鲜嫩牛肉，麻辣过瘾")
                        .price(new BigDecimal("68.00"))
                        .category("川菜")
                        .spicyLevel(4)
                        .rating(4.7)
                        .salesCount(892)
                        .build(),
                Dish.builder()
                        .name("宫保鸡丁")
                        .description("鸡肉嫩滑，花生酥脆")
                        .price(new BigDecimal("48.00"))
                        .category("川菜")
                        .spicyLevel(2)
                        .rating(4.6)
                        .salesCount(756)
                        .build(),
                Dish.builder()
                        .name("白灼虾")
                        .description("新鲜大虾，原汁原味")
                        .price(new BigDecimal("128.00"))
                        .category("粤菜")
                        .spicyLevel(0)
                        .rating(4.9)
                        .salesCount(634)
                        .build(),
                Dish.builder()
                        .name("清蒸鲈鱼")
                        .description("鲜嫩爽滑，营养丰富")
                        .price(new BigDecimal("98.00"))
                        .category("粤菜")
                        .spicyLevel(0)
                        .rating(4.8)
                        .salesCount(521)
                        .build(),
                Dish.builder()
                        .name("蒜蓉西兰花")
                        .description("清脆爽口，健康美味")
                        .price(new BigDecimal("28.00"))
                        .category("素菜")
                        .spicyLevel(0)
                        .rating(4.5)
                        .salesCount(445)
                        .build(),
                Dish.builder()
                        .name("麻婆豆腐")
                        .description("麻辣鲜香，下饭神器")
                        .price(new BigDecimal("32.00"))
                        .category("川菜")
                        .spicyLevel(3)
                        .rating(4.7)
                        .salesCount(1102)
                        .build(),
                Dish.builder()
                        .name("佛跳墙")
                        .description("滋补佳品，美味绝伦")
                        .price(new BigDecimal("288.00"))
                        .category("粤菜")
                        .spicyLevel(0)
                        .rating(4.9)
                        .salesCount(234)
                        .build(),
                Dish.builder()
                        .name("番茄鸡蛋汤")
                        .description("营养丰富，老少皆宜")
                        .price(new BigDecimal("22.00"))
                        .category("汤品")
                        .spicyLevel(0)
                        .rating(4.4)
                        .salesCount(876)
                        .build(),
                Dish.builder()
                        .name("担担面")
                        .description("川味经典，麻辣鲜香")
                        .price(new BigDecimal("26.00"))
                        .category("主食")
                        .spicyLevel(3)
                        .rating(4.6)
                        .salesCount(567)
                        .build(),
                Dish.builder()
                        .name("回锅肉")
                        .description("肥而不腻，香气四溢")
                        .price(new BigDecimal("45.00"))
                        .category("川菜")
                        .spicyLevel(2)
                        .rating(4.6)
                        .salesCount(723)
                        .build(),
                Dish.builder()
                        .name("干煸四季豆")
                        .description("外焦里嫩，咸香可口")
                        .price(new BigDecimal("35.00"))
                        .category("川菜")
                        .spicyLevel(2)
                        .rating(4.5)
                        .salesCount(432)
                        .build()
        );
        dishRepository.saveAll(dishes);
    }
}