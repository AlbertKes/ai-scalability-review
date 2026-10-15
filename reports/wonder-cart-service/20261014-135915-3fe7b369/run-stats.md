# Run Cost & Latency — wonder-cart-service

**Job**: 3fe7b369-f029-4e77-ae24-f5f970e29956

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_2_INFRA_CONTEXT | none (local) | 3 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 130 | 2417073 | 31228 | 2059885 | 32 | 14 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 483 | 1501079 | 54963 | 1355079 | 19 | 0 |

**Total tokens**: 4004343 (budget 2000000)
**Stage time**: 617 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 617 s (budget 1200 s)
**MCP tool calls**: 14
**Within budget**: NO
