import watersApiFunctions from "../network/api/WatersApiFunctions";
import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";

const UploadToPipeline = () => {
    return (
        <FileHandler
            headingText="File Uploader"
            successMessage="Your file has been uploaded"
            handlerType={FileHandlerType.UPLOAD}
            formLabel="Select an HL7 or CSV formatted file to upload."
            resetText="Upload another file"
            submitText="Upload"
            showSuccessMetadata={true}
            fetcher={watersApiFunctions.postData}
        />
    );
};

export default UploadToPipeline;
