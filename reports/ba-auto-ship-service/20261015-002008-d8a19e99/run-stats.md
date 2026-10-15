# Run Cost & Latency — ba-auto-ship-service

**Job**: d8a19e99-6664-4b5a-821b-8b1af31d4738

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.5-flash | 723 | 10843666 | 60777 | 9642825 | 137 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 2 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 140 | 2398209 | 21678 | 2099562 | 35 | 13 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 423 | 2260932 | 61253 | 1972869 | 28 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.5-flash | 194 | 369769 | 32391 | 281209 | 8 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.5-flash | 480 | 4708953 | 49080 | 4150400 | 41 | 25 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.5-flash | 587 | 5108124 | 48609 | 4632770 | 65 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 25963441 (budget 2000000)
**Stage time**: 2552 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 1734 s (budget 1200 s)
**MCP tool calls**: 38
**Within budget**: NO
