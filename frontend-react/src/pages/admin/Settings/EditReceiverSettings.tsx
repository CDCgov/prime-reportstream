import React, { Suspense } from "react";
import {
    Form,
    FormGroup,
    TextInput,
    Textarea,
    Label,
    Button,
    GridContainer,
    Grid,
    Checkbox,
} from "@trussworks/react-uswds";
import { useResource, NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps } from "react-router-dom";

import { ErrorPage } from "../../error/ErrorPage";
import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";

type Props = { orgname: string; receivername: string };

export function EditReceiverSettings({ match }: RouteComponentProps<Props>) {
    const orgname = match?.params?.orgname || "";
    const receivername = match?.params?.receivername || "";

    const FormComponent = () => {
        const orgReceiverSettings: OrgReceiverSettingsResource = useResource(
            OrgReceiverSettingsResource.detail(),
            { orgname, receivername }
        );

        const { fetch: fetchController } = useController();
        const saveData = async () => {
            try {
                const data = JSON.stringify(orgReceiverSettings);
                await fetchController(
                    OrgReceiverSettingsResource.update(),
                    { orgname, receivername },
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
                                Receiver name:{" "}
                                {match?.params?.receivername ||
                                    "missing param 'receivername'"}
                                <br />
                                <br />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="topic">Topic: </Label>
                            </Grid>
                            <Grid col="fill">
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
                                    id={`customerStatus-${orgReceiverSettings.name}`}
                                    name="customerStatus"
                                    type="text"
                                    defaultValue={
                                        orgReceiverSettings.customerStatus
                                    }
                                    data-testid="customerStatus"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgReceiverSettings.customerStatus =
                                            e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="description">
                                    Description:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <TextInput
                                    id={`description-${orgReceiverSettings.description}`}
                                    name="description"
                                    type="text"
                                    defaultValue={
                                        orgReceiverSettings.description
                                    }
                                    data-testid="description"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgReceiverSettings.description =
                                            e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="translation">
                                    Translation:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <Textarea
                                    id={`translation-${orgReceiverSettings.translation}`}
                                    name="translation"
                                    defaultValue={
                                        JSON.stringify(
                                            orgReceiverSettings?.translation,
                                            null,
                                            "\t"
                                        ) || ""
                                    }
                                    data-testid="description"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLTextAreaElement>
                                    ) =>
                                        (orgReceiverSettings.translation =
                                            JSON.parse(e.target.value || "{}"))
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="description">
                                    Description:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <TextInput
                                    id={`description-${orgReceiverSettings.description}`}
                                    name="description"
                                    type="text"
                                    defaultValue={
                                        orgReceiverSettings.description
                                    }
                                    data-testid="description"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgReceiverSettings.description =
                                            e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="jurisdictionalFilter">
                                    Jurisdictional Filter:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <Textarea
                                    id={`jurisdictionalFilter-${orgReceiverSettings.jurisdictionalFilter}`}
                                    name="jurisdictionalFilter"
                                    defaultValue={
                                        JSON.stringify(
                                            orgReceiverSettings?.jurisdictionalFilter,
                                            null,
                                            "\t"
                                        ) || ""
                                    }
                                    data-testid="jurisdictionalFilter"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLTextAreaElement>
                                    ) =>
                                        (orgReceiverSettings.jurisdictionalFilter =
                                            JSON.parse(e.target.value || "{}"))
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="qualityFilter">
                                    Quality Filter:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <Textarea
                                    id={`qualityFilter-${orgReceiverSettings.qualityFilter}`}
                                    name="qualityFilter"
                                    defaultValue={
                                        JSON.stringify(
                                            orgReceiverSettings?.qualityFilter,
                                            null,
                                            "\t"
                                        ) || ""
                                    }
                                    data-testid="qualityFilter"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLTextAreaElement>
                                    ) =>
                                        (orgReceiverSettings.qualityFilter =
                                            JSON.parse(e.target.value || "{}"))
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="reverseTheQualityFilter">
                                    Reverse the Quality Filter:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <Checkbox
                                    id={`reverseTheQualityFilter-${orgReceiverSettings.reverseTheQualityFilter}`}
                                    name="reverseTheQualityFilter"
                                    data-testid="reverseTheQualityFilter"
                                    defaultChecked={
                                        orgReceiverSettings.reverseTheQualityFilter
                                    }
                                    label=""
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgReceiverSettings.reverseTheQualityFilter =
                                            e.target.checked)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="routingFilter">
                                    Routing Filter:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <Textarea
                                    id={`routingFilter-${orgReceiverSettings.routingFilter}`}
                                    name="routingFilter"
                                    defaultValue={
                                        JSON.stringify(
                                            orgReceiverSettings?.routingFilter,
                                            null,
                                            "\t"
                                        ) || ""
                                    }
                                    data-testid="routingFilter"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLTextAreaElement>
                                    ) =>
                                        (orgReceiverSettings.routingFilter =
                                            JSON.parse(e.target.value || "{}"))
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="processingModeFilter">
                                    Processing Mode Filter:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <Textarea
                                    id={`processingModeFilter-${orgReceiverSettings.processingModeFilter}`}
                                    name="processingModeFilter"
                                    defaultValue={
                                        JSON.stringify(
                                            orgReceiverSettings?.processingModeFilter,
                                            null,
                                            "\t"
                                        ) || ""
                                    }
                                    data-testid="processingModeFilter"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLTextAreaElement>
                                    ) =>
                                        (orgReceiverSettings.processingModeFilter =
                                            JSON.parse(e.target.value || "{}"))
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="deidentify">
                                    De-identify:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <Checkbox
                                    id={`deidentify-${orgReceiverSettings.deidentify}`}
                                    name="deidentify"
                                    data-testid="deidentify"
                                    defaultChecked={
                                        orgReceiverSettings.deidentify
                                    }
                                    label=""
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgReceiverSettings.deidentify =
                                            e.target.checked)
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="timing">Timing: </Label>
                            </Grid>
                            <Grid col="fill">
                                <Textarea
                                    id={`timing-${orgReceiverSettings.timing}`}
                                    name="timing"
                                    defaultValue={
                                        JSON.stringify(
                                            orgReceiverSettings?.timing,
                                            null,
                                            "\t"
                                        ) || ""
                                    }
                                    data-testid="timing"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLTextAreaElement>
                                    ) =>
                                        (orgReceiverSettings.timing =
                                            JSON.parse(e.target.value || "{}"))
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="transport">Transport: </Label>
                            </Grid>
                            <Grid col="fill">
                                <Textarea
                                    id={`transport-${orgReceiverSettings.transport}`}
                                    name="transport"
                                    defaultValue={
                                        JSON.stringify(
                                            orgReceiverSettings?.transport,
                                            null,
                                            "\t"
                                        ) || ""
                                    }
                                    data-testid="transport"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLTextAreaElement>
                                    ) =>
                                        (orgReceiverSettings.transport =
                                            JSON.parse(e.target.value || "{}"))
                                    }
                                />
                            </Grid>
                        </Grid>
                        <Grid row>
                            <Grid col="fill">
                                <Label htmlFor="externalName">
                                    External Name:{" "}
                                </Label>
                            </Grid>
                            <Grid col="fill">
                                <TextInput
                                    id={`externalName-${orgReceiverSettings.externalName}`}
                                    name="externalName"
                                    type="text"
                                    defaultValue={
                                        orgReceiverSettings.externalName
                                    }
                                    data-testid="externalName"
                                    maxLength={255}
                                    onChange={(
                                        e: React.ChangeEvent<HTMLInputElement>
                                    ) =>
                                        (orgReceiverSettings.externalName =
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
