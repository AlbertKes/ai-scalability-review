import {MenuFoldOutlined, MenuUnfoldOutlined, PlusOutlined} from "@ant-design/icons";
import {Button, ConfigProvider, Layout, Space, Tag, Typography, theme as antdTheme} from "antd";
import {Navigate, Route, Routes, useNavigate} from "@wonder/core-fe";
import {Outlet} from "react-router-dom";
import {DASHBOARD_PATH, NEW_RUN_PATH, routes} from "@/route";
import {useRouteMeta} from "@/utils/useRouteMeta";
import {useRuntimeState} from "@/utils/useRuntimeState";
import {Nav} from "./component/Nav";
import {NotificationBell} from "./component/NotificationBell";
import {actions} from "./module";

const COLLAPSED_WIDTH = 64;
const EXPANDED_WIDTH = 208;

const Header = () => {
  const {navCollapsed} = useRuntimeState("main");
  const meta = useRouteMeta();
  const navigate = useNavigate();

  return (
    <div style={{display: "flex", alignItems: "center", justifyContent: "space-between", height: "100%"}}>
      <Space size={12}>
        <Button
          type="text"
          icon={navCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={() => void actions.toggleNav()}
          aria-label="Toggle navigation"
        />
        {meta.backTo ? (
          <Button type="link" style={{paddingInline: 0}} onClick={() => navigate(`/${meta.backTo!.path}`)}>
            ← {meta.backTo.label}
          </Button>
        ) : null}
        <Typography.Title level={4} style={{margin: 0}}>
          {meta.title ?? ""}
        </Typography.Title>
      </Space>
      <Space size={12}>
        <Tag color="blue">local</Tag>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(`/${NEW_RUN_PATH}`)}>
          New review
        </Button>
        <NotificationBell />
      </Space>
    </div>
  );
};

const MainLayout = () => {
  const {navCollapsed} = useRuntimeState("main");
  return (
    <Layout style={{height: "100%"}} hasSider>
      <Layout.Sider
        theme="light"
        collapsed={navCollapsed}
        collapsedWidth={COLLAPSED_WIDTH}
        width={EXPANDED_WIDTH}
        style={{paddingTop: 16, boxShadow: "1px 0 1px rgba(0,0,0,.06)", overflow: "auto"}}
      >
        <div style={{padding: navCollapsed ? "0 8px 16px" : "0 16px 16px"}}>
          <Typography.Text strong style={{fontSize: navCollapsed ? 12 : 14}}>
            {navCollapsed ? "SRC" : "Scalability Review"}
          </Typography.Text>
        </div>
        <Nav collapsed={navCollapsed} />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{background: "#fff", padding: "0 20px", borderBottom: "1px solid #f0f0f0"}}>
          <Header />
        </Layout.Header>
        <Layout.Content style={{padding: 20, overflow: "auto"}}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  );
};

const Main = () => {
  const {settings} = useRuntimeState("main");
  const dark = settings?.theme === "dark";
  const compact = settings?.density === "compact";

  return (
    <ConfigProvider
      theme={{
        algorithm: [
          dark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
          ...(compact ? [antdTheme.compactAlgorithm] : []),
        ],
      }}
    >
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route path="/" element={<Navigate to={`/${DASHBOARD_PATH}`} replace />} />
          {routes.map(route => (
            <Route key={route.path} path={route.path} Component={route.component} />
          ))}
          <Route path="*" element={<Navigate to={`/${DASHBOARD_PATH}`} replace />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
};

export default Main;
