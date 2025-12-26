import site from "../../content/site.json";
import { USExtLink } from "../USLink";

export default function FileHandlerPiiWarning() {
    return (
        <div className="font-sans-md margin-bottom-2">
            <p className="margin-bottom-4">
                Check that public health departments can receive your data
                through ReportStream by validating your file format.{" "}
            </p>
            <p className="margin-0">
                Reminder: Do not submit PII. Email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </USExtLink>{" "}
                if you need fake data to use.
            </p>
        </div>
    );
}
