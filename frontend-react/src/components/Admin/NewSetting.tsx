import React, { Suspense, useCallback, useState } from "react";
import { Button, GridContainer, Grid } from "@trussworks/react-uswds";
import { useNavigate, useParams } from "react-router-dom";

import { showAlertNotification, showError } from "../AlertNotifications";
import Spinner from "../Spinner";
import { getErrorDetailFromResponse, isProhibitedName } from "../../utils/misc";
import { AuthElement } from "../AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { useCreateOrganizationReceiverSettings } from "../../hooks/api/Settings/UseCreateOrganizationReceiverSettings";
import { useCreateOrganizationSenderSettings } from "../../hooks/api/Settings/UseCreateOrganizationSenderSettings";

import { TextInputComponent, TextAreaComponent } from "./AdminFormEdit";

type NewSettingProps = {
    orgname: string;
    settingtype: "sender" | "receiver";
};

interface NewOrgSettingState {
    setting: {};
    name: string;
}

export function NewSetting() {
    const navigate = useNavigate();
    const { orgname, settingtype } = useParams<NewSettingProps>();
    /* useParams now returns possible undefined. This will error up to a boundary
     * if the url param is undefined */
    if (orgname === undefined || settingtype === undefined)
        throw Error("Expected orgname & settingtype from path");

    const { mutateAsync: createReceiver } =
        useCreateOrganizationReceiverSettings();
    const { mutateAsync: createSender } = useCreateOrganizationSenderSettings();
    const [newOrgSetting, setNewOrgSetting] = useState<NewOrgSettingState>({
        name: "",
        setting: {},
    });

    const saveData = async () => {
        try {
            const { prohibited, errorMsg } = isProhibitedName(
                newOrgSetting.name
            );

            if (prohibited) {
                showError(errorMsg);
                return false;
            }

            let createFn;

            switch (settingtype) {
                case "receiver":
                    createFn = createReceiver;
                    break;
                case "sender":
                    createFn = createSender;
                    break;
                default:
                    throw new Error("Invalid setting type");
            }

            await createFn({
                ...newOrgSetting.setting,
                name: newOrgSetting.name,
            });

            showAlertNotification(
                "success",
                `Item '${newOrgSetting.name}' has been created`
            );
            navigate(-1);
        } catch (e: any) {
            let errorDetail = await getErrorDetailFromResponse(e);
            console.trace(e, errorDetail);
            showError(
                `Updating setting '${newOrgSetting.name}' failed with: ${errorDetail}`
            );
            return false;
        }
        return true;
    };

    const onSettingUpdate = useCallback((value: any, name: string) => {
        setNewOrgSetting((v) => {
            if (name === "orgSettingName") {
                return {
                    ...v,
                    name: value,
                };
            } else {
                return {
                    ...v,
                    setting: value,
                };
            }
        });
    }, []);

    return (
        <section className="grid-container margin-bottom-5">
            <Suspense
                fallback={
                    <span className="text-normal text-base">
                        <Spinner />
                    </span>
                }
            >
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
                        savefunc={onSettingUpdate}
                    />
                    <TextAreaComponent
                        fieldname={"orgSetting"}
                        label={"JSON"}
                        savefunc={onSettingUpdate}
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
            </Suspense>
        </section>
    );
}

export const NewSettingWithAuth = () => (
    <AuthElement
        element={<NewSetting />}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
