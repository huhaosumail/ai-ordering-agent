# AI Ordering Agent - 智能点餐 Agent 系统

基于 **Spring Boot WebFlux**、**DeepSeek 大模型** 和 **React 聊天前端** 构建的智能点餐系统。支持自然语言查菜、推荐、下单，多轮对话记忆，Agent 工具调用，**飞书机器人**（可选）接入，以及全链路操作日志留痕。

## 项目概览

| 模块 | 说明 |
|------|------|
| **后端** | Java 21 + WebFlux + R2DBC + H2，端口 `8080` |
| **前端** | React + Vite 小助手，端口 `5173`，开发时代理 `/api` → 后端 |
| **大模型** | DeepSeek Chat API（Agent 与 `/api/ai/*` 共用） |
| **飞书** | 事件订阅 Webhook + Open API 回复（`feishu.enabled=true` 时启用） |
| **数据** | 启动时自动加载 3 类分类、8 道示例菜品 |

### 典型使用场景

1. 打开浏览器小助手 → 问「有什么辣的菜推荐？」→ Agent 查库并总结  
2. 继续说「麻婆豆腐 三份」或「两份麻婆豆腐」→ Agent 调用 `create_order` 创建订单  
3. 通过 `/api/logs` 按 `traceId` 追踪请求与 AI 调用  
4. 在飞书私聊/群聊 @ 机器人 → 同样走 Agent 查菜、推荐、下单（会话按 `chat_id` 隔离）  

## 技术栈

- **框架**: Spring Boot 3.2.5 + Spring WebFlux（响应式）
- **数据库**: H2 内存库 + R2DBC
- **大模型**: DeepSeek Chat API（OkHttp）
- **飞书**: 开放平台事件订阅 + `tenant_access_token` + 消息回复 API（OkHttp）
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
| `semantic_search_dishes` | **RAG** 语义/口味/场景向量检索 |
| `query_orders` | 查订单 |
| `query_categories` | 查分类 |
| `create_order` | 按菜名+数量创建订单 |

- **RAG 增强**: 对话前自动检索相关菜品注入 Prompt；模糊描述（如「辣的」「下饭」）优先走向量检索  
- **会话 API**: 聊天、消息列表、摘要、清除会话  
- **兜底机制**: API 失败或模型未返回工具调用时，本地解析下单意图（如「麻婆豆腐 三份」「三份麻婆豆腐」）并执行 `create_order`  

### RAG 与向量检索（`/api/rag`）

菜品向量化后存入 `dish_embedding` 表，内存索引 + 余弦相似度检索。

| 能力 | 说明 |
|------|------|
| **向量索引** | 启动加载示例菜后自动 `reindex`；菜品增删改后自动更新向量 |
| **Embedding** | 默认 **豆包 bge-m3**（VikingDB embedding v2，1024 维）；可选火山方舟 `doubao-ark`；失败时本地向量兜底 |
| **Agent 注入** | `rag.inject-to-agent-prompt=true` 时在对话 Prompt 附带 Top-K 相关菜品 |
| **语义工具** | Agent 可调用 `semantic_search_dishes`，参数 `query` |
| **管理 API** | 检索、重建索引、查看索引状态 |

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/rag/search?q=` | 向量检索（JSON，含 score） |
| GET | `/api/rag/search/text?q=` | 向量检索（可读文本） |
| GET | `/api/rag/status` | 索引条数、是否启用 |
| POST | `/api/rag/reindex` | 全量重建向量索引 |

```yaml
rag:
  enabled: true
  top-k: 5
  min-score: 0.35
  inject-to-agent-prompt: true

ai:
  embedding:
    provider: doubao-bge-m3
    api-key: ${VIKINGDB_EMBEDDING_TOKEN}
    host: https://api-vikingdb.vikingdb.cn-beijing.volces.com
    model-name: bge-m3
    dimensions: 1024
    fallback-local: true
```

**与 DeepSeek 的关系**：`ai.deepseek` 仅负责 Agent **对话**；`ai.embedding` 仅负责 **向量化**，API Key 与模型互不影响。

#### 豆包 bge-m3（默认）

在 [VikingDB Embedding v2 文档](https://www.volcengine.com/docs/84313/1254554) 开通服务并获取 Token：

```bash
export AI_EMBEDDING_PROVIDER=doubao-bge-m3
export VIKINGDB_EMBEDDING_TOKEN=你的EmbeddingToken
export VIKINGDB_EMBEDDING_HOST=https://api-vikingdb.vikingdb.cn-beijing.volces.com
export AI_EMBEDDING_DIMENSIONS=1024
export AI_DEEPSEEK_API_KEY=你的DeepSeek密钥   # 仅聊天
curl -X POST http://localhost:8080/api/rag/reindex
```

#### 火山方舟 Embedding（可选）

```bash
export AI_EMBEDDING_PROVIDER=doubao-ark
export AI_EMBEDDING_API_KEY=你的方舟APIKey
export AI_EMBEDDING_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
export AI_EMBEDDING_MODEL=ep-xxxxxxxxxx
```

### 飞书机器人（`/api/feishu`，可选）

默认关闭（`feishu.enabled=false`），开启后注册 `FeishuController` / `FeishuEventService` / `FeishuClient`。

| 能力 | 说明 |
|------|------|
| **事件回调** | `POST /api/feishu/webhook`，处理飞书事件订阅 |
| **URL 校验** | 配置订阅地址时自动响应 `challenge`（支持明文与加密响应） |
| **接收消息** | 订阅 `im.message.receive_v1`，支持 Schema 1.0 / 2.0 |
| **Agent 对话** | 文本消息复用 `AgentService.chat()`，与 Web 小助手相同的查菜/推荐/下单工具链 |
| **多轮记忆** | 会话 ID：`feishu:{chat_id}`，写入 `chat_history`，与 Web 端 `sessionId` 隔离 |
| **消息回复** | 调用 `im/v1/messages/{message_id}/reply` 回复用户原消息 |
| **异步处理** | 先快速返回 HTTP 200，再在后台调用 Agent 并回复（满足飞书 3 秒超时） |
| **安全校验** | 可选 `verification-token`；可选 `encrypt-key` AES 加解密（与控制台 Encrypt Key 一致） |
| **去重** | Schema 2.0 按 `event_id` 内存去重，避免重复投递 |
| **过滤** | 忽略机器人自身消息（`sender_type=app`）、非文本消息 |
| **快捷指令** | `/help`、`/帮助` 使用说明；`/clear`、`/重置` 清除当前会话记忆 |

**配置项**（`application.yml` / 环境变量）：

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| `feishu.enabled` | `FEISHU_ENABLED` | 是否启用，默认 `false` |
| `feishu.app-id` | `FEISHU_APP_ID` | 应用 App ID |
| `feishu.app-secret` | `FEISHU_APP_SECRET` | 应用 App Secret |
| `feishu.verification-token` | `FEISHU_VERIFICATION_TOKEN` | 事件订阅 Verification Token（建议配置） |
| `feishu.encrypt-key` | `FEISHU_ENCRYPT_KEY` | 事件加密密钥（控制台开启加密时必填） |
| `feishu.base-url` | `FEISHU_BASE_URL` | API 根地址，默认 `https://open.feishu.cn` |

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

# 飞书机器人（默认关闭）
feishu:
  enabled: ${FEISHU_ENABLED:false}
  app-id: ${FEISHU_APP_ID:}
  app-secret: ${FEISHU_APP_SECRET:}
  verification-token: ${FEISHU_VERIFICATION_TOKEN:}
  encrypt-key: ${FEISHU_ENCRYPT_KEY:}
  base-url: ${FEISHU_BASE_URL:https://open.feishu.cn}
```

### 配置飞书机器人

1. 打开 [飞书开放平台](https://open.feishu.cn/app) 创建**企业自建应用**  
2. **凭证与基础信息**：复制 App ID、App Secret  
3. **权限管理**：开通并发布版本，至少包含：  
   - 获取与发送单聊、群组消息（`im:message`）  
   - 以应用身份发消息（`im:message:send_as_bot`）  
4. **事件订阅**（需公网 **HTTPS**，本地开发可用 [ngrok](https://ngrok.com/) 等隧道）：  
   - 请求地址：`https://你的域名/api/feishu/webhook`  
   - 添加事件：`im.message.receive_v1`  
   - 填写 **Verification Token**、**Encrypt Key**（与下方环境变量一致；未开启加密可不填 `encrypt-key`）  
5. **机器人**：在应用能力中启用机器人，将机器人拉入目标群或发起私聊  

```bash
export FEISHU_ENABLED=true
export FEISHU_APP_ID=cli_xxxx
export FEISHU_APP_SECRET=xxxx
export FEISHU_VERIFICATION_TOKEN=xxxx      # 建议配置，与控制台一致
export FEISHU_ENCRYPT_KEY=xxxx             # 控制台开启「加密」时必填
# export FEISHU_BASE_URL=https://open.feishu.cn  # 可选，国际版可改为 Lark 域名
```

启动后端后，在飞书私聊或群里 @ 机器人发送「有什么辣的菜推荐？」或「麻婆豆腐 三份」即可对话、下单。

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
# 可选：飞书
export FEISHU_ENABLED=true
export FEISHU_APP_ID=cli_xxxx
export FEISHU_APP_SECRET=xxxx
export FEISHU_VERIFICATION_TOKEN=xxxx
export FEISHU_ENCRYPT_KEY=xxxx
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

### 飞书（需 `feishu.enabled=true`）

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/feishu/webhook` | 飞书事件订阅回调（URL 校验、收消息、加密体解密） |

飞书侧无额外 REST；用户通过 IM 发消息，服务端异步回复。清除飞书会话可在 IM 中发送 `/clear` 或 `/重置`。

### 菜品 / 订单 / AI / 日志

详见下文「API 使用示例」及原表；日志模块路径前缀 `/api/logs`。

## API 使用示例

### Agent：查辣味推荐

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"有什么辣的菜推荐？"}'
```

### RAG：语义找菜

```bash
curl "http://localhost:8080/api/rag/search/text?q=辣的下饭"
curl -X POST http://localhost:8080/api/rag/reindex
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

### 飞书：本地调试 Webhook（URL 校验）

将 `challenge` 替换为飞书控制台推送的值：

```bash
curl -X POST http://localhost:8080/api/feishu/webhook \
  -H "Content-Type: application/json" \
  -d '{"challenge":"test-challenge-token","token":"你的VERIFICATION_TOKEN","type":"url_verification"}'
```

期望响应：`{"challenge":"test-challenge-token"}`（若开启加密则为 `{"encrypt":"..."}`）。

飞书真实消息由开放平台 POST 到同一地址，无需手动 curl；需保证 `FEISHU_ENABLED=true` 且回调 URL 公网可达。

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
│   ├── controller/
│   │   ├── AgentController.java
│   │   └── FeishuController.java        # POST /api/feishu/webhook
│   ├── feishu/                  # 飞书回调与 Open API
│   ├── vector/                  # 余弦相似度、ScoredDocument
│   ├── service/
│   │   ├── EmbeddingService.java        # 文本向量化
│   │   ├── VectorStoreService.java      # 向量存储与检索
│   │   ├── RagService.java              # RAG 上下文组装
│   │   └── DishVectorIndexService.java  # 菜品索引维护
│   ├── controller/RagController.java
│   ├── service/                 # 业务 + ChatMemory + AiOrdering
│   ├── config/
│   │   ├── DataInitializer.java # 示例数据（须在 java 目录下）
│   │   ├── FeishuProperties.java  # feishu.* 配置
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

## RAG / 向量库架构

```
菜品 dish → 拼接索引文本（名/分类/描述/价格）
        → EmbeddingService（豆包 bge-m3 / 方舟 / 本地兜底）
        → dish_embedding 表 + 内存索引
用户提问 → 查询向量化 → Top-K 余弦相似度
        → 注入 Agent Prompt / semantic_search_dishes 工具
        → DeepSeek 结合检索结果回答或下单
```

生产环境可将 H2 换为 Postgres + pgvector，或对接 Milvus/Qdrant，当前实现为**嵌入式向量库**（H2 持久化 + 内存检索），适合演示与中小菜单。

## 飞书接入架构

```
飞书用户发文本 → 开放平台 POST /api/feishu/webhook
        ↓
FeishuController → FeishuEventService
        ↓
  解密/校验 token → 识别 im.message.receive_v1
        ↓
  立即返回 200（challenge 或 {}）
        ↓
  异步：sessionId = feishu:{chat_id}
        ↓
  AgentService.chat()（同 Web 小助手）
        ↓
  FeishuClient.replyText(message_id) → 飞书会话展示回复
        ↓
  写入 chat_history
```

与 Web 小助手共用 **Agent 工具链**（`query_dishes` / `create_order` 等）和 **DeepSeek** 配置；仅入口与会话 ID 前缀不同。

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
| 飞书 URL 校验失败 | 未启用或 Token 不一致 | 设置 `FEISHU_ENABLED=true`，核对 `FEISHU_VERIFICATION_TOKEN` |
| 飞书收消息无回复 | 权限未发布、未 @ 机器人、DeepSeek 失败 | 检查应用权限与版本发布；看日志 `飞书回复消息失败` / DeepSeek 402 |
| 飞书提示加密错误 | 控制台开启加密但未配 Key | 设置 `FEISHU_ENCRYPT_KEY` 与控制台 Encrypt Key 一致 |
| 飞书回调 404 | 未启用飞书模块 | `feishu.enabled` 必须为 `true` 并重启 |
| 本地无法收飞书事件 | 飞书要求公网 HTTPS | 使用 ngrok：`ngrok http 8080`，将 HTTPS 地址配到事件订阅 |
| 语义检索无结果 | 索引未建立或分数过低 | `POST /api/rag/reindex`；调低 `rag.min-score` |
| Embedding API 失败 | Token/Host 错误或模型未开通 | 核对 `VIKINGDB_EMBEDDING_TOKEN`、地域 Host；或改 `doubao-ark`；保持 `fallback-local=true` |
| 切换 bge-m3 后检索异常 | 向量维度变化 | `POST /api/rag/reindex` 全量重建索引 |

## 相关文档

- [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) — 技术架构与调用链  
- [AI_ORDERING_DISCUSSION_SUMMARY.md](./AI_ORDERING_DISCUSSION_SUMMARY.md) — 方案讨论摘要  
- [frontend/README.md](./frontend/README.md) — 前端说明  

## License

MIT License
