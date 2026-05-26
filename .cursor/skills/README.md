# 项目 Cursor Skills

仓库级 Agent Skills，路径 `.cursor/skills/<name>/SKILL.md`，克隆后 Cursor 可自动发现。

| Skill | 说明 |
|-------|------|
| [ai-ordering-dev](./ai-ordering-dev/SKILL.md) | 本仓库后端/RAG/Agent/飞书开发与调试（6 工具、方舟 multimodal） |

## 新增 Skill

1. 新建 `.cursor/skills/<skill-name>/SKILL.md`（含 YAML frontmatter）  
2. 参考 `ai-ordering-dev` 或 [Cursor 文档](https://cursor.com/docs)  
3. 在本表增加一行  

个人全局 Skill：`~/.cursor/skills/`（勿写 `~/.cursor/skills-cursor/`）。
