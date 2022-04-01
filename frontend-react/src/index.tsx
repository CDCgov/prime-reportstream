import ReactDOM from "react-dom";
import { CacheProvider } from "rest-hooks";
import { BrowserRouter as Router } from "react-router-dom";

import App from "./App";
import "./global.scss";
import SessionProvider from "./contexts/SessionStorageContext";

ReactDOM.render(
    <CacheProvider>
        <SessionProvider>
            <Router>
                <App />
            </Router>
        </SessionProvider>
    </CacheProvider>,
    document.getElementById("root")
);
