import { Alert, Table } from "../../shared";

interface CodeMappingResultsProps {
    fileName: string;
}

const CodeMappingResults = ({ fileName, data }: CodeMappingResultsProps) => {
    console.log("data  = ", data);

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
                    File Name
                    <br />
                    {fileName}
                </span>
            </h2>
            <div>
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
            <div className="bg-gray-5">
                <Table borderless rowData={rowData} />
            </div>
        </>
    );
};

export default CodeMappingResults;
