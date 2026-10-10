import type {LocalReviewRequestView} from "type/api";
import type {State} from "./type";

export const emptyRequest = (): LocalReviewRequestView => ({
  service: "",
  local_app_repo_path: "",
  local_infra_repo_path: "",
  env: "",
  namespace: "",
  domain: "",
  mysql_host: "",
  mysql_db: "",
  atlas_cluster: "",
  azure_resource_group: "",
  azure_subscription_id: "",
  hpa_type: "none",
  kafka_consumer_groups: "",
  skip_code_analysis: false,
  skip_validation: false,
  app_code_paths: [],
  code_analysis_model: "",
  metric_collection_model: "",
  synthesis_model: "",
  validation_model: "",
  output_base_dir: "",
  operator: "",
});

export const initialState: State = {
  request: emptyRequest(),
  labels: [],
  errors: [],
  warnings: [],
  validated: false,
  acknowledged: false,
  presets: [],
  preflight: [],
  estimate: null,
};
