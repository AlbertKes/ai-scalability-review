# Run Cost & Latency — wonder-cart-service

**Job**: fede24ad-5616-4082-b31a-938b3a435d94

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.8-flash | 288 | 2435349 | 34315 | 2150491 | 69 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 4 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.5-flash-lite | 42 | 269580 | 5436 | 193357 | 13 | 9 |
| STAGE_4_SYNTHESIS | gemini-3.8-flash | 231 | 177523 | 46294 | 68785 | 7 | 0 |
| STAGE_5A_VALIDATION_STATIC | gemini-3.8-flash | 169 | 404510 | 29319 | 316968 | 10 | 0 |
| STAGE_5B_VALIDATION_DATA | gemini-3.8-flash | 164 | 675761 | 26447 | 571280 | 15 | 0 |
| STAGE_5C_VALIDATION_ADVICE | gemini-3.8-flash | 397 | 2514846 | 44472 | 2249594 | 54 | 0 |
| STAGE_5D_VALIDATION_REPORT | none (local) | 0 | 0 | 0 | 0 | 0 | 0 |

**Total tokens**: 6663852 (budget 20000000)
**Stage time**: 1299 s (sum of the stages; they overlap, so this exceeds the wall clock)
**Wall clock**: 918 s (budget 1800 s)
**MCP tool calls**: 9
**Within budget**: yes
