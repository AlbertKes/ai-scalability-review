# Run Cost & Latency — mobile-api

**Job**: b1028b3d-d39b-4fe5-9d6d-97120be0c721

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.5-flash | 1311 | 14851677 | 60071 | 12822464 | 151 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 3 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 198 | 3546005 | 42222 | 2837950 | 46 | 24 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 386 | 1039914 | 52133 | 743824 | 17 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.5-flash | 372 | 981966 | 48361 | 848657 | 18 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.5-flash | 631 | 4306438 | 60617 | 3953857 | 51 | 16 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.5-flash | 463 | 3915065 | 41575 | 3594600 | 53 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 28946044 (budget 2000000)
**Stage time**: 3367 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 2329 s (budget 1200 s)
**MCP tool calls**: 40
**Within budget**: NO
