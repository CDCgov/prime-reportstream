import ReactDOM from "react-dom";
import "./index.css";
import App from "./App";
import { CacheProvider } from "rest-hooks";
import { BrowserRouter as Router } from "react-router-dom";

ReactDOM.render(
    <CacheProvider>
        <Router>
            <App />
        </Router>
    </CacheProvider>,
    document.getElementById("root")
);
