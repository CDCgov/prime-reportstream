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
import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";
import {
    CheckboxComponent,
    TextAreaComponent,
    TextInputComponent,
} from "../../../components/Admin/AdminFormEdit";

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
                        <TextInputComponent
                            fieldname={"topic"}
                            label={"Topic"}
                            defaultvalue={orgReceiverSettings.topic}
                            savefunc={(v) => (orgReceiverSettings.topic = v)}
                        />
                        <TextInputComponent
                            fieldname={"customerStatus"}
                            label={"Customer Status"}
                            defaultvalue={orgReceiverSettings.customerStatus}
                            savefunc={(v) =>
                                (orgReceiverSettings.customerStatus = v)
                            }
                        />
                        <TextInputComponent
                            fieldname={"description"}
                            label={"Description"}
                            defaultvalue={orgReceiverSettings.description}
                            savefunc={(v) =>
                                (orgReceiverSettings.description = v)
                            }
                        />
                        <TextAreaComponent
                            fieldname={"translation"}
                            label={"Translation"}
                            defaultvalue={orgReceiverSettings.translation}
                            savefunc={(v) =>
                                (orgReceiverSettings.translation = v)
                            }
                        />
                        <TextAreaComponent
                            fieldname={"jurisdictionalFilter"}
                            label={"Jurisdictional Filter"}
                            defaultvalue={
                                orgReceiverSettings.jurisdictionalFilter
                            }
                            savefunc={(v) =>
                                (orgReceiverSettings.jurisdictionalFilter = v)
                            }
                        />
                        <TextAreaComponent
                            fieldname={"qualityFilter"}
                            label={"Quality Filter"}
                            defaultvalue={orgReceiverSettings.qualityFilter}
                            savefunc={(v) =>
                                (orgReceiverSettings.qualityFilter = v)
                            }
                        />
                        <CheckboxComponent
                            fieldname={"reverseTheQualityFilter"}
                            label={"Reverse the Quality Filter"}
                            defaultvalue={
                                orgReceiverSettings.reverseTheQualityFilter
                            }
                            savefunc={(v) =>
                                (orgReceiverSettings.reverseTheQualityFilter =
                                    v)
                            }
                        />
                        <TextAreaComponent
                            fieldname={"routingFilter"}
                            label={"Routing Filter"}
                            defaultvalue={orgReceiverSettings.routingFilter}
                            savefunc={(v) =>
                                (orgReceiverSettings.routingFilter = v)
                            }
                        />
                        <TextAreaComponent
                            fieldname={"processingModeFilter"}
                            label={"Processing Mode Filter"}
                            defaultvalue={
                                orgReceiverSettings.processingModeFilter
                            }
                            savefunc={(v) =>
                                (orgReceiverSettings.processingModeFilter = v)
                            }
                        />
                        <CheckboxComponent
                            fieldname={"deidentify"}
                            label={"De-identify"}
                            defaultvalue={orgReceiverSettings.deidentify}
                            savefunc={(v) =>
                                (orgReceiverSettings.deidentify = v)
                            }
                        />
                        <TextAreaComponent
                            fieldname={"timing"}
                            label={"Timing"}
                            defaultvalue={orgReceiverSettings.timing}
                            savefunc={(v) => (orgReceiverSettings.timing = v)}
                        />
                        <TextAreaComponent
                            fieldname={"transport"}
                            label={"Transport"}
                            defaultvalue={orgReceiverSettings.transport}
                            savefunc={(v) =>
                                (orgReceiverSettings.transport = v)
                            }
                        />
                        <TextInputComponent
                            fieldname={"externalName"}
                            label={"External Name"}
                            defaultvalue={orgReceiverSettings.externalName}
                            savefunc={(v) =>
                                (orgReceiverSettings.externalName = v)
                            }
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
