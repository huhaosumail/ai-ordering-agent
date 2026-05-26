---
name: ai-ordering-dev
description: >-
  Develop and debug the ai-ordering-agent Spring WebFlux project: Agent tools,
  DeepSeek chat, RAG with Volcengine Ark embedding (ep-xxx), H2 vector store,
  Feishu webhook, and local secrets. Use when modifying Java backend, adding
  Agent tools, testing /api/agent or /api/rag, or troubleshooting Ark embedding.
---

# AI Ordering Agent 开发指南

## 项目要点

- **后端**：Java 21、Spring Boot 3.2 WebFlux、R2DBC + H2（`8080`）
- **前端**：`frontend/` React + Vite（`5173`，代理 `/api` → 后端）
- **对话**：`AgentServiceImpl` + DeepSeek `ai.deepseek.*`
- **向量/RAG**：火山方舟 `DoubaoArkEmbeddingClient` → `dish_embedding` 表 + 内存余弦检索
- **飞书**：`feishu.enabled=true` 时 `POST /api/feishu/webhook`

## 密钥（勿提交 Git）

| 用途 | 配置 |
|------|------|
| DeepSeek 聊天 | `AI_DEEPSEEK_API_KEY` 或 `application-local.yml` |
| 方舟 Embedding | `ARK_API_KEY` + `AI_EMBEDDING_MODEL=ep-xxx` |
| 飞书 | `FEISHU_*` 环境变量 |

本地启动推荐：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Agent 工具一览

| 工具名 | 类 |
|--------|-----|
| `query_dishes` | `DishQueryTool` |
| `semantic_search_dishes` | `SemanticDishSearchTool` |
| `query_dishes_sales_rank` | `DishSalesRankTool` |
| `query_orders` | `OrderQueryTool` |
| `query_categories` | `CategoryQueryTool` |
| `create_order` | `CreateOrderTool` |

## 常用命令

```bash
mvn -q compile test

curl -s http://localhost:8080/api/rag/status
curl -s -X POST http://localhost:8080/api/rag/reindex
curl -s "http://localhost:8080/api/rag/search/text?q=辣的下饭"

curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"dev-1","message":"有什么辣的菜推荐？"}'
```

## 新增 Agent 工具

1. 在 `agent/tool/` 实现 `Tool` 接口（`getName`、`execute`）
2. 注册到 `AgentServiceImpl` 的 `tools` 列表与构造函数注入
3. 在 `SYSTEM_PROMPT` 中补充工具说明与参数格式
4. 工具调用格式：`<function name="xxx" params='{"key":"value"}'>`

## 修改菜品后

- `DishServiceImpl` 已挂钩 `DishVectorIndexService.indexDish`
- 批量变更后：`POST /api/rag/reindex`

## 文档

- 向量库架构：[README.md](../../../README.md#向量库与-rag-技术架构)
- 模块细节：[TECH_ARCHITECTURE.md](../../../TECH_ARCHITECTURE.md)

## 禁止事项

- 勿将 API Key 写入 `application.yml` 并提交
- 勿修改 `~/.cursor/skills-cursor/`（Cursor 内置目录）
