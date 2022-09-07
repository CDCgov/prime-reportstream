import ReactDOM from "react-dom";
import { CacheProvider } from "rest-hooks";
// need to include browser here for now so that app can have easy access to history
import { BrowserRouter as Router } from "react-router-dom";

import App from "./App";
// compiled css so the resources process for the static site by the compiler
import "./content/generated/global.out.css";

ReactDOM.render(
    <CacheProvider>
        <Router>
            <App />
        </Router>
    </CacheProvider>,
    document.getElementById("root")
);
