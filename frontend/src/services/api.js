const API_URL = (import.meta.env?.VITE_API_URL ?? "").replace(/\/$/, "");

export async function api(path, { token, ...options } = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(
      body?.detail ?? body?.message ?? `Request failed (${response.status})`,
    );
  }

  if (response.status === 204) return null;
  return response.json();
}
