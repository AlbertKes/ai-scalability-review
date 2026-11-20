# Run Cost & Latency — wonder-cart-service

**Job**: fdfeb85e-56dc-4b4c-a4d7-69760273bc97

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.8-flash | 364 | 3646901 | 32248 | 3110404 | 96 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 2 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.5-flash-lite | 32 | 213745 | 5003 | 134975 | 10 | 7 |
| STAGE_4_SYNTHESIS | gemini-3.8-flash | 225 | 233523 | 41429 | 125135 | 9 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.8-flash | 248 | 447340 | 38258 | 358558 | 9 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.8-flash | 173 | 603692 | 29182 | 511242 | 17 | 0 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.8-flash | 324 | 2345638 | 42046 | 2119162 | 44 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 7679005 (budget 20000000)
**Stage time**: 1371 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 915 s (budget 1800 s)
**MCP tool calls**: 7
**Within budget**: yes
