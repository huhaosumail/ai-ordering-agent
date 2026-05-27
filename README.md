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
| 向量/RAG | 方舟算向量 + `dish_embedding` 存搜 | `ARK_API_KEY` · `ep-xxx` |
| 飞书（可选） | 事件订阅 Webhook | `feishu.enabled=true` |

**典型对话路径**

| 用户说法 | 路径 |
|----------|------|
| 有什么比较辣的菜？ | RAG 注入 + `semantic_search_dishes`（见下文示例 ①） |
| 麻辣香锅两份谢谢 | `create_order`（见下文示例 ②；示例库无此菜会失败） |
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

本项目**不依赖** Milvus / pgvector 等外置向量数据库，而是 **「方舟负责算向量 + 本地 `dish_embedding` 负责存和搜」** 的两层结构。

### 本地向量库 vs 火山方舟：各自做什么？

很多人会把 `dish_embedding` 和「向量大模型」混在一起，其实职责不同：

| 角色 | 是什么 | 在本项目里 | 类比 |
|------|--------|------------|------|
| **火山方舟 Embedding** | 云端**向量化模型**（把文字变成一串数字） | 调用 `ep-xxx` 接入点，返回 `float[]`（如 2048 维） | 「翻译官」：把「辣的下饭」译成坐标 |
| **`dish` 表** | 业务数据（菜名、描述、价格等） | 人类可读的菜品主数据 | 菜单正文 |
| **`dish_embedding` 表** | **向量仓库**（只存已算好的向量） | `embedding_json` + 内存 Map，查询时做余弦相似度 | 「图书馆索引卡」：存坐标，按距离找相近 |

**方舟在什么时候被调用？**

1. **建索引（写库）**：每道菜拼一段索引文本 → 调方舟 → 得到向量 → 写入 `dish_embedding`（`POST /api/rag/reindex` 或启动时自动执行）。  
2. **检索（读库）**：用户问题同样调方舟 → 得到 query 向量 → 与库里各菜向量算 cosine → 取 Top-K。

因此：**方舟不是向量数据库**，也不会替你持久化或检索；**`dish_embedding` 不是大模型**，里面没有语义理解能力，只有数字和相似度计算。二者必须配套使用，且索引与查询最好用**同一套**方舟接入点，向量才在同一语义空间里可比。

**DeepSeek** 仍只负责 Agent **对话与工具决策**，与方舟 Embedding **密钥、计费、接口均独立**。

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

## 评估体系

项目内置 **黄金集（Golden Set）+ 指标计算 + HTTP 触发 + JUnit**，用来回答两类问题：

- 改完 RAG / 换方舟接入点后，**该搜到的菜还能搜到吗？**
- 模拟模式下，**用户说法能否路由到正确的 Agent 工具？**

（**不**自动评估 DeepSeek 真实对话质量，那属于 LLM 评测，需另接 Judge 或人工。）

### 黄金集整体架构

```text
┌─────────────────────────────────────────────────────────────────────────┐
│  classpath:eval/                                                         │
│    rag-golden.json          agent-intent-golden.json                     │
│    (query + 期望菜品)        (用户话术 + 期望工具名)                          │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ EvalDatasetLoader 启动时加载
                                ▼
                    ┌───────────────────────┐
                    │   EvaluationService    │
                    │  POST /api/eval/run      │
                    └───────────┬─────────────┘
            ┌───────────────────┴───────────────────┐
            ▼                                       ▼
   ┌─────────────────┐                   ┌──────────────────────┐
   │ RagEvaluation    │                   │ AgentIntentEvaluation │
   │ Runner           │                   │ Runner                │
   └────────┬─────────┘                   └──────────┬───────────┘
            │ 可选先 reindexAll                     │
            ▼                                       ▼
   RagService.retrieve(query)            AgentIntentMatcher.match(message)
            │                                       │
            ▼                                       ▼
   对比 expectedDishes                    对比 expectedTool
   算 Hit@K / Recall@K / MRR              算 passRate（准确率）
            │                                       │
            └───────────────────┬───────────────────┘
                                ▼
                         EvalReport
                    (rag 套件 + agent-intent 套件)
```

**数据流（RAG 单条用例）**

```text
用例: query="辣的下饭", expectedDishes=["麻婆豆腐","鱼香肉丝",...]
  → embed(query)           # 仍走方舟（或测试时 fallback-local）
  → similaritySearch     # 读 dish_embedding + 内存索引
  → 得到 Top-K 菜名列表
  → EvalMetrics 判断：是否命中、召回率、MRR
```

**数据流（Agent 意图单条用例）**

```text
用例: message="销量最高的菜", expectedTool="query_dishes_sales_rank"
  → AgentIntentMatcher.match()   # 与模拟模式同一套规则
  → 实际工具名 == 期望工具名 → pass/fail
```

### 评估范围

| 套件 | 数据集 | 指标 | 测的是什么 |
|------|--------|------|------------|
| **rag** | `eval/rag-golden.json` | Hit@K、Recall@K、MRR | **检索链路**：方舟 embed + `dish_embedding` 相似度 |
| **agent-intent** | `eval/agent-intent-golden.json` | passRate（准确率） | **规则路由**：`AgentIntentMatcher`（非 DeepSeek） |

### 黄金集文件格式（示例）

**RAG**（`rag-golden.json`）：

```json
{
  "id": "rag-spicy",
  "query": "辣的下饭",
  "expectedDishes": ["麻婆豆腐", "鱼香肉丝", "宫保鸡丁"],
  "topK": 5,
  "minRecall": 0.33
}
```

**Agent 意图**（`agent-intent-golden.json`）：

```json
{
  "id": "intent-sales",
  "message": "销量最高的菜有哪些",
  "expectedTool": "query_dishes_sales_rank"
}
```

### 运行方式

```bash
# 全量（RAG 会先 reindex）
curl -s -X POST "http://localhost:8080/api/eval/run?suite=all" | jq .

# 仅 RAG / 仅意图
curl -s -X POST "http://localhost:8080/api/eval/run?suite=rag&reindex=true"
curl -s -X POST "http://localhost:8080/api/eval/run?suite=agent-intent"

curl -s http://localhost:8080/api/eval/info
```

```bash
# CI / 本地单元与集成测试
mvn test
```

关闭评估 API：`EVAL_ENABLED=false`。

### 扩展黄金集

编辑 `src/main/resources/eval/*.json`，新增用例字段：

- RAG：`id`、`query`、`expectedDishes`、`topK`、`minRecall`
- Agent：`id`、`message`、`expectedTool`

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
│   ├── evaluation/              # 评估指标、Runner、黄金集加载
│   ├── service/                 # RAG、业务、ChatMemory
│   └── controller/
├── src/main/resources/
│   ├── eval/                    # rag / agent-intent 黄金集 JSON
│   ├── application.yml          # 默认配置（无密钥）
│   └── application-local.yml.example
├── .env.example
├── README.md
└── TECH_ARCHITECTURE.md
```

---

## 端到端运行示例（两条用户话）

以下假设：后端已启动、`POST /api/agent/chat`，`sessionId=demo-1`。飞书场景只是把请求换成 `feishu:{chat_id}`，**Agent 内部链路相同**。

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"有什么比较辣的菜"}'
```

---

### 示例 ①：「有什么比较辣的菜」— 查菜 / 语义推荐

**用户意图**：按口味模糊找菜，不是要下单。

#### 第 0 步：请求进入系统

```text
浏览器/飞书 → POST /api/agent/chat
  → OperationLogWebFilter 记日志、生成 X-Trace-Id
  → AgentController → AgentServiceImpl.chat(sessionId, message, userId)
```

#### 第 1 步：写入对话记忆

```text
ChatMemoryService.addUserMessage   → 表 chat_history 插入 role=user
ChatMemoryService.getMessagesForLlm → 取该 session 最近 10 条（首轮仅当前句）
```

#### 第 2 步：RAG 预检索（在调 DeepSeek 之前）

与向量库、方舟的关系：

```text
用户句「有什么比较辣的菜」
  → RagService.buildAgentContext(currentInput)
       → EmbeddingService.embed(query)     ← 火山方舟：问题 → 向量
       → VectorStoreService.similaritySearch ← dish_embedding + 内存：比相似度
       → 格式化为【RAG 检索到的相关菜品…】文本块
```

此步**只把参考信息写进 Prompt**，还不等于已调用 Agent 工具。默认 `rag.inject-to-agent-prompt=true`。

#### 第 3 步：组装 Prompt

```text
assemblePrompt =
  SYSTEM_PROMPT（6 个工具说明）
  + RAG 上下文（若有）
  + 对话历史
  + 「用户最新提问：有什么比较辣的菜」
  + 要求输出 <function …> 或直接回答
```

#### 第 4 步：大模型决策（DeepSeek 或模拟）

| 模式 | 行为 |
|------|------|
| **DeepSeek**（`AGENT_SIMULATION_MODE=false` 且 Key 有效） | HTTP `POST …/chat/completions`，由模型根据 Prompt 输出工具调用 |
| **模拟**（无 Key / 显式开启模拟 / API 失败降级） | `AgentIntentMatcher`：句中含 **「辣」** → 固定返回 `semantic_search_dishes` |

典型工具调用：

```text
<function name="semantic_search_dishes" params='{"query":"有什么比较辣的菜"}'>
```

#### 第 5 步：执行工具 `semantic_search_dishes`

```text
SemanticDishSearchTool.execute
  → RagService.formatSearchResult(query)   ← 再次 embed + 检索（与第 2 步同源逻辑）
  → 返回文本，例如：
     「语义检索…共 N 道相关菜品：麻婆豆腐、鱼香肉丝…」
```

示例库里辣味相关常见：**麻婆豆腐**、**鱼香肉丝**、**宫保鸡丁**（含「辣」描述或语义相近）。

#### 第 6 步：第二轮总结

```text
processResponse 发现工具调用
  → executeTools → 结果写入 chat_history（role=tool）
  → 再次 callDeepSeek（或 simulate 的 summarizeToolResults）
  → 生成面向用户的自然语言推荐
  → addAssistantMessage → 返回 JSON 给前端
```

**模拟模式**下若走到 `summarizeToolResults`，会把工具结果里的「找到 N 道菜品」整理成推荐话术。

#### 本例涉及的数据表 / 外部服务

| 环节 | 读写 |
|------|------|
| `chat_history` | 写 user / tool / assistant |
| `dish_embedding` | 只读（检索） |
| 火山方舟 | 调 Embedding（建库时已写过向量，此处是 query 向量） |
| DeepSeek | 调 Chat（正常模式） |
| `dish` / `orders` | 本例**不**下单，不写订单 |

---

### 示例 ②：「麻辣香锅两份谢谢」— 下单

**用户意图**：指定菜名 + 数量，完成下单（「谢谢」为礼貌用语，解析时会去掉）。

#### 第 0～2 步

与示例 ① 相同：进入 `/api/agent/chat`、记日志、写 `chat_history`。

RAG 仍可能跑一遍 `buildAgentContext`（对「麻辣香锅」做语义检索），但**不应替代下单**；最终应以 **create_order** 为准。

#### 第 3～4 步：大模型 / 模拟如何选工具

| 模式 | 预期 |
|------|------|
| **DeepSeek** | 根据 SYSTEM_PROMPT，输出 `create_order`，例如 `items:[{name:"麻辣香锅",quantity:2}]` |
| **模拟** | 先 `OrderIntentParser.tryParse`：需识别「份」+ 菜名。推荐说法 **「麻辣香锅 两份」** 或 **「两份麻辣香锅」**（中间有空格更易命中正则）。**「麻辣香锅两份」无空格时，本地正则可能解析失败**；若失败且句中含「麻辣」，模拟路由可能误走 `semantic_search_dishes`（与下单冲突）——生产环境请用 DeepSeek 或改进解析。 |

若 LLM **未**返回工具，`processResponse` 会用 `orderIntentParser.tryParse(userInput)` **兜底下单**。

#### 第 5 步：执行工具 `create_order`

```text
CreateOrderTool.execute
  → 按 name 在 dish 表模糊匹配（findByNameLike）
  → 组装 OrderRequest（dishId + quantity）
  → OrderService.createOrder
  → 写 orders 表（状态 PENDING、items JSON、总金额）
  → 返回「订单创建成功 / 下单失败：…」
```

**重要：当前示例数据只有 8 道菜**（宫保鸡丁、麻婆豆腐、鱼香肉丝等），**没有「麻辣香锅」**。

因此本句在库里通常会：

```text
下单失败：未找到菜品: 麻辣香锅
```

若要演示成功下单，请说示例库存在的菜，例如：**「麻婆豆腐两份谢谢」**。

#### 第 6 步：结果返回用户

- 成功：`summarizeToolResults` 或 DeepSeek 总结 →「好的，已为您下单！订单号：…」
- 失败：直接返回「下单失败：未找到菜品: 麻辣香锅」

#### 本例涉及的数据表 / 外部服务

| 环节 | 读写 |
|------|------|
| `chat_history` | 写 user / tool / assistant |
| `dish` | 读（按名称匹配 dishId） |
| `orders` | **写** 新订单 |
| `operation_log` | 记 AGENT、下单相关日志 |
| DeepSeek | 正常模式下参与工具选择与总结 |
| RAG / 方舟 | 可能只参与 Prompt 注入，不参与订单落库 |

---

### 两条路径对比（一图看懂）

```text
                    POST /api/agent/chat
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
     「有什么比较辣的菜」           「麻辣香锅两份谢谢」
              │                           │
     RAG embed + 检索（可选注入）    RAG 可能注入（次要）
              │                           │
     DeepSeek / 模拟                 DeepSeek / 解析兜底
              │                           │
     semantic_search_dishes          create_order
              │                           │
     读 dish_embedding               读 dish + 写 orders
              │                           │
     文本推荐列表                     成功/失败订单信息
```

| 维度 | 比较辣的菜 | 麻辣香锅两份 |
|------|------------|--------------|
| 主要工具 | `semantic_search_dishes` | `create_order` |
| 方舟 Embedding | 检索 query + 菜品向量 | 通常仅 RAG 预检索（若有） |
| `dish_embedding` | 核心：算相似度 | 一般不写 |
| 业务表 | 只读菜品 | 写 `orders` |
| 示例库实测 | 可命中辣味菜 | **无麻辣香锅 → 失败** |

更细的模块说明见 [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md)。

---

## 测试数据

启动后加载 3 个分类、8 道示例菜（含 `sales_count`）。辣味检索可命中麻婆豆腐、鱼香肉丝等；销量 Top 3 约为宫保鸡丁、麻婆豆腐、鱼香肉丝。**不含「麻辣香锅」**，下单该菜会提示未找到。

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
| 下单「麻辣香锅」失败 | 示例库无此菜；可改说「麻婆豆腐两份」或先在 `dish` 表新增菜品 |
| 模拟模式把下单当成搜菜 | 句中含「麻辣」会优先语义检索；请用 DeepSeek 或写「麻婆豆腐 两份」 |

---

## License

MIT License
