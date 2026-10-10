import type {State} from "./type";

export const initialState: State = {
  job: null,
  artifacts: [],
  outputDir: null,
  report: null,
  validation: null,
  feedback: [],
  logStage: null,
  logContent: "",
  logOffset: 0,
  logComplete: true,
  preview: null,
  showSources: true,
  notCollected: [],
};
