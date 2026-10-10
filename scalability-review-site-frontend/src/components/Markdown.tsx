import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {Children, isValidElement, type ReactNode} from "react";
import {slugify} from "@/utils/markdown";

/**
 * Renders a generated report.
 *
 * Two deliberate choices. Raw HTML is never rendered — no `rehype-raw` is installed, so the model's
 * output cannot inject markup into the operator's browser, and that is the whole sanitisation story.
 * And `[Source: …]` attributions are decorated in the rendered tree rather than rewritten in the
 * markdown: the attributions are the report's evidence trail, so the text that is shown stays byte
 * for byte what the pipeline produced.
 */
const SOURCE_PATTERN = /\[Source:[^\]]*\]/g;

const sourceChipStyle: React.CSSProperties = {
  display: "inline-block",
  margin: "0 2px",
  padding: "0 6px",
  borderRadius: 4,
  background: "#f0f5ff",
  color: "#1d39c4",
  fontSize: 11,
  lineHeight: "18px",
  verticalAlign: "middle",
  wordBreak: "break-all",
};

function decorateText(text: string, showSources: boolean, keyPrefix: string): ReactNode[] {
  const parts: ReactNode[] = [];
  let lastIndex = 0;
  let index = 0;
  for (const match of text.matchAll(SOURCE_PATTERN)) {
    const start = match.index ?? 0;
    if (start > lastIndex) parts.push(text.slice(lastIndex, start));
    if (showSources) {
      parts.push(
        <span key={`${keyPrefix}-${index}`} style={sourceChipStyle} title="Evidence the report cites for this claim">
          {match[0].replace(/^\[|\]$/g, "")}
        </span>
      );
    }
    lastIndex = start + match[0].length;
    index++;
  }
  if (lastIndex < text.length) parts.push(text.slice(lastIndex));
  return parts.length > 0 ? parts : [text];
}

function decorate(children: ReactNode, showSources: boolean): ReactNode {
  return Children.map(children, (child, childIndex) => {
    if (typeof child === "string") return decorateText(child, showSources, `s${childIndex}`);
    if (isValidElement(child)) return child;
    return child;
  });
}

const headingText = (children: ReactNode): string =>
  Children.toArray(children)
    .map(child => (typeof child === "string" ? child : ""))
    .join("");

interface Props {
  content: string;
  showSources?: boolean;
}

export const Markdown = ({content, showSources = true}: Props) => (
  <div className="markdown-body">
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      components={{
        h1: ({children}) => <h1 id={slugify(headingText(children))}>{children}</h1>,
        h2: ({children}) => <h2 id={slugify(headingText(children))}>{children}</h2>,
        h3: ({children}) => <h3 id={slugify(headingText(children))}>{children}</h3>,
        h4: ({children}) => <h4 id={slugify(headingText(children))}>{children}</h4>,
        p: ({children}) => <p>{decorate(children, showSources)}</p>,
        li: ({children}) => <li>{decorate(children, showSources)}</li>,
        td: ({children}) => <td>{decorate(children, showSources)}</td>,
      }}
    >
      {content}
    </ReactMarkdown>
  </div>
);
