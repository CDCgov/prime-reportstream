import ReportStreamHeader from "../../components/header/ReportStreamHeader";
import { USExtLink } from "../../components/USLink";
import site from "../../content/site.json";

export function UserAgentNotSupported() {
    return (
        <>
            <ReportStreamHeader isNavHidden blueVariant />
            <main>
                <article>
                    <h1>Sorry! ReportStream does not support your browser</h1>
                    <p>
                        Please update your browser or use one of the following:
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
                </article>
            </main>
        </>
    );
}

export default UserAgentNotSupported;
