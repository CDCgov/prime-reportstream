import { Form, GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { AdminFormWrapper } from "./AdminFormWrapper";
import { EditReceiverSettingsParams } from "./EditReceiverSettings";
import useReportTesting from "../../hooks/api/reports/UseReportTesting";
import AdminFetchAlert from "../alerts/AdminFetchAlert";
import Spinner from "../Spinner";
import Title from "../Title";

function ReportTesting() {
    const { orgname, receivername } = useParams<EditReceiverSettingsParams>();
    const { testMessages, isDisabled, isLoading } = useReportTesting();
    if (isDisabled) {
        return <AdminFetchAlert />;
    }
    if (isLoading || !testMessages) return <Spinner />;

    const RadioField = ({ title, index }: { title: string; index: number }) => {
        return (
            <div className="usa-radio bg-base-lighter padding-2 border-bottom-1px border-gray-30">
                <input
                    className="usa-radio__input"
                    id={`message-${index}`}
                    type="radio"
                    name="message-test-form"
                    value={title}
                />
                <label className="usa-radio__label margin-0" htmlFor={`message-${index}`}>
                    {title}
                </label>
            </div>
        );
    };

    console.log("testMessages = ", testMessages);
    return (
        <>
            <Helmet>
                <title>Message testing - ReportStream</title>
            </Helmet>
            <AdminFormWrapper
                header={
                    <>
                        <Title title={"Message testing"} />
                        <h2 className="margin-bottom-0">
                            <span className="text-normal font-body-md text-base margin-bottom-0">
                                Org name: {orgname}
                                <br />
                                Receiver name: {receivername}
                            </span>
                        </h2>
                    </>
                }
            >
                <GridContainer>
                    <p>
                        Test a message from the message bank or by entering a custom message. You can view test results
                        in this window while you are logged in. To save for later reference, you can open messages, test
                        results and output messages in separate tabs. 
                    </p>
                    <hr />
                    <p className="font-sans-xl text-bold">Test message bank</p>
                    <Form className="width-full maxw-full">
                        <fieldset className="usa-fieldset bg-base-lighter padding-3">
                            {testMessages?.map((item, index) => (
                                <RadioField key={index} title={item.fileName} index={index} />
                            ))}
                        </fieldset>
                    </Form>
                </GridContainer>
            </AdminFormWrapper>
        </>
    );
}

export default ReportTesting;
