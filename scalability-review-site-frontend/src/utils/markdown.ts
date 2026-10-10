/**
 * Markdown helpers kept out of the component file so the module exports only components —
 * which is what keeps fast refresh working.
 */
export interface MarkdownHeading {
  level: number;
  text: string;
  id: string;
}

export const slugify = (text: string): string =>
  text
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, "")
    .trim()
    .replace(/\s+/g, "-");

/** Pulls the heading tree out of the markdown so the reader gets a table of contents. */
export const extractHeadings = (markdown: string): MarkdownHeading[] => {
  const headings: MarkdownHeading[] = [];
  let inFence = false;
  for (const line of markdown.split("\n")) {
    if (line.startsWith("```")) {
      inFence = !inFence;
      continue;
    }
    if (inFence) continue;
    const match = /^(#{1,4})\s+(.*)$/.exec(line);
    if (match) {
      const text = match[2].trim();
      headings.push({level: match[1].length, text, id: slugify(text)});
    }
  }
  return headings;
};
