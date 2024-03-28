import { createRoot } from "react-dom/client";

import App from "./App";
import { appRoutes } from "./AppRouter";
import config from "./config";

import "./global.scss";

const root = createRoot(document.getElementById("root")!);

root.render(<App config={config} routes={appRoutes} />);
