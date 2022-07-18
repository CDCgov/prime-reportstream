import FileHandler from "../components/FileHandlers/FileHandler";
import watersApiFunctions from "../network/api/WatersApiFunctions";

const Validate = () => {
    return (
        <FileHandler
            headingText="File Validator"
            successMessage="Your file has been validated"
            action="validation"
            formLabel={"Select an HL7 or CSV formatted file to validate."}
            resetText={"Validate another file"}
            submitText={"Validate"}
            showDestinations={false}
            fetcher={watersApiFunctions.postData}
        />
    );
};

export default Validate;
