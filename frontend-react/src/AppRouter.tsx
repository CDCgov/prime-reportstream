import { lazy } from "react";
import { redirect, RouteObject } from "react-router";

import RSErrorBoundary from "./components/RSErrorBoundary/RSErrorBoundary";
import { lazyRouteMarkdown } from "./utils/LazyRouteMarkdown";

/* Content Pages */
const Home = lazy(lazyRouteMarkdown(() => import("./content/home/index.mdx")));

const MainLayout = lazy(() => import("./layouts/Main/MainLayout"));

export const appRoutes: RouteObject[] = [
    /* Public Site */
    {
        path: "/",
        Component: MainLayout,
        ErrorBoundary: RSErrorBoundary,
        children: [
            {
                path: "",
                index: true,
                element: <Home />,
                handle: {
                    isContentPage: true,
                    isFullWidth: true,
                },
            },
            {
                path: "*",
                loader: () => {
                    return redirect("/");
                },
            },
        ],
    },
] satisfies RsRouteObject[];
