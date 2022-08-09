import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";
import watersApiFunctions from "../network/api/WatersApiFunctions";
import { EndpointName } from "../network/api/WatersApi";

const Validate = () => {
    return (
        <FileHandler
            headingText="File Validator"
            successMessage="Your file has been validated"
            handlerType={FileHandlerType.VALIDATION}
            resetText="Validate another file"
            submitText="Validate"
            showSuccessMetadata={false}
            fetcher={watersApiFunctions.postData}
            showWarningBanner={false}
            endpointName={EndpointName.VALIDATE}
        />
    );
};

export default Validate;
