import {DownloadOutlined, EyeOutlined, FileZipOutlined} from "@ant-design/icons";
import {Button, Card, Modal, Space, Table, Tag, Typography} from "antd";
import {Markdown} from "@/components/Markdown";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import {STAGE_LABELS} from "@/constants/review";
import {formatBytes, formatShortDateTime} from "@/utils/format";
import type {ListArtifactResponse$ArtifactView} from "type/api";

interface Props {
  jobId: string;
  outputDir: string | null;
  artifacts: ListArtifactResponse$ArtifactView[];
  preview: {name: string; content: string} | null;
  onPreview: (name: string) => void;
  onClosePreview: () => void;
}

/** Everything the run left on disk, readable without leaving the browser (FR-11). */
export const ArtifactsTab = ({jobId, outputDir, artifacts, preview, onPreview, onClosePreview}: Props) => (
  <Space direction="vertical" size={12} style={{width: "100%"}}>
    <Card size="small">
      <Space direction="vertical" size={4}>
        <Typography.Text type="secondary">Output directory — one per run, never shared</Typography.Text>
        <Typography.Text copyable code>
          {outputDir ?? "not created yet"}
        </Typography.Text>
      </Space>
    </Card>

    <Card
      size="small"
      title={`${artifacts.length} file(s)`}
      extra={
        <Button
          size="small"
          icon={<FileZipOutlined />}
          href={ReviewAJAXWebService.archiveURL(jobId)}
          disabled={artifacts.length === 0}
        >
          Download all
        </Button>
      }
    >
      <Table
        rowKey="name"
        size="small"
        pagination={false}
        dataSource={artifacts}
        columns={[
          {title: "File", dataIndex: "name"},
          {
            title: "Stage",
            dataIndex: "stage",
            width: 180,
            render: value => (value ? (STAGE_LABELS[value] ?? value) : "—"),
          },
          {title: "Kind", dataIndex: "kind", width: 110, render: value => <Tag>{value}</Tag>},
          {title: "Size", dataIndex: "size_bytes", width: 100, align: "right", render: value => formatBytes(value)},
          {
            title: "Modified",
            dataIndex: "modified_at",
            width: 140,
            render: value => formatShortDateTime(value),
          },
          {
            title: "",
            width: 150,
            render: (_, artifact) => (
              <Space size={4}>
                <Button size="small" icon={<EyeOutlined />} onClick={() => onPreview(artifact.name)}>
                  View
                </Button>
                <Button
                  size="small"
                  icon={<DownloadOutlined />}
                  href={ReviewAJAXWebService.artifactURL(jobId, artifact.name)}
                  download={artifact.name}
                />
              </Space>
            ),
          },
        ]}
      />
    </Card>

    <Modal open={preview !== null} title={preview?.name} width={960} footer={null} onCancel={onClosePreview}>
      <div style={{maxHeight: "70vh", overflow: "auto"}}>
        {preview?.name.endsWith(".md") ? (
          <Markdown content={preview.content} />
        ) : (
          <pre style={{whiteSpace: "pre-wrap", fontSize: 12}}>{formatPreview(preview?.name, preview?.content)}</pre>
        )}
      </div>
    </Modal>
  </Space>
);

function formatPreview(name: string | undefined, content: string | undefined): string {
  if (!content) return "";
  if (!name?.endsWith(".json")) return content;
  try {
    return JSON.stringify(JSON.parse(content), null, 2);
  } catch {
    return content;
  }
}
