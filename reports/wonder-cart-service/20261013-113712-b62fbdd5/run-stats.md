# Run Cost & Latency — wonder-cart-service

**Job**: b62fbdd5-45f1-4715-a491-004b7445e784

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.5-flash | 259 | 2113060 | 23431 | 1790771 | 42 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 3 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 73 | 316723 | 11180 | 246251 | 11 | 6 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 259 | 802931 | 48345 | 576504 | 16 | 0 |
| STAGE_5_VALIDATION | gemini-3.5-flash | 547 | 6041804 | 45287 | 5320235 | 61 | 22 |

**Total tokens**: 9402761 (budget 2000000)
**Stage time**: 1142 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 1066 s (budget 1200 s)
**MCP tool calls**: 28
**Within budget**: NO
