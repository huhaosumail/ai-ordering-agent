# AI Ordering Agent - 智能点餐 Agent 系统

基于 Spring Boot WebFlux 和 DeepSeek 大模型构建的智能点餐系统，支持自然语言点餐、智能菜品推荐、多轮对话记忆和工具调用能力。

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

### 🧠 Agent智能对话 (新增)
- **多轮对话记忆**: 基于RAG的对话历史管理
- **工具调用能力**: 
  - `query_dishes`: 查询菜品信息
  - `query_orders`: 查询订单信息  
  - `query_categories`: 查询分类信息
- **会话管理**: 支持会话创建、查询、清除

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
    api-key: ${AI_DEEPSEEK_API_KEY:your-api-key}
    base-url: ${AI_DEEPSEEK_BASE_URL:https://api.deepseek.com/v1}
    model: ${AI_DEEPSEEK_MODEL:deepseek-chat}

agent:
  ordering:
    memory:
      max-history-messages: 10
```

**环境变量配置**（推荐生产环境使用）：
- `AI_DEEPSEEK_API_KEY`: DeepSeek API密钥
- `AI_DEEPSEEK_BASE_URL`: API基础URL
- `AI_DEEPSEEK_MODEL`: 使用的模型名称

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

### Agent智能对话 (新增)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/agent/chat` | 智能对话（支持工具调用和记忆） |
| GET | `/api/agent/session/{sessionId}/summary` | 获取会话摘要 |
| GET | `/api/agent/session/{sessionId}/messages` | 获取会话消息列表 |
| DELETE | `/api/agent/session/{sessionId}` | 清除会话 |

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
| `/api/agent` | AGENT |
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

### 4. Agent智能对话（新增）

```bash
# 查询菜品
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"user-123","message":"有什么好吃的推荐吗？"}'

# 继续对话（上下文记忆）
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"user-123","message":"有辣的菜吗？"}'

# 查询订单
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"user-123","message":"查询我的订单"}'
```

**响应示例**:
```json
{"code":200,"message":"success","data":"当前可用的辣味菜品有：\n- 宫保鸡丁 (中式菜肴) - ¥38.00\n  描述：经典川菜，鸡肉鲜嫩，花生酥脆\n- 鱼香肉丝 (中式菜肴) - ¥32.00\n  描述：酸甜微辣，口感丰富\n- 麻婆豆腐 (中式菜肴) - ¥28.00\n  描述：麻辣鲜香，下饭神器"}
```

### 5. 获取会话摘要

```bash
curl http://localhost:8080/api/agent/session/user-123/summary
```

### 6. 创建订单

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

### 7. 查询菜品列表

```bash
curl http://localhost:8080/api/dishes
```

### 8. 查询操作日志

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

### 9. 操作日志统计

```bash
curl "http://localhost:8080/api/logs/stats"

# 指定时间范围
curl "http://localhost:8080/api/logs/stats?startTime=2026-05-01T00:00:00&endTime=2026-05-25T23:59:59"
```

### 10. 按链路 ID 追踪

```bash
# 从响应头 X-Trace-Id 获取 traceId
curl "http://localhost:8080/api/logs/trace/{traceId}"
```

## 项目结构

```
ai-ordering-agent/
├── src/main/java/com/ximalaya/ai/ordering/
│   ├── agent/              # Agent核心模块 (新增)
│   │   ├── AgentService.java
│   │   ├── impl/
│   │   │   └── AgentServiceImpl.java
│   │   ├── tool/           # 工具调用 (新增)
│   │   │   ├── Tool.java
│   │   │   ├── DishQueryTool.java
│   │   │   ├── OrderQueryTool.java
│   │   │   └── CategoryQueryTool.java
│   │   └── ToolDefinition.java
│   ├── controller/         # REST API 控制层
│   │   ├── AiOrderingController.java
│   │   ├── AgentController.java  # Agent对话控制器 (新增)
│   │   ├── CategoryController.java
│   │   ├── DishController.java
│   │   ├── OperationLogController.java
│   │   └── OrderController.java
│   ├── service/            # 业务逻辑层
│   │   ├── ChatMemoryService.java      # RAG记忆服务 (新增)
│   │   ├── AiOrderingService.java
│   │   ├── DishService.java
│   │   ├── OperationLogService.java
│   │   ├── OrderService.java
│   │   └── impl/
│   │       ├── ChatMemoryServiceImpl.java  # RAG记忆实现 (新增)
│   │       ├── AiOrderingServiceImpl.java
│   │       ├── DishServiceImpl.java
│   │       ├── OperationLogServiceImpl.java
│   │       └── OrderServiceImpl.java
│   ├── repository/         # 数据访问层
│   │   ├── ChatHistoryRepository.java  # 对话历史 (新增)
│   │   ├── CategoryRepository.java
│   │   ├── DishRepository.java
│   │   ├── OperationLogRepository.java
│   │   └── OrderRepository.java
│   ├── entity/             # 数据库实体
│   │   ├── ChatHistory.java  # 对话历史实体 (新增)
│   │   ├── Category.java
│   │   ├── Dish.java
│   │   ├── OperationLog.java
│   │   └── Order.java
│   ├── dto/                # 数据传输对象
│   ├── filter/             # WebFilter（操作日志自动留痕）
│   ├── util/               # 工具类
│   ├── config/             # 配置类
│   │   ├── AiConfig.java   # AI配置 (新增)
│   │   └── R2dbcConfig.java
│   └── Application.java    # 启动类
├── src/main/resources/
│   ├── application.yml     # 配置文件
│   └── schema.sql          # 数据库表结构
└── pom.xml                 # Maven配置
```

## Agent架构说明 (新增)

### 核心组件

1. **AgentService**: 主入口，负责接收用户消息并返回响应
2. **ChatMemoryService**: RAG记忆模块，管理对话历史
3. **Tool**: 工具接口，定义可调用的工具能力
4. **DishQueryTool**: 菜品查询工具
5. **OrderQueryTool**: 订单查询工具
6. **CategoryQueryTool**: 分类查询工具

### 工作流程

```
用户消息 → AgentService → 检查是否需要调用工具
                          ↓
                     LLM分析意图
                          ↓
              ┌───────────┴───────────┐
              ↓                       ↓
         需要调用工具           直接回答用户
              ↓                       ↓
     执行工具调用              返回自然语言响应
              ↓
     获取工具结果
              ↓
     LLM总结结果
              ↓
     返回自然语言响应
```

### 工具调用格式

Agent支持以下工具调用格式：
```
<function name="工具名" params="参数JSON">
```

**可用工具**:
- `query_dishes`: 查询菜品
  - 参数: `keyword` (可选), `category` (可选)
- `query_orders`: 查询订单
  - 参数: `orderNo` (可选), `userId` (可选), `status` (可选)
- `query_categories`: 查询分类
  - 参数: 无

### RAG记忆机制

- 对话历史存储在 `chat_history` 表中
- 支持按sessionId管理多个会话
- 默认保留最近10条消息作为上下文
- 支持会话清除和摘要生成

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