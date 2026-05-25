# AI Ordering Agent - 智能点餐 Agent 系统

基于 **Spring Boot WebFlux**、**DeepSeek 大模型** 和 **React 聊天前端** 构建的智能点餐系统。支持自然语言查菜、推荐、下单，多轮对话记忆，Agent 工具调用，以及全链路操作日志留痕。

## 项目概览

| 模块 | 说明 |
|------|------|
| **后端** | Java 21 + WebFlux + R2DBC + H2，端口 `8080` |
| **前端** | React + Vite 小助手，端口 `5173`，开发时代理 `/api` → 后端 |
| **大模型** | DeepSeek Chat API（Agent 与 `/api/ai/*` 共用） |
| **数据** | 启动时自动加载 3 类分类、8 道示例菜品 |

### 典型使用场景

1. 打开浏览器小助手 → 问「有什么辣的菜推荐？」→ Agent 查库并总结  
2. 继续说「麻婆豆腐 三份」或「两份麻婆豆腐」→ Agent 调用 `create_order` 创建订单  
3. 通过 `/api/logs` 按 `traceId` 追踪请求与 AI 调用  

## 技术栈

- **框架**: Spring Boot 3.2.5 + Spring WebFlux（响应式）
- **数据库**: H2 内存库 + R2DBC
- **大模型**: DeepSeek Chat API（OkHttp）
- **前端**: React 19 + Vite 8 + TypeScript
- **语言**: Java 21

## 功能特性

### 菜品与订单（REST）

- 菜品 CRUD、分类、搜索、销量/评分排行  
- 订单创建、查询、状态更新、取消  

### AI 智能点餐（`/api/ai`）

- **自然语言解析**: `POST /api/ai/order/parse`  
- **一键下单**: `POST /api/ai/order`（解析 + 创建订单）  
- **推荐**: `GET /api/ai/recommend`  

### Agent 智能对话（`/api/agent`）— 小助手核心

- **默认调用 DeepSeek** 理解意图并决定是否使用工具  
- **多轮记忆**: `chat_history` 表，默认最近 10 条上下文  
- **工具能力**:

| 工具 | 说明 |
|------|------|
| `query_dishes` | 按关键词/分类查菜品 |
| `query_orders` | 查订单 |
| `query_categories` | 查分类 |
| `create_order` | 按菜名+数量创建订单 |

- **会话 API**: 聊天、消息列表、摘要、清除会话  
- **兜底机制**: API 失败或模型未返回工具调用时，本地解析下单意图（如「麻婆豆腐 三份」「三份麻婆豆腐」）并执行 `create_order`  

### 操作日志留痕

- 所有 `/api/**` 自动写入 `operation_log`  
- 响应头 `X-Trace-Id` 链路追踪  
- DeepSeek 内部调用记录为 `AI` / `AGENT_CHAT` 等  

## 快速开始

### 环境要求

- JDK 21+、Maven 3.9+  
- Node.js 18+（前端开发）  
- 有效的 **DeepSeek API Key**（需账户有余额）  

### 配置 DeepSeek（必做）

**推荐使用环境变量**（勿将真实 Key 提交到 Git）：

```bash
export AI_DEEPSEEK_API_KEY=你的密钥
export AI_DEEPSEEK_BASE_URL=https://api.deepseek.com/v1   # 可选
export AI_DEEPSEEK_MODEL=deepseek-chat                     # 可选
```

`application.yml` 片段：

```yaml
ai:
  deepseek:
    api-key: ${AI_DEEPSEEK_API_KEY:your-api-key}
    base-url: ${AI_DEEPSEEK_BASE_URL:https://api.deepseek.com/v1}
    model: ${AI_DEEPSEEK_MODEL:deepseek-chat}

agent:
  ordering:
    # false（默认）= 调用 DeepSeek；true = 仅本地关键词模拟（联调/无 Key 时）
    simulation-mode: ${AGENT_SIMULATION_MODE:false}
    memory:
      max-history-messages: 10
```

### 启动前后端（开发）

```bash
# 终端 1：后端（先 export AI_DEEPSEEK_API_KEY）
cd ai-ordering-agent
mvn spring-boot:run

# 终端 2：前端
cd frontend
npm install
npm run dev
```

- **小助手**: http://localhost:5173  
- **后端 API**: http://localhost:8080  
- **H2 控制台**: http://localhost:8080/h2-console  

### Docker 一键部署

```bash
export AI_DEEPSEEK_API_KEY=your-api-key
docker compose up --build -d
```

- 前端: http://localhost:3000  
- 后端: http://localhost:8080  

### 打包运行

```bash
mvn clean package
export AI_DEEPSEEK_API_KEY=你的密钥
java -jar target/ai-ordering-agent-1.0.0-SNAPSHOT.jar
```

## 如何确认已走 DeepSeek

查看后端日志：

- **成功**: 无 `DeepSeek API调用失败`，回复为自然语言（非固定模板）  
- **失败降级**: 出现 `DeepSeek API调用失败: 402`（余额/Key）等，随后 `使用模拟模式响应`  

充值或更换 Key 后需**重启后端**，并确保进程能读到 `AI_DEEPSEEK_API_KEY`。

## API 接口摘要

### Agent 对话

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/agent/chat` | 智能对话（body: `sessionId`, `message`, `userId` 可选） |
| GET | `/api/agent/session/{id}/messages` | 会话消息列表 |
| GET | `/api/agent/session/{id}/summary` | 会话摘要 |
| DELETE | `/api/agent/session/{id}` | 清除会话 |

### 菜品 / 订单 / AI / 日志

详见下文「API 使用示例」及原表；日志模块路径前缀 `/api/logs`。

## API 使用示例

### Agent：查辣味推荐

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"有什么辣的菜推荐？"}'
```

### Agent：自然语言下单

支持多种说法，例如：

- `我要两份麻婆豆腐`  
- `麻婆豆腐 三份`  
- `麻婆豆腐三份`  

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"麻婆豆腐 三份"}'
```

### AI 解析点餐（不经过 Agent 会话）

```bash
curl -X POST http://localhost:8080/api/ai/order/parse \
  -H "Content-Type: application/json" \
  -d '{"input":"我要一份宫保鸡丁和两份鱼香肉丝"}'
```

### 查询菜品

```bash
curl http://localhost:8080/api/dishes
```

### 操作日志

```bash
curl "http://localhost:8080/api/logs?module=AGENT&page=0&size=20"
curl "http://localhost:8080/api/logs/trace/{traceId}"
```

## 项目结构

```
ai-ordering-agent/
├── frontend/                    # React 点餐小助手
│   ├── src/api/agent.ts         # Agent API
│   ├── src/components/ChatApp.tsx
│   ├── vite.config.ts           # 开发代理 /api → :8080
│   └── Dockerfile / nginx.conf
├── docker-compose.yml
├── src/main/java/.../ordering/
│   ├── agent/
│   │   ├── impl/AgentServiceImpl.java   # DeepSeek + 工具编排 + 模拟兜底
│   │   └── tool/
│   │       ├── DishQueryTool.java
│   │       ├── OrderQueryTool.java
│   │       ├── CategoryQueryTool.java
│   │       └── CreateOrderTool.java     # 下单工具
│   ├── controller/              # REST（含 AgentController）
│   ├── service/                 # 业务 + ChatMemory + AiOrdering
│   ├── config/
│   │   ├── DataInitializer.java # 示例数据（须在 java 目录下）
│   │   └── R2dbcConfig.java
│   ├── filter/OperationLogWebFilter.java
│   └── ...
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql
├── TECH_ARCHITECTURE.md         # 架构说明
├── AI_ORDERING_DISCUSSION_SUMMARY.md
└── pom.xml
```

## Agent 架构

```
用户消息 → AgentController → AgentService
                ↓
        加载会话历史 (ChatMemory)
                ↓
        DeepSeek（simulation-mode=false）
                ↓
    ┌───────────┴────────────┐
    │ 返回 <function ...>   │  直接文本回复
    └───────────┬────────────┘
                ↓
         执行 Tool（查菜/查单/下单）
                ↓
         再次调用 DeepSeek 总结（或本地 summarize）
                ↓
         写入 chat_history → 返回用户
```

**工具调用格式**（模型或模拟层输出）：

```text
<function name="create_order" params='{"items":[{"name":"麻婆豆腐","quantity":3}],"userId":1}'>
```

## 测试数据

启动后 `DataInitializer` 自动写入：

- **分类**: 中式菜肴、西式料理、甜点饮品  
- **菜品**: 宫保鸡丁、鱼香肉丝、麻婆豆腐、糖醋里脊、黑椒牛柳、意大利面、提拉米苏、芒果布丁  

辣味相关查询会匹配名称/描述中含「辣」的菜品（如鱼香肉丝、麻婆豆腐）。

## 常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| 固定问候语、不像 AI | DeepSeek 402/401，降级模拟 | 充值、设置 `AI_DEEPSEEK_API_KEY` 并重启 |
| 查不到任何菜 | 示例数据未加载 | 确认 `DataInitializer` 在 `src/main/java/.../config/` |
| 说了份数却不下单 | 旧版仅支持「三份麻婆豆腐」 | 已支持「麻婆豆腐 三份」及兜底下单 |
| 前端 API 失败 | 后端未启动 | 先 `mvn spring-boot:run`，再 `npm run dev` |

## 相关文档

- [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) — 技术架构与调用链  
- [AI_ORDERING_DISCUSSION_SUMMARY.md](./AI_ORDERING_DISCUSSION_SUMMARY.md) — 方案讨论摘要  
- [frontend/README.md](./frontend/README.md) — 前端说明  

## License

MIT License
