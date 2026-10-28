import assert from "node:assert/strict";
import test from "node:test";
import { api } from "./api.js";

const originalFetch = globalThis.fetch;

test.afterEach(() => {
  globalThis.fetch = originalFetch;
});

test("sends JSON and bearer authentication", async () => {
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return new Response(JSON.stringify({ id: 1 }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  };

  const result = await api("/api/books", {
    method: "POST",
    token: "test-token",
    body: JSON.stringify({ title: "Testing" }),
  });

  assert.deepEqual(result, { id: 1 });
  assert.equal(request.url, "/api/books");
  assert.equal(request.options.method, "POST");
  assert.equal(request.options.headers["Content-Type"], "application/json");
  assert.equal(request.options.headers.Authorization, "Bearer test-token");
});

test("returns null for successful empty responses", async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 });

  assert.equal(await api("/api/loans/1/return", { method: "PUT" }), null);
});

test("uses the API error detail for failed requests", async () => {
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ detail: "Book is unavailable" }), {
      status: 409,
      headers: { "Content-Type": "application/json" },
    });

  await assert.rejects(
    api("/api/loans/1", { method: "POST" }),
    /Book is unavailable/,
  );
});
