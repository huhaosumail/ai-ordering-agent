# 方案讨论摘要（历史记录）

> **现行说明以 [README.md](./README.md) 与 [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) 为准。**  
> 本文保留 2026 年 5 月前后关于响应式架构与 AI 集成的讨论结论，便于回顾「为何这样选型」。

---

## 讨论背景

| 项 | 内容 |
|----|------|
| 时间 | 2026-05-21 ~ 2026-05-22（讨论稿）；此后持续演进至当前 main |
| 主题 | 响应式 AI 智能点餐：WebFlux + DeepSeek + 业务 CRUD |
| 现状 | Agent 6 工具、火山方舟 RAG（H2 向量表）、飞书 Webhook、React 前端 |

---

## 技术栈（讨论时 vs 现在）

| 组件 | 讨论稿 | 当前实现 |
|------|--------|----------|
| 框架 | Spring Boot 3.2 WebFlux | 同左 |
| 数据库 | H2 + R2DBC | 同左 + `dish_embedding` |
| 对话 LLM | DeepSeek | 同左 |
| 向量 | （未定型） | **火山方舟 Ark**，支持 `/embeddings/multimodal` |
| 前端 | — | React 19 + Vite 8 |
| 机器人 | — | 飞书可选 |

---

## 核心结论（仍适用）

### 响应式编程

| 特性 | 说明 |
|------|------|
| 高并发 | 少量线程承载大量连接 |
| IO 密集 | 等待 LLM/Embedding 时不占满线程池 |
| 组合 | `Mono`/`Flux` 链式编排查库、调 API、写日志 |

单请求延迟不会因 WebFlux 单独变快；优势在**并发资源利用**与**统一异步模型**。

### 为何 AI 场景适合响应式

1. 外部 API 以网络 IO 为主（DeepSeek、方舟）  
2. 未来可接 SSE 流式 Token  
3. 查库 + 调模型 + 记日志可一条 Reactive 链完成  

当前 Agent 为**非流式**整段返回；扩展时可 `Flux<String>` 推送。

---

## 讨论后已落地能力

- **Agent**：`AgentServiceImpl` + 6 个 Tool（含 `semantic_search_dishes`、`query_dishes_sales_rank`）  
- **RAG**：`dish_embedding` + 内存余弦检索；方舟 `ep-xxx`；vision 接入点需 `multimodal=true`  
- **飞书**：`im.message.receive_v1`，会话 `feishu:{chat_id}`  
- **可观测**：`operation_log`、`X-Trace-Id`  
- **工程化**：Docker Compose、Cursor Skill `ai-ordering-dev`、文档体系（README / TECH_ARCHITECTURE）  

---

## 配置示例（占位符，勿提交真实密钥）

```yaml
ai:
  deepseek:
    api-key: ${AI_DEEPSEEK_API_KEY:}
    base-url: https://api.deepseek.com/v1
    model: deepseek-chat
  embedding:
    provider: doubao-ark
    api-key: ${ARK_API_KEY:}
    model: ${AI_EMBEDDING_MODEL:}
    multimodal: ${AI_EMBEDDING_MULTIMODAL:false}
    fallback-local: false
```

---

## 延伸阅读

- [README.md](./README.md) — 快速开始、向量库架构、FAQ  
- [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) — 模块与 API  
- [frontend/README.md](./frontend/README.md) — 前端开发  
