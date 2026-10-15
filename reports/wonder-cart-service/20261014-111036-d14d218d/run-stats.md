# Run Cost & Latency — wonder-cart-service

**Job**: d14d218d-2603-4452-8129-7612f1aec15f

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.5-flash | 762 | 9606531 | 54041 | 8705856 | 127 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 4 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 155 | 1440951 | 46997 | 1167692 | 25 | 12 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 326 | 1126845 | 54349 | 798460 | 15 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.5-flash | 245 | 699552 | 35647 | 573999 | 16 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.5-flash | 440 | 5271034 | 50689 | 4927001 | 53 | 13 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.5-flash | 438 | 5181110 | 43005 | 4800221 | 73 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 23610751 (budget 2000000)
**Stage time**: 2372 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 1528 s (budget 1200 s)
**MCP tool calls**: 25
**Within budget**: NO
