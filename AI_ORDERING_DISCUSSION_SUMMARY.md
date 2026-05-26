# 方案讨论摘要（历史记录）

> **当前实现以 [README.md](./README.md) 与 [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) 为准。**  
> 本文档保留 2026 年 5 月前后关于响应式架构与 AI 集成的讨论结论，便于回顾选型原因。

---

## 讨论背景

- **时间**：2026-05-21 ~ 2026-05-22  
- **主题**：响应式 AI 智能点餐系统的设计与实现  
- **现状**：项目已演进为含 **Agent 工具链**、**RAG（bge-m3）**、**飞书机器人** 的完整示例，见 README。

---

## 技术栈（讨论时）

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2 + WebFlux |
| 数据库 | H2 + R2DBC |
| 大模型 | DeepSeek Chat API |
| HTTP | OkHttp |

---

## 核心结论（仍适用）

### 响应式编程的价值

| 特性 | 说明 |
|------|------|
| 高并发 | 少量线程处理大量并发连接 |
| IO 密集 | LLM / Embedding 等待时不占满线程池 |
| 组合能力 | `Mono`/`Flux` 编排多步异步 |
| 背压 | 流式场景可控制消费速度 |

**结论**：单个请求不会因「用了 WebFlux」而更快，但高并发下资源利用更好。

### 为何 AI 场景适合响应式

1. 外部 API（DeepSeek、Embedding）以网络 IO 为主  
2. 未来可对接流式 Token 输出（SSE）  
3. 查库 + 调模型 + 写日志可链式组合  

### 流式输出（概念）

```text
传统：等待完整响应(数秒) → 一次返回
流式：首 Token 很快 → 持续推送 → 结束
```

当前 Agent 实现为**非流式**整段返回；扩展时可改 WebFlux `Flux` 推送。

---

## 已实现功能（讨论后新增）

以下在讨论稿之后已落地，细节见 README：

- Agent 多轮对话与 6 个工具（`query_dishes`、`semantic_search_dishes`、`query_dishes_sales_rank`、`query_orders`、`query_categories`、`create_order`）  
- RAG + `dish_embedding` 向量索引  
- 飞书 `im.message.receive_v1` Webhook  
- 操作日志 `operation_log` + `X-Trace-Id`  
- React 聊天前端、Docker Compose  
- Cursor 项目 Skill：`.cursor/skills/ai-ordering-dev`  

---

## 配置示例（勿写真实密钥）

```yaml
ai:
  deepseek:
    api-key: ${AI_DEEPSEEK_API_KEY:}
    base-url: https://api.deepseek.com/v1
    model: deepseek-chat
```

---

## 延伸阅读

- [README.md](./README.md) — 使用与配置  
- [TECH_ARCHITECTURE.md](./TECH_ARCHITECTURE.md) — 架构与 API  
