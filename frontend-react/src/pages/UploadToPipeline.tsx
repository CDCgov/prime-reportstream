import watersApiFunctions from "../network/api/WatersApiFunctions";
import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";
import { EndpointName } from "../network/api/WatersApi";

const UploadToPipeline = () => {
    return (
        <FileHandler
            headingText="File Uploader"
            successMessage="Your file has been uploaded"
            handlerType={FileHandlerType.UPLOAD}
            resetText="Upload another file"
            submitText="Upload"
            showSuccessMetadata={true}
            fetcher={watersApiFunctions.postData}
            showWarningBanner={true}
            warningText={
                "Uploading files on this page will result in data being transmitted to public health authorities. Use caution when uploading data."
            }
            endpointName={EndpointName.WATERS}
        />
    );
};

export default UploadToPipeline;
