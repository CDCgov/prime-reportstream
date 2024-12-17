import { GridContainer } from "@trussworks/react-uswds";
import MessageTestingForm from "./MessageTestingForm";

const MessageTestingBody = () => {
    return (
        <GridContainer>
            <p>
                Test a message from the message bank or by entering a custom message. You can view test results in this
                window while you are logged in. To save for later reference, you can open messages, test results and
                output messages in separate tabs.
            </p>
            <hr />
            <p className="font-sans-xl text-bold">Test message bank</p>
            <MessageTestingForm />
        </GridContainer>
    );
};

export default MessageTestingBody;
