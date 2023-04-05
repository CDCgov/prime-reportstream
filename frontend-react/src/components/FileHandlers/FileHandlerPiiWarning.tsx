import site from "../../content/site.json";
import { USExtLink } from "../USLink";

export default function FileHandlerPiiWarning() {
    return (
        <div className="font-sans-md">
            <p>
                Check that public health departments can receive your data
                through ReportStream by validating your file format.{" "}
            </p>
            <p>
                Reminder: Do not submit PII. Email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </USExtLink>{" "}
                if you need fake data to use.
            </p>
        </div>
    );
}
