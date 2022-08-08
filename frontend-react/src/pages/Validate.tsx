import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";
// import validateApiFunctions from "../network/api/ValidateApiFunctions";
import watersApiFunctions from "../network/api/WatersApiFunctions";

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
            endpointName="validate"
        />
    );
};

export default Validate;
