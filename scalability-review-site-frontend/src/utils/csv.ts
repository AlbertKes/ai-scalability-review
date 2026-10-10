/** Minimal CSV export for the run list (FR-2.10). Values are quoted so commas survive. */
export function downloadCsv(filename: string, rows: Array<Record<string, string | number | null | undefined>>): void {
  if (rows.length === 0) return;
  const headers = Object.keys(rows[0]);
  const escape = (value: string | number | null | undefined) => `"${String(value ?? "").replace(/"/g, '""')}"`;
  const body = [headers.join(","), ...rows.map(row => headers.map(header => escape(row[header])).join(","))].join("\n");

  const blob = new Blob([body], {type: "text/csv;charset=utf-8"});
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
