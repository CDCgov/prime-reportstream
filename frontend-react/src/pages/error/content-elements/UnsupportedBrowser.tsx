import DOMPurify from "dompurify";

import site from "../../../content/site.json";

export const UnsupportedBrowser = () => {
    return (
        <div className="rs-unsupported">
            <h1>Sorry! ReportStream does not support your browser</h1>
            <p>
                Please download and up-to-date browser, like one linked below,
                and try again.
            </p>
            <ul>
                <li>
                    <a href="src/pages/error/content-elements/UnsupportedBrowser">
                        Google Chrome
                    </a>
                </li>
                <li>
                    <a href="src/pages/error/content-elements/UnsupportedBrowser">
                        Mozilla Firefox
                    </a>
                </li>
                <li>
                    <a href="src/pages/error/content-elements/UnsupportedBrowser">
                        Apple Safari
                    </a>
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
