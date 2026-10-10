import {matchPath, useLocation} from "@wonder/core-fe";
import {routes} from "@/route";
import type {RouteHandle} from "@/route/type";

/**
 * Resolves the current route's title and back link from the route table.
 *
 * Read from the table rather than from the router's own match data so the header works the same
 * whichever router flavour the framework mounts underneath.
 */
export function useRouteMeta(): RouteHandle {
  const location = useLocation();
  for (const route of routes) {
    if (matchPath({path: `/${route.path}`, end: true}, location.pathname)) return route.handle ?? {};
  }
  return {};
}
