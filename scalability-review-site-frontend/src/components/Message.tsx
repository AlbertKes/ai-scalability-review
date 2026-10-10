import {message} from "antd";

/**
 * Toasts.
 *
 * Multi-line content is deliberately preserved: the validation endpoint answers with a list, and
 * collapsing that list to one line would undo the point of reporting every problem at once.
 */
const MULTILINE_STYLE = {whiteSpace: "pre-line" as const, textAlign: "left" as const, maxWidth: 640};

export const successToast = (content: string, duration = 3): void => {
  message.success({content, duration, style: MULTILINE_STYLE});
};

export const errorToast = (content: string, duration = 5): void => {
  message.error({content, duration, style: MULTILINE_STYLE});
};

export const warningToast = (content: string, duration = 5): void => {
  message.warning({content, duration, style: MULTILINE_STYLE});
};

export const infoToast = (content: string, duration = 3): void => {
  message.info({content, duration, style: MULTILINE_STYLE});
};
