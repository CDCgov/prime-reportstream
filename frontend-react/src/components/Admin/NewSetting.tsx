import React, { Suspense } from "react";
import { Button, GridContainer, Grid } from "@trussworks/react-uswds";
import { NetworkErrorBoundary, useController } from "rest-hooks";
import { RouteComponentProps, useHistory } from "react-router-dom";

import { ErrorPage } from "../../pages/error/ErrorPage";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";

import { TextInputComponent, TextAreaComponent } from "./AdminFormEdit";

type Props = {
    orgname: string;
    settingtype: string;
};

export function NewSetting({ match }: RouteComponentProps<Props>) {
    const history = useHistory();
    const orgname = match?.params?.orgname || "";
    const settingtype = match?.params?.settingtype || "";

    const FormComponent = () => {
        let orgSetting: object = [];
        let orgSettingName: string = "";

        const { fetch: fetchController } = useController();
        const saveData = async () => {
            const data = orgSetting;
            switch (settingtype) {
                case "sender":
                    try {
                        await fetchController(
                            OrgSenderSettingsResource.update(),
                            { orgname, sendername: orgSettingName },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${orgSettingName}' has been created`
                        );
                        history.goBack();
                    } catch (e: any) {
                        console.trace(e);

                        showError(
                            `Creating item '${orgSettingName}' failed. ${e.toString()}`
                        );
                        return false;
                    }
                    break;
                case "receiver":
                    try {
                        await fetchController(
                            OrgReceiverSettingsResource.update(),
                            { orgname, receivername: orgSettingName },
                            data
                        );
                        showAlertNotification(
                            "success",
                            `Item '${orgSettingName}' has been created`
                        );
                        history.goBack();
                    } catch (e: any) {
                        console.trace(e);

                        showError(
                            `Creating item '${orgSettingName}' failed. ${e.toString()}`
                        );
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
                <Grid row>
                    <Grid col="fill" className="text-bold">
                        Org name:{" "}
                        {match?.params?.orgname || "missing param 'orgname'"}
                        <br />
                        Setting Type:{" "}
                        {match?.params?.settingtype ||
                            "missing param 'settingtype' (should be 'sender' or 'receiver')"}
                        <br />
                        <br />
                    </Grid>
                </Grid>
                <TextInputComponent
                    fieldname={"orgSettingName"}
                    label={"Name"}
                    defaultvalue=""
                    savefunc={(v) => (orgSettingName = v)}
                />
                <TextAreaComponent
                    fieldname={"orgSetting"}
                    label={"JSON"}
                    savefunc={(v) => (orgSetting = v)}
                    defaultvalue={[]}
                    defaultnullvalue="[]"
                />
                <Grid row>
                    <Button type="button" onClick={() => history.goBack()}>
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
            </GridContainer>
        );
    };

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <section className="grid-container margin-bottom-5">
                <Suspense
                    fallback={
                        <span className="text-normal text-base">
                            Loading Sender Settings Info...
                        </span>
                    }
                >
                    <FormComponent />
                </Suspense>
            </section>
        </NetworkErrorBoundary>
    );
}
