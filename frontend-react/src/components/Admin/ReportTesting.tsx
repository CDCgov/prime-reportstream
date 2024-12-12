import { Button, GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { AdminFormWrapper } from "./AdminFormWrapper";
import { EditReceiverSettingsParams } from "./EditReceiverSettings";
import useReportTesting from "../../hooks/api/reports/UseReportTesting";
import AdminFetchAlert from "../alerts/AdminFetchAlert";
import Spinner from "../Spinner";
import Title from "../Title";
import { Icon } from "../../shared";

function ReportTesting() {
    const { orgname, receivername } = useParams<EditReceiverSettingsParams>();
    const { testMessages, isDisabled, isLoading } = useReportTesting();
    if (isDisabled) {
        return <AdminFetchAlert />;
    }
    if (isLoading || !testMessages) return <Spinner />;

    const RadioField = ({ title, body, index }: { title: string; body: string; index: number }) => {
        const openJsonInNewTab = () => {
            // Your JSON object or string
            const jsonString = JSON.stringify(JSON.parse(body), null, 2);

            // Create a Blob from the JSON string
            const blob = new Blob([jsonString], { type: "application/json" });

            // Create an object URL for the Blob
            const url = URL.createObjectURL(blob);

            // Open the URL in a new browser tab
            window.open(url, "_blank");

            // Revoke the URL after some time to free up memory
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        };
        return (
            <div className="usa-radio bg-base-lightest padding-2 border-bottom-1px border-gray-30">
                <input
                    className="usa-radio__input"
                    id={`message-${index}`}
                    type="radio"
                    name="message-test-form"
                    value={title}
                />
                <label className="usa-radio__label" htmlFor={`message-${index}`}>
                    {title}{" "}
                    <Button type="button" unstyled onClick={openJsonInNewTab}>
                        View message <Icon name="Visibility" />
                    </Button>
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
                    <form>
                        <fieldset className="usa-fieldset bg-base-lightest padding-3">
                            {testMessages?.map((item, index) => (
                                <RadioField key={index} index={index} title={item.fileName} body={item.reportBody} />
                            ))}
                        </fieldset>
                    </form>
                </GridContainer>
            </AdminFormWrapper>
        </>
    );
}

export default ReportTesting;
