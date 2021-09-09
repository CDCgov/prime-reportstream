import download from "downloadjs";
import ReportResource from "../../../resources/ReportResource";

type Props = {
    /* REQURIED
    A ReportResource is passed in using this property. This is necessary for download()
    since that function relies on the content, fileName, and mimeType properties */
    report: ReportResource
}

/* 
    This element provides a download link on each row of the table and on the report
    details page
*/
function ReportLink(props: Props) {
    const handleClick = (e: any) => {
        e.preventDefault();
        if (props.report !== undefined) {
            download(props.report.content, props.report.fileName, props.report.mimeType);
        }
    };

    return (
        <a href="/" onClick={handleClick} className="usa-link">
            {props.report !== undefined
                ? props.report.fileType === "HL7_BATCH"
                    ? "HL7(BATCH)"
                    : props.report.fileType
                : ""}
        </a>
    );
}

export default ReportLink