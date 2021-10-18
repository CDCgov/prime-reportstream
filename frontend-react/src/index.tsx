import ReactDOM from "react-dom";
import { CacheProvider } from "rest-hooks";
import { BrowserRouter as Router } from "react-router-dom";

import App from "./App";
import "./global.scss";

ReactDOM.render(
    <CacheProvider>
        <Router>
            <App />
        </Router>
    </CacheProvider>,
    document.getElementById("root")
);
