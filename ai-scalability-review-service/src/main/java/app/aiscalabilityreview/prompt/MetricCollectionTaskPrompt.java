package app.aiscalabilityreview.prompt;

public class MetricCollectionTaskPrompt {
    /**
     * Prompt for the Azure half of metric collection. The Datadog half — every number the review
     * scores — is read by the pipeline itself over the REST API (see {@code DatadogMetricsCollector}),
     * because a model copying 28-day series into a file got a third of them wrong. Azure is read here
     * because choosing sub-commands and resource names is a judgement call that code cannot make.
     * Placeholders: {{SERVICE}}, {{ENV}}, {{NAMESPACE}}, {{MYSQL_HOST}}, {{AZURE_RESOURCE_GROUP}},
     * {{AZURE_SUBSCRIPTION}}, {{OUTPUT_PATH}}
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Azure Live State Collection Task

        You are a data collector. Retrieve the live Azure configuration of one service's resources and
        write it into a single data file. You do **not** score anything, you do **not** write an
        assessment and you do **not** produce recommendations — a later stage does that.

        - **Service**: {{SERVICE}}
        - **Environment**: {{ENV}}
        - **Namespace**: {{NAMESPACE}}
        - **MySQL host/cluster**: {{MYSQL_HOST}}  ("N/A" if the service does not use MySQL)

        ## Rules

        1. **Attribution.** Every value carries its source: `[Source: Azure MCP → <tool name>(<args>)]`.
        2. **No approximations.** Write the exact value the tool returned.
        3. **`NOT_COLLECTED` is for tool failures.** Write `NOT_COLLECTED: Azure MCP error — <reason>`
           when a tool returned an error, naming what you attempted. Never guess.
        4. **Be quick.** Make the calls below and nothing more. Do not re-run a call you already ran,
           and do not explore other Azure resources.

        ## What to collect

        Skip entirely only if the service uses no Azure-managed resource.

        ### Naming — read this before the first Azure call

        - **Resource group**: `{{AZURE_RESOURCE_GROUP}}`
        - **Subscription**: `{{AZURE_SUBSCRIPTION}}`

        **`{{ENV}}` is a Datadog tag value, not an Azure name.** The resource group is *not*
        derivable from it and never equals it — the environment tagged `uat` lives in the group
        `FTIUat`, and `prod` lives in `RFProdV2`. Passing `{{ENV}}` as a resource group is the single
        most common failure of this group and produces
        `Resource group '{{ENV}}' could not be found`. Pass the value given above, verbatim.

        If the resource group above reads `unknown`, **discover it, do not guess**: call
        `group_list` once and choose the group whose name matches this environment, then use that
        name for every later call in this group. One extra call is far cheaper than a whole group of
        `NOT_COLLECTED`. Same for a subscription of `unknown` — omit the parameter entirely and let
        the Azure CLI profile default apply; do not invent a GUID.

        Azure MCP server names take the **short** server name. Strip any
        `.mysql.database.azure.com` suffix from `{{MYSQL_HOST}}` before passing it.

        ### Calling convention

        The Azure MCP tools are hierarchical command routers, not flat functions: each call sets
        `command` to a sub-command name and wraps that sub-command's arguments in `parameters`. Use
        exactly the sub-command names below — they are the only ones that exist, and a plausible
        invention like `get_mysql_flexible_server` or `get_aks_node_pools` will simply fail. If a
        name below is ever rejected, call the same tool once with `learn=true` to list the real
        sub-commands rather than guessing a second name.

        ### MySQL Flexible Server (skip if {{MYSQL_HOST}} is N/A)

        - `mysql` / `mysql_server_config_get` with `{server, resource-group, subscription}` —
          returns ServerName, Location, Version, SKU, StorageSizeGB, BackupRetentionDays,
          GeoRedundantBackup. Record the SKU, version and provisioned storage in GB.
        - `mysql` / `mysql_server_param_get` with `{param, server, resource-group, subscription}`,
          once per parameter, for `max_connections`, `slow_query_log` and `long_query_time`.
          `max_connections` is the denominator the next stage scores connection saturation against,
          so it matters more than anything else in this group — it is a per-SKU value in the
          thousands, never something to assume.
        - `mysql` / `mysql_list` with `{resource-group, subscription}` — every server in the group.
          Read the replica hosts of `{{MYSQL_HOST}}` off this list; there is no dedicated replica
          sub-command.

        Storage: take **provisioned** storage from `mysql_server_config_get` (StorageSizeGB) and
        **used** storage is collected from Datadog by the pipeline, so do not look for it. Record only the
        provisioned figure.

        Do not query `storage_percent`. These servers run with storage auto-grow ON, which moves the
        provisioned figure on its own, so a percentage of it measures the server's willingness to
        grow rather than any risk to the service. Should a later stage ever need the ratio — it does
        only when auto-grow is OFF — it is used ÷ provisioned from the two figures above, so there is
        nothing to collect for it.

        ### AKS node pools

        - `aks` / `aks_cluster_get` with `{resource-group, subscription}` — lists the clusters in the
          group. Take the cluster name from here; do not construct it from `{{ENV}}` or the
          namespace `{{NAMESPACE}}`.
        - `aks` / `aks_nodepool_get` with `{cluster, resource-group, subscription}` — per pool:
          `name`, `vmSize`, `minCount`, `maxCount`, `count`, `enableAutoScaling`, `mode`. Record the
          pool the application runs on, which is the `User`-mode pool with the largest `maxCount`.

        Note that AKS creates a second, auto-managed group named after the cluster (e.g.
        `<group>AKSClusterV2`) holding the node VMSS. The cluster resource itself lives in
        `{{AZURE_RESOURCE_GROUP}}` — query that one, not the node group.

        On failure write `NOT_COLLECTED: Azure MCP error — <reason>` once per affected value and move
        on; do not retry the same call with the same arguments. Retry once only if the reason names
        something you can correct from the rules above — a wrong resource group, a long server name,
        or an invented sub-command.

        ---

        ## Output

        Write the result to `{{OUTPUT_PATH}}` with the file system write tool: the values above as
        bullets under the heading `## 8. Infrastructure Live State (Azure MCP)`, then **last**, a single
        fenced ```json block with exactly one key — the value of the `max_connections` parameter as a
        number, or `null` when it was not collected or the service uses no MySQL:

        ```json
        { "mysql_max_connections": null }
        ```
            """;
    }
}
