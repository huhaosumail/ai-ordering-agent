# 点餐小助手前端

React 聊天界面，开发态通过 Vite 将 `/api` 代理到后端 `http://localhost:8080`。

> 全栈说明、方舟 RAG、飞书配置见仓库根目录 [README.md](../README.md)。

---

## 技术栈

| 项 | 说明 |
|----|------|
| 框架 | React 19 + TypeScript |
| 构建 | Vite 8 |
| 主界面 | `src/components/ChatApp.tsx` |
| API | `src/api/agent.ts` → `POST /api/agent/chat` |

---

## 开发

### 前置条件

1. 后端已启动：`mvn spring-boot:run -Dspring-boot.run.profiles=local`（端口 `8080`）  
2. 已配置 `AI_DEEPSEEK_API_KEY`（或 `application-local.yml`），否则 Agent 可能走模拟模式  
3. RAG 可选：配置 `ARK_API_KEY` + `AI_EMBEDDING_MODEL` 后，后端语义检索更准确（与前端无直接配置）

### 安装与启动

```bash
cd frontend
npm install
npm run dev
```

浏览器：**http://localhost:5173**

### 代理

`vite.config.ts` 将 `/api` 转发到 `http://localhost:8080`，无需额外 CORS。

---

## 会话行为

- `sessionId` 存于 `localStorage`，多轮对话由后端 `chat_history` 维护  
- 清空：刷新页面生成新 session，或调用 `DELETE /api/agent/session/{id}`  

示例：

```bash
curl -X DELETE http://localhost:8080/api/agent/session/demo-1
```

---

## 构建与 Docker

```bash
npm run build    # 产出 dist/
```

根目录 `docker compose up` 构建本目录 Dockerfile，nginx 对外 **3000**，反代后端 `backend:8080`。

---

## 调试建议

| 问题 | 排查 |
|------|------|
| 网络错误 | 确认后端 `8080` 已启动 |
| 回复呆板 | 检查 DeepSeek Key / `AGENT_SIMULATION_MODE` |
| 推荐不准 | 后端 RAG：`curl /api/rag/status` 与 `reindex` |

---

## 相关文档

- [../README.md](../README.md)  
- [../TECH_ARCHITECTURE.md](../TECH_ARCHITECTURE.md)  
