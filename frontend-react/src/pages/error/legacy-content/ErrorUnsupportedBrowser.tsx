import DOMPurify from "dompurify";

import site from "../../../content/site.json";

export const ErrorUnsupportedBrowser = () => {
    return (
        <div className="rs-unsupported">
            <h1>Sorry! ReportStream does not support your browser</h1>
            <p>
                Please download and up-to-date browser, like one linked below,
                and try again.
            </p>
            <ul>
                <li>
                    <a href="https://www.google.com/chrome/">Google Chrome</a>
                </li>
                <li>
                    <a href="https://www.mozilla.org/en-US/firefox/new/">
                        Mozilla Firefox
                    </a>
                </li>
                <li>
                    <a href="https://www.apple.com/safari/">Apple Safari</a>
                </li>
                <li>
                    <a href="https://www.microsoft.com/en-us/edge">
                        Microsoft Edge
                    </a>
                </li>
            </ul>
            <p>
                Still having issues? Contact ReportStream support at{" "}
                <a href={"mailto:" + DOMPurify.sanitize(site.orgs.RS.email)}>
                    {site.orgs.RS.email}
                </a>
                .
            </p>
        </div>
    );
};
