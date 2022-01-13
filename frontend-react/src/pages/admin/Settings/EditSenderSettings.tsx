import React, { Suspense } from "react";
import {
    Form,
    FormGroup,
    Button,
    GridContainer,
    Grid,
} from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps } from "react-router-dom";

import { ErrorPage } from "../../error/ErrorPage";
import OrgSenderSettingsResource from "../../../resources/OrgSenderSettingsResource";
import { TextInputComponent } from "../../../components/Admin/AdminFormEdit";

type Props = { orgname: string; sendername: string };

export function EditSenderSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const sendername = match?.params?.sendername || "";

    const FormComponent = () => {
        const orgSenderSettings: OrgSenderSettingsResource = useResource(
            OrgSenderSettingsResource.detail(),
            { orgname, sendername: sendername }
        );

        const { fetch: fetchController } = useController();
        const saveData = async () => {
            try {
                const data = JSON.stringify(orgSenderSettings);
                await fetchController(
                    OrgSenderSettingsResource.update(),
                    { orgname, sendername: sendername },
                    data
                );
            } catch (e) {
                console.trace(e);
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
                                form="edit-setting"
                                type="submit"
                                data-testid="submit"
                                onClick={() => saveData()}
                            >
                                Submit!
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
