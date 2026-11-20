# Run Cost & Latency — ba-auto-ship-service

**Job**: 7d3d276a-0885-46ec-849a-ee942cba2876

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.8-flash | 424 | 4652913 | 43571 | 4026963 | 81 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 4 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.5-flash-lite | 39 | 285169 | 6147 | 236694 | 23 | 20 |
| STAGE_4_SYNTHESIS | gemini-3.8-flash | 286 | 389466 | 53310 | 262615 | 9 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.8-flash | 217 | 370767 | 36057 | 282039 | 8 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.8-flash | 201 | 787014 | 32176 | 588606 | 18 | 0 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.8-flash | 353 | 3561202 | 41894 | 3258862 | 57 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 10259686 (budget 20000000)
**Stage time**: 1526 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 1064 s (budget 1800 s)
**MCP tool calls**: 20
**Within budget**: yes
