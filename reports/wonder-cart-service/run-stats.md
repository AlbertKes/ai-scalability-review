# Run Cost & Latency — wonder-cart-service

**Job**: 7fe151d1-5e29-4460-9591-b59accc64699

| Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| STAGE_1_CODE_ANALYSIS | gemini-3.5-flash | 154 | 1324287 | 19748 | 1159883 | 34 | 0 |
| STAGE_2_INFRA_CONTEXT | none (local) | 3 | 0 | 0 | 0 | 0 | 0 |
| STAGE_3_METRIC_COLLECTION | gemini-3.1-flash-lite | 60 | 371502 | 9391 | 221510 | 15 | 12 |
| STAGE_4_SYNTHESIS | gemini-3.5-flash | 212 | 960029 | 47034 | 772223 | 14 | 0 |
| STAGE_5_VALIDATION | gemini-3.5-flash | 417 | 3931963 | 50881 | 3155521 | 39 | 8 |

**Total tokens**: 6714835 (budget 1500000)
**Total duration**: 848 s (budget 900 s)
**MCP tool calls**: 20
**Within budget**: NO
