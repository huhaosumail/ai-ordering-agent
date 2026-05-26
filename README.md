# AI Ordering Agent — 智能点餐系统

基于 **Spring Boot WebFlux**、**DeepSeek 对话**、**火山方舟向量 RAG** 与 **React** 的智能点餐示例：自然语言查菜、语义推荐、销量排行、下单，可选飞书机器人与全链路操作日志。

---

## 文档导航

| 文档 | 读者 | 内容 |
|------|------|------|
| **本文件** | 所有人 | 快速上手、向量库架构、配置与 API |
| [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) | 开发者 | 模块、调用链、数据表、PlantUML |
| [AI_ORDERING_DISCUSSION_SUMMARY.md](./AI_ORDERING_DISCUSSION_SUMMARY.md) | 回顾选型 | 响应式 + AI 集成讨论摘要 |
| [frontend/README.md](./frontend/README.md) | 前端 | Vite 小助手与代理 |
| [.cursor/skills/](./.cursor/skills/) | Cursor | 项目级 Agent Skills |

---

## 一分钟了解

| 模块 | 技术 | 默认端口 |
|------|------|----------|
| 后端 API | Java 21 · WebFlux · R2DBC · H2 | `8080` |
| 聊天前端 | React 19 · Vite 8 | `5173`（dev）/ `3000`（Docker） |
| 对话 LLM | DeepSeek Chat | `ai.deepseek.*` |
| 向量/RAG | 火山方舟 Ark（`ep-xxx`） | `ai.embedding.*` · `ARK_API_KEY` |
| 飞书（可选） | 事件订阅 Webhook | `feishu.enabled=true` |

**典型对话路径**

| 用户说法 | 路径 |
|----------|------|
| 有什么辣的菜推荐？ | RAG / `semantic_search_dishes` |
| 销量最高的菜？ | `query_dishes_sales_rank` |
| 麻婆豆腐来三份 | `create_order` |

---

## 快速开始

### 环境

- JDK 21+、Maven 3.9+
- Node.js 18+（仅前端）
- [DeepSeek](https://platform.deepseek.com/) API Key（Agent）
- [火山方舟](https://www.volcengine.com/product/ark) API Key + **Embedding 接入点** `ep-xxx`（RAG，与 DeepSeek **独立**）

### 配置密钥（勿提交 Git）

```bash
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml

export SPRING_PROFILES_ACTIVE=local
export AI_DEEPSEEK_API_KEY=你的DeepSeek密钥
export ARK_API_KEY=你的方舟密钥          # 控制台完整 Key，多为 ark- 开头
export AI_EMBEDDING_MODEL=ep-你的接入点ID
# 若为 doubao-embedding-vision 接入点：
export AI_EMBEDDING_MULTIMODAL=true
```

也可使用 [.env.example](./.env.example)（复制为 `.env`，已 gitignore）。

### 启动

```bash
# 后端
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 前端（可选）
cd frontend && npm install && npm run dev
```

| 地址 | 说明 |
|------|------|
| http://localhost:5173 | 聊天小助手 |
| http://localhost:8080 | REST API |
| http://localhost:8080/h2-console | H2 控制台（JDBC: `r2dbc:h2:mem:///orderdb`） |

### Docker

```bash
export AI_DEEPSEEK_API_KEY=...
export ARK_API_KEY=...
export AI_EMBEDDING_MODEL=ep-...
export AI_EMBEDDING_MULTIMODAL=true   # vision 接入点时
docker compose up --build -d
```

---

## 功能一览

### REST 业务

- 菜品 / 分类 / 订单 CRUD：`/api/dishes`、`/api/categories`、`/api/orders`
- 销量榜：`GET /api/dishes/top-sales`（Top 10）
- 评分榜：`GET /api/dishes/top-rated`

### Agent 对话 — `/api/agent`

| 工具 | 说明 |
|------|------|
| `query_dishes` | 关键词 / 分类查菜 |
| `query_dishes_sales_rank` | 销量排行，`limit` 默认 10、最大 20 |
| `semantic_search_dishes` | RAG 语义检索（口味、场景） |
| `query_orders` / `query_categories` | 查单、查分类 |
| `create_order` | 按菜名 + 数量下单 |

- 多轮记忆：`chat_history`，默认最近 10 条  
- RAG 注入：`rag.inject-to-agent-prompt=true` 时在 Prompt 附带相关菜品  
- 兜底：LLM 未返回工具时，本地解析「麻婆豆腐 三份」等下单意图  

### RAG — `/api/rag`

- 启动后 `DataInitializer` 自动索引 8 道示例菜  
- `POST /api/rag/reindex` 全量重建向量  
- 详见 [向量库与 RAG 技术架构](#向量库与-rag-技术架构)

### 飞书（可选）— `/api/feishu/webhook`

- 公网 HTTPS + 订阅 `im.message.receive_v1`  
- 会话 ID：`feishu:{chat_id}`；指令 `/help`、`/clear`  

### 其他

- `/api/ai/*`：单次 DeepSeek 解析/推荐（不经 Agent 会话）  
- `/api/logs`：操作日志，响应头 `X-Trace-Id`  

---

## 向量库与 RAG 技术架构

本项目**不依赖** Milvus / pgvector，采用 **H2 表 `dish_embedding` + 进程内余弦相似度** 的轻量向量库；向量化由**火山方舟**完成。

### 架构总览

```text
┌──────────────┐   embed (Ark)    ┌─────────────────────────────────────┐
│ dish (H2)    │ ───────────────► │ 火山方舟                             │
│ 业务数据      │                  │ · 文本: POST .../embeddings          │
└──────┬───────┘                  │ · 视觉: POST .../embeddings/multimodal│
       │                          │   model = ep-xxx, Bearer ARK_API_KEY  │
       │ DishVectorIndexService   └──────────────────┬──────────────────┘
       ▼                                             │ float[] (如 2048 维)
┌──────────────────────────────────────────────────┴──────────────────┐
│ VectorStoreService                                                     │
│  · 持久化 dish_embedding (embedding_json, dimension, content_hash)      │
│  · 内存 ConcurrentHashMap → cosine similarity → Top-K                  │
└──────────────────────────────┬────────────────────────────────────────┘
                               ▼
                    RagService → Agent Prompt / semantic_search_dishes
```

### 接入点类型

| 控制台模型类型 | `AI_EMBEDDING_MULTIMODAL` | API 路径 |
|----------------|---------------------------|----------|
| 文本 Embedding | `false` | `/api/v3/embeddings` |
| doubao-embedding-vision 等 | **`true`** | `/api/v3/embeddings/multimodal` |

vision 接入点走普通 `/embeddings` 会报错：`model does not support this api`。

### 核心组件

| 组件 | 职责 |
|------|------|
| `DoubaoArkEmbeddingClient` | 方舟 HTTP 调用与多模态响应解析 |
| `EmbeddingService` | 仅 `doubao-ark`；可选 `fallback-local` |
| `DishVectorIndexService` | 建索引 / `reindexAll` / 菜品变更增量索引 |
| `VectorStoreService` | upsert、reload、similaritySearch |
| `RagService` | 检索、格式化、注入 Agent |

### 配置项

| 变量 | 说明 |
|------|------|
| `ARK_API_KEY` | 方舟 API Key（`ark-...`，勿用展示用 `apikey-`  ID） |
| `AI_EMBEDDING_MODEL` | 接入点 ID，如 `ep-20260526155213-4gjqx` |
| `AI_EMBEDDING_MULTIMODAL` | vision 接入点设为 `true` |
| `AI_EMBEDDING_FALLBACK_LOCAL` | 默认 `false`；API 失败时本地哈希向量（仅调试） |
| `rag.top-k` / `rag.min-score` | 默认 5 / 0.35 |

### 验证

```bash
curl -s http://localhost:8080/api/rag/status
# embeddingConfigured, embeddingEndpoint, embeddingMultimodal, indexedCount

curl -s -X POST http://localhost:8080/api/rag/reindex
curl -s "http://localhost:8080/api/rag/search/text?q=辣的下饭"
```

---

## 配置参考

### DeepSeek（Agent）

```bash
export AI_DEEPSEEK_API_KEY=你的密钥
export AI_DEEPSEEK_MODEL=deepseek-chat
export AGENT_SIMULATION_MODE=false    # true = 无 Key 时本地关键词模拟
```

### 火山方舟 Embedding（RAG）

```bash
export ARK_API_KEY=ark-你的完整密钥
export AI_EMBEDDING_MODEL=ep-你的接入点
export AI_EMBEDDING_MULTIMODAL=true     # doubao-embedding-vision 必填
export AI_EMBEDDING_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
export AI_EMBEDDING_FALLBACK_LOCAL=false
```

`application-local.yml` 示例见 [application-local.yml.example](./src/main/resources/application-local.yml.example)。

### 飞书（可选）

```bash
export FEISHU_ENABLED=true
export FEISHU_APP_ID=cli_xxxx
export FEISHU_APP_SECRET=xxxx
export FEISHU_VERIFICATION_TOKEN=xxxx
export FEISHU_ENCRYPT_KEY=xxxx
```

Webhook：`https://<公网域名>/api/feishu/webhook`

---

## API 速查

```bash
# Agent
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"有什么辣的菜推荐？"}'

# RAG
curl -s http://localhost:8080/api/rag/status
curl -s -X POST http://localhost:8080/api/rag/reindex
curl -s "http://localhost:8080/api/rag/search/text?q=辣的下饭"

# 销量 REST
curl -s http://localhost:8080/api/dishes/top-sales

# 日志
curl -s "http://localhost:8080/api/logs?module=AGENT&page=0&size=20"
```

完整端点见 [TECH_ARCHITECTURE.md §6](./TECH_ARCHITECTURE.md#6-api-端点汇总)。

---

## 项目结构

```text
ai-ordering-agent/
├── .cursor/skills/              # Cursor Skills（ai-ordering-dev）
├── frontend/                    # React 聊天 UI
├── src/main/java/.../ordering/
│   ├── agent/                   # AgentServiceImpl + 6 Tools
│   ├── embedding/               # DoubaoArkEmbeddingClient
│   ├── feishu/                  # Webhook（条件装配）
│   ├── service/                 # RAG、业务、ChatMemory
│   └── controller/
├── src/main/resources/
│   ├── application.yml          # 默认配置（无密钥）
│   └── application-local.yml.example
├── .env.example
├── README.md
└── TECH_ARCHITECTURE.md
```

---

## 测试数据

启动后加载 3 个分类、8 道示例菜（含 `sales_count`）。辣味检索可命中麻婆豆腐、鱼香肉丝等；销量 Top 3 约为宫保鸡丁、麻婆豆腐、鱼香肉丝。

---

## 常见问题

| 现象 | 处理 |
|------|------|
| Agent 回复像模板 | 检查 `AI_DEEPSEEK_API_KEY`；或误开 `AGENT_SIMULATION_MODE=true` |
| `API key format is incorrect` | 使用方舟控制台 **API Key 管理** 中 `ark-` 完整密钥 |
| `model does not support this api` | vision 接入点需 `AI_EMBEDDING_MULTIMODAL=true` |
| 语义搜不到菜 | `POST /api/rag/reindex`；必要时调低 `rag.min-score` |
| `indexedCount=0` | 配置方舟后执行 reindex；看 `/api/rag/status` 的 `embeddingConfigured` |
| 飞书无回复 | `FEISHU_ENABLED=true`、应用发布、HTTPS、事件订阅 |
| 前端 502 | 先启动后端 `8080`，再 `npm run dev` |

---

## License

MIT License
