---
name: ai-ordering-dev
description: >-
  Develop the ai-ordering-agent Spring WebFlux project: 6 Agent tools, DeepSeek
  chat, Volcengine Ark embedding (text or multimodal ep-xxx), H2 vector RAG,
  Feishu webhook. Use when editing Java backend, adding tools, or testing
  /api/agent and /api/rag.
---

# AI Ordering Agent 开发指南

## 栈与端口

| 部分 | 说明 |
|------|------|
| 后端 | Java 21, Boot 3.2 WebFlux, R2DBC+H2, `:8080` |
| 前端 | `frontend/`, Vite `:5173`, `/api` → 8080 |
| 对话 | `AgentServiceImpl`, `ai.deepseek.*` |
| 向量 | `DoubaoArkEmbeddingClient`, `dish_embedding`+内存 cosine |
| 飞书 | `feishu.enabled=true`, `POST /api/feishu/webhook` |

## 密钥（勿提交 Git）

```bash
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml

export SPRING_PROFILES_ACTIVE=local
export AI_DEEPSEEK_API_KEY=...
export ARK_API_KEY=ark-...              # 完整 ark- 密钥
export AI_EMBEDDING_MODEL=ep-...
export AI_EMBEDDING_MULTIMODAL=true     # doubao-embedding-vision 必填
```

## Agent 工具（6）

| 工具 | 类 |
|------|-----|
| `query_dishes` | `DishQueryTool` |
| `semantic_search_dishes` | `SemanticDishSearchTool` |
| `query_dishes_sales_rank` | `DishSalesRankTool` |
| `query_orders` | `OrderQueryTool` |
| `query_categories` | `CategoryQueryTool` |
| `create_order` | `CreateOrderTool` |

新增工具：实现 `Tool` → 注册 `AgentServiceImpl` → 更新 `SYSTEM_PROMPT` → 可选模拟/总结兜底。

## 常用命令

```bash
mvn -q compile test
mvn spring-boot:run -Dspring-boot.run.profiles=local

curl -s http://localhost:8080/api/rag/status
curl -s -X POST http://localhost:8080/api/rag/reindex
curl -s "http://localhost:8080/api/rag/search/text?q=辣的下饭"

curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"dev-1","message":"销量最高的菜？"}'
```

## 评估

```bash
mvn test
curl -s -X POST "http://localhost:8080/api/eval/run?suite=all"
```

黄金集：`src/main/resources/eval/`。

## 菜品变更

- 单条：`DishServiceImpl` → `DishVectorIndexService.indexDish`
- 批量：`POST /api/rag/reindex`

## 文档

- [README.md](../../../README.md#向量库与-rag-技术架构)
- [TECH_ARCHITECTURE.md](../../../TECH_ARCHITECTURE.md)

## 禁止

- 勿将 API Key 写入 `application.yml` 并提交
- 勿改 `~/.cursor/skills-cursor/`
