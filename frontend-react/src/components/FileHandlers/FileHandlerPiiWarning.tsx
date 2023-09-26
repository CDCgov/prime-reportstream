import site from "../../content/site.json";
import { Link } from "../../shared/Link/Link";

export default function FileHandlerPiiWarning() {
    return (
        <div className="font-sans-md margin-bottom-2">
            <p className="margin-bottom-4">
                Check that public health departments can receive your data
                through ReportStream by validating your file format.{" "}
            </p>
            <p className="margin-0">
                Reminder: Do not submit PII. Email{" "}
                <Link href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </Link>{" "}
                if you need fake data to use.
            </p>
        </div>
    );
}
