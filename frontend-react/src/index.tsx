import ReactDOM from "react-dom";
import { CacheProvider } from "rest-hooks";
import { BrowserRouter as Router } from "react-router-dom";

import App from "./App";
// compiled css so the resources process for the static site by the compiler
import "./content/generated/global.out.css";
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
