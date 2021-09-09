import download from "downloadjs";
import ReportResource from "../../../resources/ReportResource";
import { Button } from "@trussworks/react-uswds";

type Props = {
    /* REQURIED
    A ReportResource is passed in using this property. This is necessary for download()
    since that function relies on the content, fileName, and mimeType properties */
    report: ReportResource | undefined

    /* OPTIONAL
    This boolean flag changes the return value from a standart <a> link to a <Button> (USWDS)
    so this single component can be used in Daily.tsx and Details.tsx */
    button?: boolean
}

const formatFileType = (fileType: string) => {
    if (fileType === "HL7_BATCH") return "HL7(BATCH)"
    return fileType
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

    if (!props.button) {
        return (
            <a
                href="/"
                onClick={handleClick}
                className="usa-link">
                {
                    props.report !== undefined ?
                        formatFileType(props.report.fileType)
                        :
                        ""
                }
            </a>
        );
    } else {
        return (
            <Button
                type="button"
                outline
                onClick={handleClick}
                className="usa-button usa-button--outline float-right"
            >
                {
                    props.report !== undefined ?
                        formatFileType(props.report.fileType)
                        :
                        ""
                }
            </Button>
        );
    }


}

export default ReportLink