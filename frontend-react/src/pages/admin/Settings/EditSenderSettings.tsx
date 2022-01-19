import React, { Suspense } from "react";
import {
    Form,
    FormGroup,
    Button,
    GridContainer,
    Grid,
} from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../error/ErrorPage";
import OrgSenderSettingsResource from "../../../resources/OrgSenderSettingsResource";
import { TextInputComponent } from "../../../components/Admin/AdminFormEdit";
import {
    showAlertNotification,
    showError,
} from "../../../components/AlertNotifications";

type Props = { orgname: string; sendername: string; action: string };

export function EditSenderSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const sendername = match?.params?.sendername || "";
    const action = match?.params?.action || "";
    const history = useHistory();

    const FormComponent = () => {
        const orgSenderSettings: OrgSenderSettingsResource = useResource(
            OrgSenderSettingsResource.detail(),
            { orgname, sendername: sendername, action: action }
        );

        const { fetch: fetchController } = useController();
        const saveData = async () => {
            switch (action) {
                case "edit":
                    try {
                        console.log("EDIT SETTING");
                        const data = JSON.stringify(orgSenderSettings);
                        await fetchController(
                            OrgSenderSettingsResource.update(),
                            { orgname, sendername: sendername },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${sendername}' has been updated`
                        );
                        history.goBack();
                    } catch (e) {
                        console.trace(e);
                        // @ts-ignore
                        showError(`Updating item '${sendername}' failed. ${e.toString()}`);
                        return false;
                    }
                    break;
                case "clone":
                    try {
                        const data = JSON.stringify(orgSenderSettings);
                        await fetchController(
                            OrgSenderSettingsResource.update(),
                            { orgname, sendername: orgSenderSettings.name },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${orgSenderSettings.name}' has been created`
                        );
                        history.goBack();
                    } catch (e) {
                        console.trace(e);
                        // @ts-ignore
                        showError(`Cloning item '${orgSenderSettings.name}' failed. ${e.toString()}`);
                        return false;
                    }
                    break;
                default:
                    return false;
            }

            return true;
        };

        return (
            <GridContainer>
                <Form name="edit-setting" onSubmit={() => saveData()}>
                    <FormGroup>
                        <Grid row>
                            <Grid col="fill">
                                Org name:{" "}
                                {match?.params?.orgname ||
                                    "missing param 'orgname'"}
                                <br />
                                Sender name:{" "}
                                {match?.params?.sendername ||
                                    "missing param 'sendername'"}
                                <br />
                                <br />
                            </Grid>
                        </Grid>
                        <TextInputComponent
                            fieldname={"name"}
                            label={"Name"}
                            defaultvalue={
                                action === "edit" ? orgSenderSettings.name : ""
                            }
                            savefunc={(v) => (orgSenderSettings.name = v)}
                            disabled={action === "edit"}
                        />
                        <TextInputComponent
                            fieldname={"format"}
                            label={"Format"}
                            defaultvalue={orgSenderSettings.format}
                            savefunc={(v) => (orgSenderSettings.format = v)}
                        />
                        <TextInputComponent
                            fieldname={"topic"}
                            label={"Topic"}
                            defaultvalue={orgSenderSettings.topic}
                            savefunc={(v) => (orgSenderSettings.topic = v)}
                        />
                        <TextInputComponent
                            fieldname={"customerStatus"}
                            label={"Customer Status"}
                            defaultvalue={orgSenderSettings.customerStatus}
                            savefunc={(v) =>
                                (orgSenderSettings.customerStatus = v)
                            }
                        />
                        <TextInputComponent
                            fieldname={"schemaName"}
                            label={"Schema Name"}
                            defaultvalue={orgSenderSettings.schemaName}
                            savefunc={(v) => (orgSenderSettings.schemaName = v)}
                        />
                        <Grid row>
                            <Button
                                type="button"
                                onClick={() => history.goBack()}
                            >
                                Cancel
                            </Button>
                            <Button
                                form="edit-setting"
                                type="submit"
                                data-testid="submit"
                                onClick={() => saveData()}
                            >
                                Save
                            </Button>
                        </Grid>
                    </FormGroup>
                </Form>
            </GridContainer>
        );
    };

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Suspense
                fallback={
                    <span className="text-normal text-base">
                        Loading Info...
                    </span>
                }
            >
                <FormComponent />
            </Suspense>
        </NetworkErrorBoundary>
    );
}
