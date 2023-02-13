import site from "../../../content/site.json";
import { USExtLink } from "../../../components/USLink";

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
                    <USExtLink href="https://www.google.com/chrome/">
                        Google Chrome
                    </USExtLink>
                </li>
                <li>
                    <USExtLink href="https://www.mozilla.org/en-US/firefox/new/">
                        Mozilla Firefox
                    </USExtLink>
                </li>
                <li>
                    <USExtLink href="https://www.apple.com/safari/">
                        Apple Safari
                    </USExtLink>
                </li>
                <li>
                    <USExtLink href="https://www.microsoft.com/en-us/edge">
                        Microsoft Edge
                    </USExtLink>
                </li>
            </ul>
            <p>
                Still having issues? Contact ReportStream support at{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </USExtLink>
                .
            </p>
        </div>
    );
};
