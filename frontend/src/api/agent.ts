const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
}

export interface ChatMessage {
  role: string;
  content?: string;
  toolName?: string;
  toolResult?: string;
  createdAt?: string;
}

export async function sendChat(
  sessionId: string,
  message: string,
  userId?: number
): Promise<string> {
  const res = await fetch(`${API_BASE}/api/agent/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, message, userId }),
  });
  const json: ApiResponse<string> = await res.json();
  if (json.code !== 200 || !json.data) {
    throw new Error(json.message || '请求失败');
  }
  return json.data;
}

export async function getSessionMessages(
  sessionId: string
): Promise<ChatMessage[]> {
  const res = await fetch(
    `${API_BASE}/api/agent/session/${encodeURIComponent(sessionId)}/messages`
  );
  const json: ApiResponse<ChatMessage[]> = await res.json();
  if (json.code !== 200) {
    throw new Error(json.message || '加载历史失败');
  }
  return json.data ?? [];
}

export async function clearSession(sessionId: string): Promise<void> {
  const res = await fetch(
    `${API_BASE}/api/agent/session/${encodeURIComponent(sessionId)}`,
    { method: 'DELETE' }
  );
  const json: ApiResponse<string> = await res.json();
  if (json.code !== 200) {
    throw new Error(json.message || '清除会话失败');
  }
}
