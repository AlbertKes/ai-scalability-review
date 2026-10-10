import "./index.css";
import {bootstrap} from "@wonder/core-fe";
import {ErrorHandler} from "@/utils/ErrorHandler";
import {Main} from "./page/main";

bootstrap({
  componentType: Main,
  errorListener: new ErrorHandler(),
  rootContainer: document.getElementById("app") ?? undefined,
});
