import {APIException, type ErrorListener, type Exception} from "@wonder/core-fe";
import {errorToast} from "@/components/Message";

/**
 * Turns a failed call into one readable toast.
 *
 * <p>A rejected submit is the interesting case: the backend answers with every problem it found,
 * newline separated, so the message is shown whole rather than truncated to its first line.
 */
export class ErrorHandler implements ErrorListener {
  onError(error: Exception): void {
    if (error instanceof APIException) {
      errorToast(error.message || `Request failed with ${error.statusCode}`, 8);
      return;
    }
    errorToast(error.message || "Something went wrong");
  }
}
