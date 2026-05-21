# AI Ordering Agent - 智能点餐 Agent 系统

基于 Spring Boot WebFlux 和 DeepSeek 大模型构建的智能点餐系统，支持自然语言点餐和智能菜品推荐。

## 技术栈

- **框架**: Spring Boot 3.2.5 + Spring WebFlux (响应式)
- **数据库**: H2 + R2DBC (响应式数据库)
- **大模型**: DeepSeek Chat API
- **HTTP客户端**: OkHttp 4.12.0
- **语言**: Java 21

## 功能特性

### 🍳 菜品管理
- 菜品CRUD操作
- 分类查询
- 销量排行
- 评分排行

### 📋 订单管理
- 创建订单
- 查询订单列表
- 查询订单详情
- 更新订单状态
- 取消订单

### 🤖 AI智能点餐
- **自然语言点餐**: 支持"我要一份宫保鸡丁和两份鱼香肉丝"
- **智能推荐**: 根据用户偏好推荐菜品

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+

### 配置说明

在 `application.yml` 中配置 DeepSeek API：

```yaml
ai:
  deepseek:
    api-key: sk-9dd346e5f5a6498998cb932a146959f1
    base-url: https://api.deepseek.com/v1
    model: deepseek-chat
```

### 运行方式

**开发模式**:
```bash
cd ai-ordering-agent
mvn spring-boot:run
```

**打包运行**:
```bash
mvn clean package
java -jar target/ai-ordering-agent-1.0.0-SNAPSHOT.jar
```

服务启动后访问: http://localhost:8080

### H2控制台

访问 H2 数据库控制台: http://localhost:8080/h2-console

## API 接口

### 菜品管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/dishes` | 获取所有可用菜品 |
| GET | `/api/dishes/{id}` | 获取菜品详情 |
| POST | `/api/dishes` | 创建菜品 |
| PUT | `/api/dishes/{id}` | 更新菜品 |
| DELETE | `/api/dishes/{id}` | 删除菜品 |
| GET | `/api/dishes/category/{category}` | 按分类查询 |
| GET | `/api/dishes/search?keyword=xxx` | 搜索菜品 |
| GET | `/api/dishes/top-sales` | 销量排行 |
| GET | `/api/dishes/top-rated` | 评分排行 |

### 订单管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/orders` | 创建订单 |
| GET | `/api/orders` | 获取订单列表 |
| GET | `/api/orders/{id}` | 获取订单详情 |
| PUT | `/api/orders/{id}/status` | 更新订单状态 |
| DELETE | `/api/orders/{id}` | 取消订单 |

### AI智能点餐

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/ai/order/parse` | 解析自然语言点餐 |
| POST | `/api/ai/order` | AI智能点餐 |
| GET | `/api/ai/recommend` | 获取菜品推荐 |

## API 使用示例

### 1. 自然语言点餐解析

```bash
curl -X POST http://localhost:8080/api/ai/order/parse \
  -H "Content-Type: application/json" \
  -d '{"input":"我要一份宫保鸡丁和两份鱼香肉丝"}'
```

**响应示例**:
```json
{"code":200,"message":"success","data":{"userId":null,"tableNo":null,"items":[{"dishId":1,"quantity":1},{"dishId":2,"quantity":2}],"remark":""}}
```

### 2. AI智能点餐

```bash
curl -X POST http://localhost:8080/api/ai/order \
  -H "Content-Type: application/json" \
  -d '{"input":"我要一份宫保鸡丁"}'
```

### 3. 获取菜品推荐

```bash
curl -X GET "http://localhost:8080/api/ai/recommend?preferences=喜欢辣的"
```

### 4. 创建订单

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"dishId": 1, "quantity": 2},
      {"dishId": 2, "quantity": 1}
    ],
    "remark": "少辣"
  }'
```

### 5. 查询菜品列表

```bash
curl http://localhost:8080/api/dishes
```

## 项目结构

```
ai-ordering-agent/
├── src/main/java/com/ximalaya/ai/ordering/
│   ├── controller/      # REST API 控制层
│   ├── service/         # 业务逻辑层
│   ├── repository/      # 数据访问层
│   ├── entity/          # 数据库实体
│   ├── dto/             # 数据传输对象
│   ├── config/          # 配置类
│   └── Application.java # 启动类
├── src/main/resources/
│   └── application.yml  # 配置文件
└── pom.xml              # Maven配置
```

## 响应式架构说明

本项目采用 Spring WebFlux 响应式架构，具有以下特点：

- **非阻塞IO**: 所有数据库操作使用 R2DBC
- **异步处理**: 使用 Mono/Flux 进行异步数据流处理
- **高性能**: 支持高并发场景
- **事件驱动**: 基于 Reactor 响应式编程模型

## 测试数据

系统启动时自动初始化以下测试数据：

**分类**: 中式菜肴、西式料理、甜点饮品

**菜品**: 宫保鸡丁、鱼香肉丝、麻婆豆腐、糖醋里脊、黑椒牛柳、意大利面、提拉米苏、芒果布丁

## License

MIT License