# Run Cost & Latency — wonder-cart-service

**Job**: 492f1620-d850-4ba2-ab1f-b9cadb6a8cce

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.8-flash | 436 | 4777774 | 47342 | 4264271 | 88 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 4 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.5-flash-lite | 38 | 169007 | 5252 | 127142 | 9 | 6 |
| STAGE_4_SYNTHESIS | gemini-3.8-flash | 257 | 187083 | 47112 | 72865 | 9 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.8-flash | 253 | 377516 | 34403 | 285252 | 9 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.8-flash | 150 | 488025 | 26337 | 389942 | 11 | 0 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.8-flash | 324 | 2579782 | 35942 | 2334173 | 50 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 8775575 (budget 2000000)
**Stage time**: 1463 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 1017 s (budget 1200 s)
**MCP tool calls**: 6
**Within budget**: NO
