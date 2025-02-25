import { Accordion, Button, Icon } from "@trussworks/react-uswds";
import { Alert, Table } from "../../shared";

interface CodeMappingResultsProps {
    fileName: string;
}

const CodeMappingResults = ({ fileName, data, initialStepHandler }: CodeMappingResultsProps) => {
    const unmappedData = data.filter((item) => item.mapped === "N");
    const areCodesMapped = unmappedData.length === 0;
    const rowData = unmappedData.map((dataRow) => [
        {
            columnKey: "Code",
            columnHeader: "Code",
            content: dataRow["test code"],
        },
        {
            columnKey: "Name",
            columnHeader: "Name",
            content: dataRow["test description"],
        },
        {
            columnKey: "Coding system",
            columnHeader: "Coding system",
            content: dataRow["coding system"],
        },
    ]);
    return (
        <>
            <h2 className="margin-bottom-0">
                <span className="text-normal font-body-md text-base margin-bottom-0">
                    <p>File Name</p>
                    <p>{fileName}</p>
                </span>
            </h2>
            <div className="margin-top-2">
                {areCodesMapped ? (
                    <Alert type={"success"} heading={"All codes are mapped"}></Alert>
                ) : (
                    <Alert type={"error"} heading={"Your file contains unmapped codes "}>
                        Review unmapped codes for any user error, such as a typo. If the unmapped codes are accurate,
                        download the table and send the file to your onboarding engineer or reportstream@cdc.gov. Our
                        team will support any remaining mapping needed.
                    </Alert>
                )}
            </div>
            <h3>Unmapped codes</h3>
            <div className="padding-top-2 padding-bottom-4 padding-x-3 bg-gray-5 margin-bottom-4">
                <Table gray borderless rowData={rowData} />
            </div>
            <Button type="button" onClick={initialStepHandler}>
                Test another file
            </Button>
        </>
    );
};

export default CodeMappingResults;
