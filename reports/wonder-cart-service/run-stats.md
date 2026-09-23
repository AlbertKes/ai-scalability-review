# Run Cost & Latency — wonder-cart-service

**Job**: 8b32ebed-8279-464e-8d99-e553d4cbaaa7

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.5-flash | 287 | 2995898 | 22405 | 2670091 | 58 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 74 | 1478248 | 5529 | 1088047 | 15 | 9 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 170 | 521944 | 36819 | 311294 | 9 | 0 |
| STAGE_5_VALIDATION | gemini-3.5-flash | 247 | 1833937 | 41557 | 1407066 | 28 | 18 |

**Total tokens**: 6936337 (budget 1500000)
**Total duration**: 778 s (budget 900 s)
**MCP tool calls**: 27
**Within budget**: NO
