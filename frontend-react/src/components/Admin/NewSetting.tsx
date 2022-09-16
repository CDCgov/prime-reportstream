import React, { Suspense } from "react";
import { Button, GridContainer, Grid } from "@trussworks/react-uswds";
import { NetworkErrorBoundary, useController } from "rest-hooks";
import { useNavigate, useParams } from "react-router-dom";

import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";
import Spinner from "../Spinner";
import { getErrorDetailFromResponse } from "../../utils/misc";
import { AuthElement } from "../AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { ErrorPage } from "../../pages/error/ErrorPage";

import { TextInputComponent, TextAreaComponent } from "./AdminFormEdit";

type NewSettingProps = {
    orgname: string;
    settingtype: "sender" | "receiver";
};

export function NewSetting() {
    const navigate = useNavigate();
    const { orgname, settingtype } = useParams<NewSettingProps>();
    /* useParams now returns possible undefined. This will error up to a boundary
     * if the url param is undefined */
    if (orgname === undefined || settingtype === undefined)
        throw Error("Expected orgname & settingtype from path");

    const FormComponent = () => {
        let orgSetting: object = [];
        let orgSettingName: string = "";

        const { fetch: fetchController } = useController();
        const saveData = async () => {
            try {
                const data = orgSetting;
                const SETTINGTYPE = {
                    sender: {
                        updateFn: OrgSenderSettingsResource.update(),
                        args: { orgname: orgname, sendername: orgSettingName },
                    },
                    receiver: {
                        updateFn: OrgReceiverSettingsResource.update(),
                        args: {
                            orgname: orgname,
                            receivername: orgSettingName,
                        },
                    },
                };

                await fetchController(
                    SETTINGTYPE[settingtype].updateFn,
                    SETTINGTYPE[settingtype].args,
                    data
                );

                showAlertNotification(
                    "success",
                    `Item '${orgSettingName}' has been created`
                );
                navigate(-1);
            } catch (e: any) {
                let errorDetail = await getErrorDetailFromResponse(e);
                console.trace(e, errorDetail);
                showError(
                    `Updating setting '${orgSettingName}' failed with: ${errorDetail}`
                );
                return false;
            }
            return true;
        };

        return (
            <GridContainer>
                <Grid row>
                    <Grid col="fill" className="text-bold">
                        Org name: {orgname}
                        <br />
                        Setting Type: {settingtype}
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
                    <Button type="button" onClick={() => navigate(-1)}>
                        Cancel
                    </Button>
                    <Button
                        form="edit-setting"
                        type="submit"
                        data-testid="submit"
                        onClick={async () => await saveData()}
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
                            <Spinner />
                        </span>
                    }
                >
                    <FormComponent />
                </Suspense>
            </section>
        </NetworkErrorBoundary>
    );
}

export const NewSettingWithAuth = () => (
    <AuthElement
        element={<NewSetting />}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
