import {BellOutlined, CheckOutlined, DeleteOutlined} from "@ant-design/icons";
import {Badge, Button, Dropdown, Empty, List, Space, Tag, Tooltip, Typography} from "antd";
import {useNavigate} from "@wonder/core-fe";
import {RUN_DETAIL_PATH} from "@/route";
import {formatShortDateTime} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {NotificationSeverity, NotificationView} from "type/api";
import {actions} from "../module";

const SEVERITY_COLOR: Record<NotificationSeverity, string> = {
  SUCCESS: "success",
  WARNING: "warning",
  ERROR: "error",
};

/**
 * The bell in the top-right (FR-8).
 *
 * The count is served from the backend rather than kept in the page: a review outlives the tab that
 * started it, so an unread badge that resets on reload would be worse than none.
 */
export const NotificationBell = () => {
  const {notifications, unreadCount} = useRuntimeState("main");
  const navigate = useNavigate();

  const open = (notification: NotificationView) => {
    if (!notification.read_at) void actions.markRead(notification.notification_id);
    if (notification.job_id) navigate(`/${RUN_DETAIL_PATH}/${notification.job_id}`);
  };

  const panel = (
    <div style={{width: 420, background: "#fff", borderRadius: 8, boxShadow: "0 6px 16px rgba(0,0,0,.12)"}}>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "10px 14px",
          borderBottom: "1px solid #f0f0f0",
        }}
      >
        <Typography.Text strong>Notifications</Typography.Text>
        <Space size={4}>
          <Button size="small" type="text" icon={<CheckOutlined />} onClick={() => void actions.markAllRead()}>
            Mark all read
          </Button>
          <Tooltip title="Remove every notification">
            <Button
              size="small"
              type="text"
              icon={<DeleteOutlined />}
              onClick={() => void actions.clearNotifications()}
            />
          </Tooltip>
        </Space>
      </div>
      <div style={{maxHeight: 420, overflowY: "auto"}}>
        {notifications.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Nothing yet" style={{padding: 24}} />
        ) : (
          <List
            size="small"
            dataSource={notifications}
            renderItem={item => (
              <List.Item
                onClick={() => open(item)}
                style={{
                  cursor: "pointer",
                  padding: "10px 14px",
                  background: item.read_at ? undefined : "#f6ffed66",
                }}
              >
                <List.Item.Meta
                  title={
                    <Space size={6}>
                      <Tag color={SEVERITY_COLOR[item.severity]}>{item.severity}</Tag>
                      <span>{item.title}</span>
                    </Space>
                  }
                  description={
                    <Space direction="vertical" size={0}>
                      {item.body ? <Typography.Text type="secondary">{item.body}</Typography.Text> : null}
                      <Typography.Text type="secondary" style={{fontSize: 11}}>
                        {formatShortDateTime(item.created_at)}
                      </Typography.Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </div>
    </div>
  );

  return (
    <Dropdown dropdownRender={() => panel} trigger={["click"]} placement="bottomRight">
      <Badge count={unreadCount} overflowCount={9} size="small">
        <Button type="text" icon={<BellOutlined style={{fontSize: 18}} />} aria-label="Notifications" />
      </Badge>
    </Dropdown>
  );
};
