import type { PropsWithChildren } from "react";

export type CodeMappingResultsProps = PropsWithChildren;

const CodeMappingResults = ({ data, onReset, fileName }: CodeMappingResultsProps) => {
    const unmappedData = data.filter((item: ConditionCodeData) => item.mapped === "N");
    const areCodesMapped = unmappedData.length === 0;
    const rowData = unmappedData.map((dataRow) => [
        {
            columnKey: "Code",
            columnHeader: "Code",
            content: dataRow.testCode,
        },
        {
            columnKey: "Name",
            columnHeader: "Name",
            content: dataRow.testDescription,
        },
        {
            columnKey: "Coding system",
            columnHeader: "Coding system",
            content: dataRow.codingSystem,
        },
    ]);
    function handleSaveToCsvClick() {
        const dateObj = generateDateTitles(undefined, true);
        return saveToCsv(unmappedData, {
            filename: `${removeFileExtension(fileName)} ${dateObj.dateString}`,
        });
    }
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
                        Review unmapped codes for any user error, such as a typo. If the unmapped codes are accurate,{" "}
                        <Link href="#" onClick={handleSaveToCsvClick}>
                            download the table
                        </Link>{" "}
                        and send the file to your onboarding engineer or{" "}
                        <USExtLink href={`mailto: ${site.orgs.RS.email}`}>{site.orgs.RS.email}</USExtLink> . Our team
                        will support any remaining mapping needed.
                    </Alert>
                )}
            </div>
            <div className="display-flex flex-justify flex-align-center">
                <h3>Unmapped codes</h3>
                <Button type="button" outline onClick={handleSaveToCsvClick}>
                    Download table as CSV <Icon.FileDownload className="text-top" />
                </Button>
            </div>

            <div className="padding-top-2 padding-bottom-4 padding-x-3 bg-gray-5 margin-bottom-4">
                <Table gray borderless rowData={rowData} />
            </div>
            <Alert type="tip" className="margin-bottom-6">
                Follow <a href="/developer-resources/api-onboarding-guide#3_4">these instructions</a> and use{" "}
                <a href={site.assets.codeMapTemplate.path}>our template</a> to format your result and organism codes to
                LOINC or SNOMED.
            </Alert>
            <Button type="button" onClick={onReset}>
                Test another file
            </Button>
        </>
    );
};

export default CodeMappingResults;
