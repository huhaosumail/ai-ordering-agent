package com.ximalaya.ai.ordering.config;

import com.ximalaya.ai.ordering.entity.Category;
import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.repository.CategoryRepository;
import com.ximalaya.ai.ordering.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        log.info("Loading sample categories and dishes...");
        categoryRepository.deleteAll()
                .thenMany(Flux.just(
                        Category.builder().name("中式菜肴").description("传统中式菜品").sortOrder(1).createdAt(LocalDateTime.now()).build(),
                        Category.builder().name("西式料理").description("欧美风味菜品").sortOrder(2).createdAt(LocalDateTime.now()).build(),
                        Category.builder().name("甜点饮品").description("甜点和饮品").sortOrder(3).createdAt(LocalDateTime.now()).build()
                ).flatMap(categoryRepository::save))
                .then(dishRepository.deleteAll())
                .thenMany(Flux.just(
                        Dish.builder().name("宫保鸡丁").description("经典川菜，鸡肉鲜嫩，花生酥脆").price(new BigDecimal("38.00")).category("中式菜肴").imageUrl("https://example.com/gongbao.jpg").isAvailable(true).salesCount(1250).rating(new BigDecimal("4.8")).ratingCount(320).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        Dish.builder().name("鱼香肉丝").description("酸甜微辣，口感丰富").price(new BigDecimal("32.00")).category("中式菜肴").imageUrl("https://example.com/yuxiang.jpg").isAvailable(true).salesCount(980).rating(new BigDecimal("4.7")).ratingCount(285).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        Dish.builder().name("麻婆豆腐").description("麻辣鲜香，下饭神器").price(new BigDecimal("28.00")).category("中式菜肴").imageUrl("https://example.com/mapo.jpg").isAvailable(true).salesCount(1100).rating(new BigDecimal("4.9")).ratingCount(310).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        Dish.builder().name("糖醋里脊").description("酸甜适口，外酥里嫩").price(new BigDecimal("42.00")).category("中式菜肴").imageUrl("https://example.com/tangcu.jpg").isAvailable(true).salesCount(850).rating(new BigDecimal("4.6")).ratingCount(240).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        Dish.builder().name("黑椒牛柳").description("鲜嫩多汁，黑椒浓郁").price(new BigDecimal("48.00")).category("西式料理").imageUrl("https://example.com/beef.jpg").isAvailable(true).salesCount(720).rating(new BigDecimal("4.8")).ratingCount(195).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        Dish.builder().name("意大利面").description("经典意式风味").price(new BigDecimal("36.00")).category("西式料理").imageUrl("https://example.com/pasta.jpg").isAvailable(true).salesCount(680).rating(new BigDecimal("4.5")).ratingCount(180).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        Dish.builder().name("提拉米苏").description("意大利经典甜点").price(new BigDecimal("28.00")).category("甜点饮品").imageUrl("https://example.com/tiramisu.jpg").isAvailable(true).salesCount(520).rating(new BigDecimal("4.9")).ratingCount(155).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        Dish.builder().name("芒果布丁").description("清爽可口，果香浓郁").price(new BigDecimal("18.00")).category("甜点饮品").imageUrl("https://example.com/mango.jpg").isAvailable(true).salesCount(450).rating(new BigDecimal("4.7")).ratingCount(130).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
                ).flatMap(dishRepository::save))
                .doOnComplete(() -> log.info("Sample data loaded: 3 categories, 8 dishes"))
                .doOnError(e -> log.error("Sample data load failed: {}", e.getMessage(), e))
                .subscribe();
    }
}
