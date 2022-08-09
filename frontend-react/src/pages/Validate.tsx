import watersApiFunctions from "../network/api/WatersApiFunctions";
import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";

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
        />
    );
};

export default Validate;
