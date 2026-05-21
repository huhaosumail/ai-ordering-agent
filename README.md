
# AI Ordering Agent - 智能点餐系统

基于 Spring Boot 3.2 和 AI 技术构建的智能点餐系统，提供完整的菜品管理、订单管理和分类管理功能。

## 技术栈

- **语言**: Java 21
- **框架**: Spring Boot 3.2.0
- **数据库**: SQLite (嵌入式，无需额外安装)
- **ORM**: Spring Data JPA
- **构建工具**: Maven

## 项目结构

```
ai-ordering-agent/
├── src/main/java/com/ximalaya/ai/ordering/
│   ├── Application.java          # 启动类
│   ├── controller/               # 控制器层
│   │   ├── DishController.java   # 菜品管理
│   │   ├── OrderController.java  # 订单管理
│   │   └── CategoryController.java # 分类管理
│   ├── service/                  # 服务层
│   │   ├── DishService.java
│   │   ├── OrderService.java
│   │   └── impl/
│   ├── repository/               # 数据访问层
│   ├── entity/                   # 实体类
│   ├── dto/                      # 数据传输对象
│   │   ├── request/
│   │   └── response/
│   ├── config/                   # 配置类
│   │   └── DataInitializer.java # 数据初始化
│   └── exception/                # 异常处理
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── application.yml           # 应用配置
└── pom.xml                       # Maven 配置
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+

### 构建项目

```bash
cd ai-ordering-agent
mvn clean package -DskipTests
```

### 运行项目

```bash
# 方式1: 使用 Maven
mvn spring-boot:run

# 方式2: 运行打包后的 Jar
java -jar target/ai-ordering-agent-1.0.0-SNAPSHOT.jar
```

服务启动后访问: http://localhost:8080

## API 接口

### 菜品管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/dishes | 获取菜品列表 |
| GET | /api/dishes/{id} | 获取菜品详情 |
| POST | /api/dishes | 创建菜品 |
| PUT | /api/dishes/{id} | 更新菜品 |
| DELETE | /api/dishes/{id} | 删除菜品 |
| GET | /api/dishes/top-sales | 获取销量最高的菜品 |
| GET | /api/dishes/top-rated | 获取评分最高的菜品 |

### 订单管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/orders | 创建订单 |
| GET | /api/orders/{id} | 获取订单详情 |
| GET | /api/orders/no/{orderNo} | 根据订单号获取订单 |
| GET | /api/orders/user/{userId} | 获取用户订单 |
| PUT | /api/orders/{id}/status | 更新订单状态 |
| PUT | /api/orders/{id}/cancel | 取消订单 |

### 分类管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/categories | 获取所有分类 |
| GET | /api/categories/{id} | 获取分类详情 |
| POST | /api/categories | 创建分类 |
| PUT | /api/categories/{id} | 更新分类 |
| DELETE | /api/categories/{id} | 删除分类 |

## API 示例

### 创建订单

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "tableNo": "A01",
    "items": [
      {"dishId": 1, "quantity": 2},
      {"dishId": 4, "quantity": 1}
    ],
    "remark": "少辣"
  }'
```

### 查询菜品列表

```bash
curl http://localhost:8080/api/dishes
```

### 查询销量最高的菜品

```bash
curl http://localhost:8080/api/dishes/top-sales
```

## 数据库

项目使用 SQLite 嵌入式数据库，无需额外安装数据库服务。数据库文件会自动创建在项目根目录下的 `example_db.sqlite`。

## 初始数据

系统启动时会自动初始化以下数据：

- 6 个菜品分类（热销菜品、川菜、粤菜、素菜、汤品、主食）
- 12 道示例菜品

## License

MIT License