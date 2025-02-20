interface CodeMappingResultsProps {
    fileName: string;
}

const CodeMappingResults = ({ fileName, data }: CodeMappingResultsProps) => (
    <>
        <h2 className="margin-bottom-0">
            <span className="text-normal font-body-md text-base margin-bottom-0">
                File Name
                <br />
                {fileName}
            </span>
        </h2>
    </>
);

export default CodeMappingResults;
