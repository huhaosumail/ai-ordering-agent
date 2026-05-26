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
| 向量模型 | 豆包 bge-m3 / 火山方舟（可选） | `ai.embedding.*` |
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

### 2. 配置密钥（勿提交 Git）

复制 [.env.example](./.env.example)，或启用本地配置：

```bash
cp .env.example .env   # 自行填入，.env 已 gitignore

# 推荐：本地 profile（见 application-local.yml，已 gitignore）
export SPRING_PROFILES_ACTIVE=local
export AI_DEEPSEEK_API_KEY=你的DeepSeek密钥
```

`application-local.yml` 可集中填写 DeepSeek、方舟、飞书等密钥，详见 [.env.example](./.env.example)。

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
export AI_DEEPSEEK_API_KEY=你的密钥
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

- 启动后自动索引示例菜品；`POST /api/rag/reindex` 可重建  
- Embedding：`doubao-bge-m3`（默认）或 `doubao-ark`，与 **DeepSeek 聊天独立**  

### 飞书机器人（可选）— `/api/feishu/webhook`

- 需公网 HTTPS（本地可用 ngrok）  
- 订阅 `im.message.receive_v1`，会话 ID：`feishu:{chat_id}`  
- 快捷指令：`/help`、`/clear`  

### 其他

- `/api/ai/*`：直接调 DeepSeek 解析/推荐（不经 Agent 会话）  
- `/api/logs`：操作日志与 `X-Trace-Id`  

详细配置步骤见下文 [配置参考](#配置参考)；架构图见 [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md)。

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

### RAG / Embedding（与 DeepSeek 无关）

**豆包 bge-m3（VikingDB）**

```bash
export AI_EMBEDDING_PROVIDER=doubao-bge-m3
export VIKINGDB_EMBEDDING_TOKEN=你的Token
export VIKINGDB_EMBEDDING_HOST=https://api-vikingdb.vikingdb.cn-beijing.volces.com
export AI_EMBEDDING_DIMENSIONS=1024
curl -X POST http://localhost:8080/api/rag/reindex
```

**火山方舟**

```bash
export AI_EMBEDDING_PROVIDER=doubao-ark
export ARK_API_KEY=你的方舟Key
export AI_EMBEDDING_MODEL=ep-你的向量化接入点ID
```

`ai.embedding.fallback-local=true` 时 API 失败会降级本地向量，不阻塞启动。

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

### RAG

```bash
curl "http://localhost:8080/api/rag/search/text?q=辣的下饭"
curl -X POST http://localhost:8080/api/rag/reindex
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
│   ├── embedding/           # 豆包/方舟/OpenAI 向量化客户端
│   ├── feishu/              # 飞书 Webhook
│   ├── service/             # RAG、业务、ChatMemory
│   └── controller/
├── src/main/resources/
│   ├── application.yml      # 默认配置（无密钥）
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
| Embedding 失败 | 核对 VikingDB Token / 方舟 `ep-xxx`；或依赖 `fallback-local` |
| 飞书无回复 | `FEISHU_ENABLED=true`、权限发布、公网 HTTPS、@ 机器人 |
| 前端连不上 API | 先启后端，再 `npm run dev` |

---

## License

MIT License
