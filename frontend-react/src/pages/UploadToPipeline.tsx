import watersApiFunctions from "../network/api/WatersApiFunctions";
import FileHandler from "../components/FileHandlers/FileHandler";

const UploadToPipeline = () => {
    return (
        <FileHandler
            headingText="File Uploader"
            successMessage="Your file has been uploaded"
            action="upload"
            formLabel="Select an HL7 or CSV formatted file to upload."
            resetText="Upload another file"
            submitText="Upload"
            showSuccessMetadata={true}
            fetcher={watersApiFunctions.postData}
        />
    );
};

export default UploadToPipeline;
