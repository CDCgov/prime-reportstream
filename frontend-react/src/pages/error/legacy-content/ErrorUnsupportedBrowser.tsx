import site from "../../../content/site.json";
import { Link } from "../../../shared/Link/Link";

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
                    <Link href="https://www.google.com/chrome/">
                        Google Chrome
                    </Link>
                </li>
                <li>
                    <Link href="https://www.mozilla.org/en-US/firefox/new/">
                        Mozilla Firefox
                    </Link>
                </li>
                <li>
                    <Link href="https://www.apple.com/safari/">
                        Apple Safari
                    </Link>
                </li>
                <li>
                    <Link href="https://www.microsoft.com/en-us/edge">
                        Microsoft Edge
                    </Link>
                </li>
            </ul>
            <p>
                Still having issues? Contact ReportStream support at{" "}
                <Link href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </Link>
                .
            </p>
        </div>
    );
};
