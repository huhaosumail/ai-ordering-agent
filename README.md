# AI Ordering Agent — 智能点餐系统

基于 **Spring Boot WebFlux**、**DeepSeek**、**RAG 向量检索** 与 **React** 的智能点餐示例：自然语言查菜、推荐、下单，可选飞书机器人与全链路操作日志。

---

## 文档导航

| 文档 | 适合阅读对象 | 内容 |
|------|----------------|------|
| **本文件（README）** | 所有人 | 快速上手、功能一览、配置与 API 速查 |
| [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) | 开发者 | 模块划分、架构图、调用链、扩展建议 |
| [AI_ORDERING_DISCUSSION_SUMMARY.md](./AI_ORDERING_DISCUSSION_SUMMARY.md) | 产品/技术回顾 | 早期方案讨论与响应式选型背景 |
| [frontend/README.md](./frontend/README.md) | 前端开发 | Vite 小助手启动与代理 |
| [.cursor/skills/](./.cursor/skills/) | Cursor 用户 | 项目级 Agent Skills（含开发示例） |

---

## 一分钟了解

| 模块 | 技术 | 端口 |
|------|------|------|
| 后端 API | Java 21 + WebFlux + R2DBC + H2 | `8080` |
| 聊天前端 | React 19 + Vite 8 | `5173`（开发）/ `3000`（Docker） |
| 对话模型 | DeepSeek Chat | `ai.deepseek.*` |
| 向量模型 | **火山方舟** Embedding（`ep-xxx`） | `ai.embedding.*` + `ARK_API_KEY` |
| 飞书（可选） | 事件订阅 Webhook | `feishu.enabled=true` |

**典型路径**：
- 「有什么辣的菜推荐？」→ `semantic_search_dishes` / RAG → 推荐列表
- 「销量最高的菜有哪些？」→ `query_dishes_sales_rank` → 按 `sales_count` 降序排行
- 「麻婆豆腐 三份」→ `create_order` 下单

---

## 快速开始

### 1. 环境

- JDK 21+、Maven 3.9+
- Node.js 18+（仅前端开发）
- DeepSeek API Key（Agent 对话）
- 火山方舟 API Key + **Embedding 推理接入点** `ep-xxx`（RAG 向量，与 DeepSeek 独立）

### 2. 配置密钥（勿提交 Git）

复制模板并填入密钥：

```bash
cp .env.example .env                          # 或
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml

export SPRING_PROFILES_ACTIVE=local
export AI_DEEPSEEK_API_KEY=你的DeepSeek密钥
export ARK_API_KEY=你的方舟APIKey
export AI_EMBEDDING_MODEL=ep-你的Embedding接入点ID
```

`application-local.yml` 已 gitignore，可集中配置 DeepSeek、方舟、飞书；仓库内仅保留 [application-local.yml.example](./src/main/resources/application-local.yml.example) 与 [.env.example](./.env.example)。

### 3. 启动

```bash
# 终端 1 — 后端
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 终端 2 — 前端（可选）
cd frontend && npm install && npm run dev
```

| 地址 | 说明 |
|------|------|
| http://localhost:5173 | 聊天小助手 |
| http://localhost:8080 | REST API |
| http://localhost:8080/h2-console | H2 控制台 |

### 4. Docker

```bash
export AI_DEEPSEEK_API_KEY=你的DeepSeek密钥
export ARK_API_KEY=你的方舟Key
export AI_EMBEDDING_MODEL=ep-你的接入点ID
docker compose up --build -d
```

---

## 功能一览

### REST 业务

- 菜品 / 分类 / 订单 CRUD 与搜索（`/api/dishes`、`/api/orders` 等）
- 销量榜：`GET /api/dishes/top-sales`（固定 Top 10，与 Agent 工具共用 `DishRepository.findTopSales`）

### Agent 对话（核心）— `/api/agent`

| 工具 | 说明 |
|------|------|
| `query_dishes` | 关键词/分类查菜 |
| `query_dishes_sales_rank` | 销量排行榜；`limit` 可选（默认 10，最大 20），仅统计 `is_available=true` |
| `semantic_search_dishes` | RAG 语义检索（口味、场景描述） |
| `query_orders` / `query_categories` | 查单、查分类 |
| `create_order` | 按菜名+数量下单 |

- 多轮记忆：`chat_history`，默认最近 10 条  
- RAG：对话前可注入 Top-K 相关菜品（`rag.inject-to-agent-prompt`）  
- 兜底：模型未返回工具时，本地解析「麻婆豆腐 三份」等说法  

### RAG — `/api/rag`

- 启动后自动对示例菜品建向量索引；`POST /api/rag/reindex` 可全量重建  
- 向量化仅走 **火山方舟** `POST /embeddings`（`model` = 接入点 `ep-xxx`）  
- 详见下文 [向量库与 RAG 技术架构](#向量库与-rag-技术架构)

### 飞书机器人（可选）— `/api/feishu/webhook`

- 需公网 HTTPS（本地可用 ngrok）  
- 订阅 `im.message.receive_v1`，会话 ID：`feishu:{chat_id}`  
- 快捷指令：`/help`、`/clear`  

### 其他

- `/api/ai/*`：直接调 DeepSeek 解析/推荐（不经 Agent 会话）  
- `/api/logs`：操作日志与 `X-Trace-Id`  

详细配置见 [配置参考](#配置参考)；模块调用链见 [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md)。

---

## 向量库与 RAG 技术架构

本项目**不使用** Milvus / pgvector 等外置向量数据库，而是 **H2 持久化 + 进程内余弦检索** 的轻量方案，向量化统一由 **火山方舟 Embedding 接入点** 完成。

### 架构总览

```text
┌─────────────────┐     POST /embeddings      ┌──────────────────────────┐
│  dish 业务表    │ ──索引文本──────────────► │ 火山方舟 Ark API          │
│  (H2 R2DBC)     │                           │ model = ep-xxx           │
└────────┬────────┘                           │ Authorization: Bearer  │
         │                                    └────────────┬─────────────┘
         │  DishVectorIndexService                          │ float[]
         ▼                                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│  VectorStoreService                                                     │
│  · 写入 dish_embedding（embedding_json, dimension, content_hash）       │
│  · 内存 ConcurrentHashMap 缓存，检索时 cosine similarity                  │
└────────┬───────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────┐     Top-K + min-score     ┌──────────────────────────┐
│  RagService     │ ────────────────────────► │ Agent Prompt /           │
│                 │                           │ semantic_search_dishes   │
└─────────────────┘                           └──────────────────────────┘
```

### 组件说明

| 层级 | 类 / 表 | 职责 |
|------|---------|------|
| **向量化 API** | `DoubaoArkEmbeddingClient` | 调用 `https://ark.cn-beijing.volces.com/api/v3/embeddings`，`model` 填控制台接入点 ID |
| **编排** | `EmbeddingService` | 仅支持 `doubao-ark`；未配置 Key/ep 时默认报错（可开 `fallback-local`） |
| **索引** | `DishVectorIndexService` | 菜品名+描述+分类拼文本 → embed → upsert |
| **存储** | `dish_embedding` 表 | H2 存 JSON 向量与元数据；启动 `reloadFromDatabase()` |
| **检索** | `VectorStoreService` | 内存全量余弦相似度，返回 `ScoredDocument` |
| **对外** | `RagController` | `/api/rag/reindex`、`/search`、`/status` |

### 数据流

1. **建索引**：`dish` → 拼接索引文本 → 方舟 embed → `dish_embedding` + 内存 Map  
2. **查询**：用户 query → embed → 与内存中向量算 cosine → Top-K（`rag.top-k`，默认 5）过滤 `rag.min-score`  
3. **Agent**：`rag.inject-to-agent-prompt=true` 时将检索结果注入系统上下文；或调用工具 `semantic_search_dishes`

### 配置项（火山方舟）

| 变量 / 配置 | 说明 |
|-------------|------|
| `ARK_API_KEY` / `ai.embedding.api-key` | 方舟 API Key（形如 `apikey-...`） |
| `AI_EMBEDDING_MODEL` / `ai.embedding.model` | Embedding 推理接入点，如 `ep-20260526153248-chdn7` |
| `AI_EMBEDDING_BASE_URL` | 默认 `https://ark.cn-beijing.volces.com/api/v3` |
| `AI_EMBEDDING_FALLBACK_LOCAL` | 默认 `false`；`true` 时 API 失败用本地哈希向量（仅开发兜底） |
| `rag.enabled` / `rag.top-k` / `rag.min-score` | 检索开关与阈值 |

### 验证向量库

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 状态：应显示 embeddingConfigured=true、indexedCount=8
curl -s http://localhost:8080/api/rag/status | jq .

# 全量重建（调用方舟 embed 写 dish_embedding）
curl -s -X POST http://localhost:8080/api/rag/reindex | jq .

# 语义检索
curl -s "http://localhost:8080/api/rag/search/text?q=辣的下饭" | jq .
```

`/api/rag/status` 示例字段：`embeddingProvider`、`embeddingEndpoint`、`indexedCount`、`vectorStore=h2-dish_embedding+memory`。

> **说明**：对话 LLM 仍用 DeepSeek（`ai.deepseek.*`），与方舟 Embedding **密钥、计费、接入点均独立**。

---

## Cursor Skills（项目内）

仓库提供 **Cursor Agent Skills** 目录：`.cursor/skills/`。

| Skill | 何时使用 |
|-------|----------|
| `ai-ordering-dev` | 改后端、加 Agent 工具、测 RAG/飞书、查密钥与命令 |

在 Cursor 中可说：「按 ai-ordering-dev skill 帮我加一个 Agent 工具」。新增 Skill 见 [.cursor/skills/README.md](./.cursor/skills/README.md)。

---

## 配置参考

### DeepSeek（Agent 对话，必配其一）

```bash
export AI_DEEPSEEK_API_KEY=你的密钥
export AI_DEEPSEEK_MODEL=deepseek-chat          # 可选
export AGENT_SIMULATION_MODE=false              # true=无 Key 时本地模拟
```

### 火山方舟 Embedding（RAG 向量，与 DeepSeek 无关）

在方舟控制台创建 **Embedding 推理接入点**，记下 `ep-xxx` 与 API Key：

```bash
export ARK_API_KEY=你的方舟APIKey
export AI_EMBEDDING_MODEL=ep-你的Embedding接入点ID
export AI_EMBEDDING_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
export AI_EMBEDDING_FALLBACK_LOCAL=false

curl -X POST http://localhost:8080/api/rag/reindex
curl "http://localhost:8080/api/rag/search/text?q=辣的下饭"
```

仅当 `AI_EMBEDDING_FALLBACK_LOCAL=true` 时，方舟调用失败才会降级为本地哈希向量（不推荐生产使用）。

### 飞书（可选）

```bash
export FEISHU_ENABLED=true
export FEISHU_APP_ID=cli_xxxx
export FEISHU_APP_SECRET=xxxx
export FEISHU_VERIFICATION_TOKEN=xxxx
export FEISHU_ENCRYPT_KEY=xxxx    # 控制台开启加密时
```

事件订阅 URL：`https://<公网域名>/api/feishu/webhook`

---

## API 速查

### Agent

```bash
# 语义推荐
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"有什么辣的菜推荐？"}'

# 销量排行（会调用 query_dishes_sales_rank）
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"销量最高的菜有哪些？"}'
```

### 菜品（REST，不经 Agent）

```bash
curl http://localhost:8080/api/dishes/top-sales
```

### RAG / 向量库

```bash
curl -s http://localhost:8080/api/rag/status
curl -s -X POST http://localhost:8080/api/rag/reindex
curl -s "http://localhost:8080/api/rag/search/text?q=辣的下饭"
curl -s "http://localhost:8080/api/rag/search?q=清淡"
```

### 日志

```bash
curl "http://localhost:8080/api/logs?module=AGENT&page=0&size=20"
```

更多端点见 [TECH_ARCHITECTURE.md#api-端点汇总](./TECH_ARCHITECTURE.md)。

---

## 项目结构（简图）

```
ai-ordering-agent/
├── .cursor/skills/          # Cursor Agent Skills（示例：ai-ordering-dev）
├── frontend/                # React 小助手
├── src/main/java/.../ordering/
│   ├── agent/               # AgentService、Tools
│   ├── embedding/           # 火山方舟 Ark Embedding 客户端
│   ├── feishu/              # 飞书 Webhook
│   ├── service/             # RAG、业务、ChatMemory
│   └── controller/
├── src/main/resources/
│   ├── application.yml      # 默认配置（无密钥）
│   ├── application-local.yml.example
│   └── application-local.yml  # 本地密钥（gitignore）
├── .env.example
├── README.md
└── TECH_ARCHITECTURE.md
```

---

## 测试数据

启动后自动加载 3 个分类、8 道示例菜（宫保鸡丁、麻婆豆腐等），每道菜带 `sales_count` 便于测销量榜。辣味相关会匹配名称/描述含「辣」的菜品；销量榜示例 Top 3 约为宫保鸡丁、麻婆豆腐、鱼香肉丝。

---

## 常见问题

| 现象 | 处理 |
|------|------|
| 回复像固定模板 | DeepSeek Key 无效或 402 → 检查 `AI_DEEPSEEK_API_KEY` |
| 语义搜不到菜 | `POST /api/rag/reindex`；调低 `rag.min-score` |
| Embedding 失败 | 检查 `ARK_API_KEY`、`AI_EMBEDDING_MODEL=ep-xxx`；`curl /api/rag/status` 看 `embeddingConfigured` |
| indexedCount=0 | 配置方舟后执行 `POST /api/rag/reindex` |
| `API key format is incorrect` | 使用控制台 **API Key 管理** 中完整密钥（多为 `ark-` 前缀）；`apikey-xxx` 若为展示 ID 不能当 Bearer 使用 |
| 方舟 500 InternalServiceError | 检查接入点 `ep-xxx` 是否为 **Embedding** 类型且与 Key 同账号/已授权 |
| 飞书无回复 | `FEISHU_ENABLED=true`、权限发布、公网 HTTPS、@ 机器人 |
| 前端连不上 API | 先启后端，再 `npm run dev` |

---

## License

MIT License
