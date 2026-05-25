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

### 📊 操作日志留痕
- **全链路自动记录**: 所有 `/api/**` 请求由 `OperationLogWebFilter` 自动写入 `operation_log` 表
- **链路追踪**: 响应头返回 `X-Trace-Id`，支持按 traceId 查询同一请求相关日志
- **AI 内部调用留痕**: DeepSeek API 调用单独记录为 `AI` / `DEEPSEEK_CHAT`
- **多维查询**: 支持按模块、成功/失败、关键词、时间范围分页查询
- **统计分析**: 提供总量、成功率、按模块/小时分布等统计接口

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

### 操作日志

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/logs` | 分页查询操作日志（支持 module、success、keyword、startTime、endTime） |
| GET | `/api/logs/{id}` | 获取单条日志详情 |
| GET | `/api/logs/trace/{traceId}` | 按链路 ID 查询日志 |
| GET | `/api/logs/stats` | 操作日志统计（默认近 7 天） |

**模块划分**:

| 路径前缀 | 模块标识 |
|---------|---------|
| `/api/ai` | AI |
| `/api/orders` | ORDER |
| `/api/dishes` | DISH |
| `/api/categories` | CATEGORY |
| `/api/logs` | LOG |

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

### 6. 查询操作日志

```bash
# 分页查询（可按模块、成功状态、关键词筛选）
curl "http://localhost:8080/api/logs?module=AI&page=0&size=20"

# 查询失败记录
curl "http://localhost:8080/api/logs?success=false"

# 按关键词搜索
curl "http://localhost:8080/api/logs?keyword=宫保鸡丁"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "traceId": "ba00c97f-81c1-4084-a4f1-712875e70bb7",
        "module": "AI",
        "action": "AI_PARSE_ORDER",
        "httpMethod": "POST",
        "requestPath": "/api/ai/order/parse",
        "requestParams": "{\"input\":\"我要一份宫保鸡丁\"}",
        "responseStatus": 200,
        "success": true,
        "durationMs": 1200,
        "createdAt": "2026-05-25T10:42:30.611666"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 7. 操作日志统计

```bash
curl "http://localhost:8080/api/logs/stats"

# 指定时间范围
curl "http://localhost:8080/api/logs/stats?startTime=2026-05-01T00:00:00&endTime=2026-05-25T23:59:59"
```

### 8. 按链路 ID 追踪

```bash
# 从响应头 X-Trace-Id 获取 traceId
curl "http://localhost:8080/api/logs/trace/{traceId}"
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
│   ├── filter/          # WebFilter（操作日志自动留痕）
│   ├── util/            # 工具类
│   ├── config/          # 配置类（R2DBC 建表等）
│   └── Application.java # 启动类
├── src/main/resources/
│   ├── application.yml  # 配置文件
│   └── schema.sql       # 数据库表结构（含 operation_log）
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