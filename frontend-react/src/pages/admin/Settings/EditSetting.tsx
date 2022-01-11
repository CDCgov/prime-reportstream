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
import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";

type Props = { orgname: string; receivername: string };

export function EditSettings({ match }: RouteComponentProps<Props>) {
    console.log("editsettings");
    const orgname = match?.params?.orgname || "";
    const receivername = match?.params?.receivername || "";

    const FormComponent = (prop: { orgname: string; receivername: string }) => {
        const orgReceiverSettings: OrgReceiverSettingsResource = useResource(
            OrgReceiverSettingsResource.detail(),
            { orgname, receivername }
        );

        const { fetch: fetchController } = useController();
        const saveData = async () => {
            console.log("saveData");
            try {
                const testData = JSON.stringify(orgReceiverSettings);
                await fetchController(
                    OrgReceiverSettingsResource.update(),
                    { orgname, receivername },
                    testData
                );
            } catch (e) {
                console.trace(e);
            }
            return true;
        };

        return (
            <GridContainer>
                <Grid row>
                    <Grid col="fill">
                        Org name:{" "}
                        {match?.params?.orgname || "missing param 'orgname'"}
                        <Form name="edit-setting" onSubmit={() => saveData()}>
                            <FormGroup>
                                <Label htmlFor="topic">Topic: </Label>
                                <TextInput
                                    id={`topic-${orgReceiverSettings.name}`}
                                    name="topic"
                                    type="text"
                                    defaultValue={orgReceiverSettings.topic}
                                    data-testid="topic"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgReceiverSettings.topic =
                                            e.target.value)
                                    }
                                />
                            </FormGroup>
                            <Button
                                form="edit-setting"
                                type="submit"
                                data-testid="submit"
                                onClick={() => saveData()}
                            >
                                Submit!
                            </Button>
                        </Form>
                    </Grid>
                </Grid>
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
                <FormComponent orgname={orgname} receivername={receivername} />
            </Suspense>
        </NetworkErrorBoundary>
    );
}
