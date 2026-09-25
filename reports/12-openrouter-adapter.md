# OpenRouter Adapter

## Wire contract used

The adapter targets the documented `POST /api/v1/chat/completions` endpoint with:

- `Authorization: Bearer <key>`;
- `Content-Type: application/json`;
- `Accept: text/event-stream`;
- `model`, a single user message, and `stream: true` in the request body;
- SSE `data:` records containing `choices[0].delta.content`;
- `[DONE]` as the terminal stream sentinel;
- optional `id`, `model`, `finish_reason`, and `usage` metadata.

Official references consulted on 2026-09-25:

- [OpenRouter quickstart](https://openrouter.ai/docs/quickstart)
- [Create a chat completion](https://openrouter.ai/docs/api/api-reference/chat/send-chat-completion-request)
- [List all models](https://openrouter.ai/docs/api/api-reference/models/get-models)

## Boundary

`OpenRouterCodec` owns OpenRouter JSON/SSE parsing. `OpenRouterProviderAdapter` owns HTTP, authorization, cancellation, resource closure, and error translation. `ConversationCoordinator` sees only `ProviderRequest`, `StreamEvent`, `ProviderError`, and optional `ProviderMetadata`.

The adapter allocates no conversation, turn, or message IDs; does not write persistence; and does not mutate UI state.

## Confirmed

Local SSE fixtures cover incremental deltas, `[DONE]`, 401 authentication mapping, malformed SSE protocol failure, disconnected-stream network failure, cancellation, repeated cancellation, and missing-credential short-circuiting.

## Not tested

Live OpenRouter model streaming, provider model availability, usage semantics from a live response, network cancellation against OpenRouter, and provider routing behavior require a user-owned credential and a connected runtime.
