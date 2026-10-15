# Run Cost & Latency — order-service

**Job**: b7d5468b-6561-48a4-8675-07d7bcb3cf28

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.5-flash | 1162 | 12570318 | 51759 | 11510570 | 127 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 3 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 128 | 1962221 | 27889 | 1507884 | 27 | 12 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 548 | 1687410 | 53062 | 1205654 | 22 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.5-flash | 527 | 210437 | 39814 | 125125 | 5 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.5-flash | 1136 | 6287082 | 78731 | 4824387 | 58 | 13 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.5-flash | 286 | 3683481 | 36660 | 3373371 | 60 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 26688864 (budget 2000000)
**Stage time**: 3794 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 2848 s (budget 1200 s)
**MCP tool calls**: 25
**Within budget**: NO
