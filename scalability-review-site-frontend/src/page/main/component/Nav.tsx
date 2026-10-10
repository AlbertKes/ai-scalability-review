import {Menu} from "antd";
import {useLocation, useNavigate} from "@wonder/core-fe";
import {menus} from "@/route";

/** Left navigation. The selected key comes from the first path segment, so detail pages stay lit. */
export const Nav = ({collapsed}: {collapsed: boolean}) => {
  const location = useLocation();
  const navigate = useNavigate();
  const segment = location.pathname.split("/").filter(Boolean)[0] ?? "";
  const selected = menus.find(menu => menu.path === segment)?.key ?? "";

  return (
    <Menu
      mode="inline"
      inlineCollapsed={collapsed}
      selectedKeys={[selected]}
      style={{borderInlineEnd: "none"}}
      items={menus.map(menu => ({key: menu.key, icon: menu.icon, label: menu.title}))}
      onClick={({key}) => {
        const menu = menus.find(item => item.key === key);
        if (menu) navigate(`/${menu.path}`);
      }}
    />
  );
};
