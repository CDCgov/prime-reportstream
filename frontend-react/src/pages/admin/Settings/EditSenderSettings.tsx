import React, { Suspense } from "react";
import {
    Form,
    FormGroup,
    TextInput,
    Label,
    Button,
    GridContainer,
    Grid,
} from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps } from "react-router-dom";

import { ErrorPage } from "../../error/ErrorPage";
import OrgSenderSettingsResource from "../../../resources/OrgSenderSettingsResource";

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
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="format">Format: </Label>
                            </Grid>
                            <Grid col="fill">
                                <TextInput
                                    id={`format-${orgSenderSettings.name}`}
                                    name="format"
                                    type="text"
                                    defaultValue={orgSenderSettings.format}
                                    data-testid="format"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgSenderSettings.format =
                                            e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="topic">Topic: </Label>
                            </Grid>
                            <Grid col="fill">
                                <TextInput
                                    id={`topic-${orgSenderSettings.name}`}
                                    name="topic"
                                    type="text"
                                    defaultValue={orgSenderSettings.topic}
                                    data-testid="topic"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgSenderSettings.topic =
                                            e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="customerStatus">
                                    Customer Status:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <TextInput
                                    id={`customerStatus-${orgSenderSettings.name}`}
                                    name="customerStatus"
                                    type="text"
                                    defaultValue={
                                        orgSenderSettings.customerStatus
                                    }
                                    data-testid="customerStatus"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgSenderSettings.customerStatus =
                                            e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="schemaName">schemaName: </Label>
                            </Grid>
                            <Grid col="fill">
                                <TextInput
                                    id={`schemaName-${orgSenderSettings.name}`}
                                    name="schemaName"
                                    type="text"
                                    defaultValue={orgSenderSettings.schemaName}
                                    data-testid="schemaName"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgSenderSettings.schemaName =
                                            e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
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
