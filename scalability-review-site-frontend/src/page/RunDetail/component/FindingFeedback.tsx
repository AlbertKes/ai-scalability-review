import {Button, Card, Input, Select, Space, Table, Tag, Typography} from "antd";
import {useState} from "react";
import type {MarkdownHeading} from "@/utils/markdown";
import {formatShortDateTime} from "@/utils/format";
import type {ListFindingFeedbackResponse$FindingFeedbackView} from "type/api";

const VERDICTS = [
  {value: "CONFIRMED", label: "Confirmed"},
  {value: "FALSE_POSITIVE", label: "False positive"},
  {value: "WONT_FIX", label: "Won't fix"},
];

const VERDICT_COLORS: Record<string, string> = {
  CONFIRMED: "success",
  FALSE_POSITIVE: "error",
  WONT_FIX: "default",
};

interface Props {
  headings: MarkdownHeading[];
  feedback: ListFindingFeedbackResponse$FindingFeedbackView[];
  onSave: (findingKey: string, title: string, verdict: string, comment: string) => void;
}

/**
 * A reader's verdict on one finding (FR-6.10).
 *
 * This is the only ground truth there is for whether the pipeline produces useful findings — a
 * report that reads well and a report that is right are not the same thing.
 */
export const FindingFeedback = ({headings, feedback, onSave}: Props) => {
  const [findingKey, setFindingKey] = useState<string>();
  const [verdict, setVerdict] = useState<string>("CONFIRMED");
  const [comment, setComment] = useState("");

  const options = headings
    .filter(heading => heading.level >= 2)
    .map(heading => ({value: heading.id, label: heading.text}));
  const selected = headings.find(heading => heading.id === findingKey);

  return (
    <Card size="small" title="Finding verdicts">
      <Space direction="vertical" size={10} style={{width: "100%"}}>
        <Space wrap>
          <Select
            showSearch
            placeholder="pick a section"
            style={{width: 360}}
            value={findingKey}
            options={options}
            optionFilterProp="label"
            onChange={setFindingKey}
          />
          <Select style={{width: 160}} value={verdict} options={VERDICTS} onChange={setVerdict} />
          <Input
            placeholder="why (optional)"
            style={{width: 300}}
            value={comment}
            onChange={event => setComment(event.target.value)}
          />
          <Button
            type="primary"
            disabled={!findingKey}
            onClick={() => {
              if (!findingKey) return;
              onSave(findingKey, selected?.text ?? findingKey, verdict, comment);
              setComment("");
            }}
          >
            Record
          </Button>
        </Space>

        {feedback.length === 0 ? (
          <Typography.Text type="secondary">
            Nothing recorded yet. Marking false positives here is what makes report quality measurable over time.
          </Typography.Text>
        ) : (
          <Table
            rowKey="feedback_id"
            size="small"
            pagination={false}
            dataSource={feedback}
            columns={[
              {title: "Finding", dataIndex: "finding_title", render: (value, row) => value ?? row.finding_key},
              {
                title: "Verdict",
                dataIndex: "verdict",
                width: 150,
                render: value => <Tag color={VERDICT_COLORS[value] ?? "default"}>{value}</Tag>,
              },
              {title: "Comment", dataIndex: "comment", render: value => value ?? "—"},
              {title: "By", dataIndex: "author", width: 120},
              {title: "When", dataIndex: "created_at", width: 140, render: value => formatShortDateTime(value)},
            ]}
          />
        )}
      </Space>
    </Card>
  );
};
