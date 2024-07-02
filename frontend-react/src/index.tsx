import { createRoot } from "react-dom/client";

import { appRoutes } from "./AppRouter";
import App from "./components/App/App";
import config from "./config";

import "./global.scss";

const root = createRoot(document.getElementById("root")!);

root.render(<App config={config} routes={appRoutes} />);
