import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router";

import "./global.scss";

import { createRouter } from "./AppRouter";
import App from "./App";
import MainLayout from "./layouts/Main/MainLayout";

const router = createRouter(<App Layout={MainLayout} />);
const root = createRoot(document.getElementById("root")!);

root.render(<RouterProvider router={router} />);
