import './index.css';
import App from './App';
//import reportWebVitals from './reportWebVitals';
import { CacheProvider } from 'rest-hooks';
import { BrowserRouter as Router } from 'react-router-dom';

import { hydrate, render } from "react-dom";

const rootElement = document.getElementById("root");
if (rootElement && rootElement.hasChildNodes()) {
  hydrate( <CacheProvider>
    <Router>
      <App />
    </Router>
  </CacheProvider>,  document.getElementById("root"));
} else {
  render( <CacheProvider>
    <Router>
      <App />
    </Router>
  </CacheProvider>,  document.getElementById("root"));
}


// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
//reportWebVitals();
