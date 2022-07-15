import FileHandler from "../components/FileHandlers/FileHandler";
import watersApiFunctions from "../network/api/WatersApiFunctions";

const Validate = () => {
    return (
        <FileHandler
            headingText="File Validator"
            successMessage="Your file has been validated"
            action="validation"
            fetcher={watersApiFunctions.postData}
        />
    );
};

export default Validate;
