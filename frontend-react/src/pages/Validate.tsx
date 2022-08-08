import FileHandler, {
    FileHandlerType,
} from "../components/FileHandlers/FileHandler";
import validateApiFunctions from "../network/api/ValidateApiFunctions";

const Validate = () => {
    return (
        <FileHandler
            headingText="File Validator"
            successMessage="Your file has been validated"
            handlerType={FileHandlerType.VALIDATION}
            resetText="Validate another file"
            submitText="Validate"
            showSuccessMetadata={false}
            fetcher={validateApiFunctions.postData}
            showWarningBanner={false}
        />
    );
};

export default Validate;
