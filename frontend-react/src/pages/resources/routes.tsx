import { ReadonlyDeep } from "type-fest";
import React from "react";

import { lazy } from "../../utils/misc";
import { RSRouteObject } from "../../utils/UsefulTypes";

import { Resources } from "./Resources";
import apiProgrammersGuideRoute from "./api-programmers-guide/routes";

const LazyManagePublicKeyPage = lazy(
    () => import("../../components/ManagePublicKey/ManagePublicKey"),
    "ManagePublicKeyWithAuth"
);
const LazyResourcesPage = lazy(
    () => import("./ResourcesPage"),
    "ResourcesPage"
);

const routes = {
    path: "/resources",
    children: [
        ...(apiProgrammersGuideRoute?.children ?? []),
        {
            path: "manage-public-key",
            element: <LazyManagePublicKeyPage />,
        },
        { path: "", element: <LazyResourcesPage /> },
        { path: "*", element: <Resources /> },
    ],
} as const satisfies ReadonlyDeep<RSRouteObject>;

export default routes;

export { apiProgrammersGuideRoute };
export * from "./api-programmers-guide/routes";
