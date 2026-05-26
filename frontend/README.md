# 点餐小助手前端

本目录为 **AI Ordering Agent** 的 React 聊天界面，通过 Vite 开发服务器将 `/api` 代理到后端 `http://localhost:8080`。

> 项目总览与后端配置见仓库根目录 [README.md](../README.md)。

---

## 技术栈

- React 19 + TypeScript  
- Vite 8  
- 主要页面：`src/components/ChatApp.tsx`  
- API 封装：`src/api/agent.ts`  

---

## 开发

### 前置条件

后端已启动（默认 `8080`），且已配置 `AI_DEEPSEEK_API_KEY`（或 `SPRING_PROFILES_ACTIVE=local`）。

### 安装与启动

```bash
cd frontend
npm install
npm run dev
```

浏览器打开：**http://localhost:5173**

### 代理

`vite.config.ts` 将 `/api` 转发到 `http://localhost:8080`，无需在前端配置 CORS。

---

## 构建与 Docker

```bash
npm run build          # 产出 dist/
```

仓库根目录 `docker compose up` 会使用 `frontend/Dockerfile` + nginx，对外端口 **3000**。

---

## 与 Agent 的交互

- 使用 `localStorage` 保存 `sessionId`，多轮对话走后端 `POST /api/agent/chat`  
- 清空会话可刷新页面或调用后端 `DELETE /api/agent/session/{id}`  

---

## 相关文档

- [../README.md](../README.md)  
- [../TECH_ARCHITECTURE.md](../TECH_ARCHITECTURE.md)  
