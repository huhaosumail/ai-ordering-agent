# 项目 Cursor Skills

本目录存放**仓库级** Agent Skills，克隆项目后 Cursor 可自动发现（路径：`.cursor/skills/<skill-name>/SKILL.md`）。

| Skill | 说明 |
|-------|------|
| [ai-ordering-dev](./ai-ordering-dev/SKILL.md) | 在本仓库内开发、调试 Agent / RAG / 飞书时的约定与命令 |

## 如何新增 Skill

1. 新建目录：`.cursor/skills/<skill-name>/`
2. 添加 `SKILL.md`（YAML frontmatter + 正文），参考 [create-skill](https://cursor.com/docs) 或仓库内 `ai-ordering-dev` 示例
3. 在本表登记一行说明

个人全局 Skill 请放在 `~/.cursor/skills/`，不要写入 `~/.cursor/skills-cursor/`。
