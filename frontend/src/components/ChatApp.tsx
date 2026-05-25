import { useCallback, useEffect, useRef, useState } from 'react';
import {
  clearSession,
  getSessionMessages,
  sendChat,
  type ChatMessage,
} from '../api/agent';
import './ChatApp.css';

const SESSION_KEY = 'ai-ordering-session-id';

function getOrCreateSessionId(): string {
  let id = localStorage.getItem(SESSION_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(SESSION_KEY, id);
  }
  return id;
}

function displayRole(role: string): string {
  if (role === 'user') return '你';
  if (role === 'assistant') return '点餐助手';
  if (role === 'tool') return '工具';
  return role;
}

function toDisplayMessages(messages: ChatMessage[]) {
  return messages
    .filter((m) => m.role === 'user' || m.role === 'assistant')
    .map((m) => ({
      role: m.role,
      content: m.content ?? '',
      createdAt: m.createdAt,
    }));
}

export default function ChatApp() {
  const [sessionId] = useState(getOrCreateSessionId);
  const [messages, setMessages] = useState<
    { role: string; content: string; createdAt?: string }[]
  >([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = useCallback(() => {
    requestAnimationFrame(() => {
      if (listRef.current) {
        listRef.current.scrollTop = listRef.current.scrollHeight;
      }
    });
  }, []);

  const loadHistory = useCallback(async () => {
    try {
      const history = await getSessionMessages(sessionId);
      setMessages(toDisplayMessages(history));
    } catch {
      /* 新会话无历史 */
    }
  }, [sessionId]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading, scrollToBottom]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;

    setInput('');
    setError(null);
    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setLoading(true);

    try {
      const reply = await sendChat(sessionId, text);
      setMessages((prev) => [...prev, { role: 'assistant', content: reply }]);
    } catch (e) {
      const msg = e instanceof Error ? e.message : '发送失败';
      setError(msg);
      setMessages((prev) => prev.slice(0, -1));
    } finally {
      setLoading(false);
    }
  };

  const handleClear = async () => {
    if (!confirm('确定要清除当前对话吗？')) return;
    setLoading(true);
    setError(null);
    try {
      await clearSession(sessionId);
      setMessages([]);
      const newId = crypto.randomUUID();
      localStorage.setItem(SESSION_KEY, newId);
      window.location.reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : '清除失败');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="chat-app">
      <header className="chat-header">
        <div className="chat-header__brand">
          <span className="chat-header__icon">🍜</span>
          <div>
            <h1>智能点餐助手</h1>
            <p>基于 Agent 的多轮对话 · 支持菜品/订单查询</p>
          </div>
        </div>
        <div className="chat-header__actions">
          <span className="session-badge" title={sessionId}>
            会话 {sessionId.slice(0, 8)}…
          </span>
          <button
            type="button"
            className="btn btn--ghost"
            onClick={handleClear}
            disabled={loading}
          >
            新对话
          </button>
        </div>
      </header>

      <main className="chat-main" ref={listRef}>
        {messages.length === 0 && !loading && (
          <div className="chat-empty">
            <p>你好！我是智能点餐助手，可以帮你：</p>
            <ul>
              <li>推荐菜品、按口味筛选</li>
              <li>查询菜单与分类</li>
              <li>查询订单状态</li>
            </ul>
            <p className="chat-empty__hint">试试：「有什么辣的菜推荐？」</p>
          </div>
        )}

        {messages.map((msg, i) => (
          <div
            key={`${msg.role}-${i}-${msg.content.slice(0, 20)}`}
            className={`chat-bubble chat-bubble--${msg.role}`}
          >
            <div className="chat-bubble__label">{displayRole(msg.role)}</div>
            <div className="chat-bubble__content">{msg.content}</div>
          </div>
        ))}

        {loading && (
          <div className="chat-bubble chat-bubble--assistant">
            <div className="chat-bubble__label">点餐助手</div>
            <div className="chat-bubble__content chat-bubble__typing">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        )}
      </main>

      {error && <div className="chat-error">{error}</div>}

      <footer className="chat-footer">
        <textarea
          className="chat-input"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行…"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={2}
          disabled={loading}
        />
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleSend}
          disabled={loading || !input.trim()}
        >
          发送
        </button>
      </footer>
    </div>
  );
}
