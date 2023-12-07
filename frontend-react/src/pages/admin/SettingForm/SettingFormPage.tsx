import { useNavigate, useParams } from "react-router";
import { NetworkErrorBoundary, useController } from "rest-hooks";
import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { Helmet } from "react-helmet-async";
import { Button, Grid, ModalRef } from "@trussworks/react-uswds";

import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";
import { RSSetting } from "../../../config/endpoints/settings";
import {
    ReceiverForm,
    SenderForm,
    OrganizationForm,
    SettingFormProps,
    SettingFormMode,
} from "../../../shared/SettingFormWithJson/SettingForm/SettingForm";
import { useToast } from "../../../contexts/Toast";
import OrgSenderSettingsResource from "../../../resources/OrgSenderSettingsResource";
import OrgSettingsResource from "../../../resources/OrgSettingsResource";
import { USLink } from "../../../components/USLink";
import Spinner from "../../../components/Spinner";
import { ErrorPage } from "../../error/ErrorPage";
import { OrgReceiverTable } from "../../../components/Admin/OrgReceiverTable";
import { OrgSenderTable } from "../../../components/Admin/OrgSenderTable";
import HipaaNotice from "../../../components/HipaaNotice";
import {
    getVersionWarning,
    VersionWarningType,
    getErrorDetailFromResponse,
} from "../../../utils/misc";
import MetaDisplay from "../../../shared/MetaDisplay/MetaDisplay";

import {
    ModalConfirmButton,
    ModalConfirmDialog,
} from "./ModalConfirmDialog/ModalConfirmDialog";

export type SettingFormPageParams = {
    orgId: string;
    entityId: string;
};

export interface SettingFormPageSharedProps {
    entityType: "Receiver" | "Sender" | "Organization";
    mode: SettingFormMode;
}

export interface SettingFormPageBaseProps extends SettingFormPageSharedProps {
    Form: (props: SettingFormProps) => JSX.Element;
    setting?: RSSetting;
    orgId: string;
    entityId?: string;
    onError: (e: Error) => void;
    onDelete?: () => void;
    onSubmit: (v: RSSetting) => void;
    onCancel: () => void;
    isSubmitDisabled?: boolean;
}

export interface SettingFormPageAction {
    action: "delete" | "save";
    setting: RSSetting;
}

export function SettingFormPageBase({
    entityType,
    mode,
    setting,
    orgId,
    entityId,
    onError,
    onDelete,
    Form,
    onCancel,
    onSubmit,
    isSubmitDisabled,
}: SettingFormPageBaseProps) {
    const initialSettingRef = useRef(setting);
    const modeTitle = `${mode[0].toUpperCase()}${mode.slice(1)}`;

    let historyHref = `/admin/organizations/${orgId}`;
    if (entityType !== "Organization") {
        historyHref += `/${entityType.toLocaleLowerCase()}/${entityId}`;
    }
    historyHref += "/history";

    const modalRef = useRef<ModalRef>(null);
    const [settingAction, setSettingAction] = useState<SettingFormPageAction>();

    useEffect(() => {
        if (
            setting &&
            initialSettingRef.current &&
            mode === "edit" &&
            setting.version !== initialSettingRef.current.version
        ) {
            onError(new Error(getVersionWarning(VersionWarningType.POPUP)));
        }
    }, [mode, onError, setting]);

    const onDeleteHandler = () => {
        if (!setting?.name) throw TypeError("No setting to delete");
        setSettingAction({ action: "delete", setting });
    };

    const onSubmitHandler = async (setting: RSSetting) => {
        setSettingAction({ action: "save", setting });
    };

    const onSubmitConfirmHandler = useCallback(async () => {
        if (!settingAction) throw new TypeError("Setting action undefined");
        if (settingAction?.action !== "save")
            throw new TypeError("Invalid setting action");
        if (!settingAction?.setting)
            throw new TypeError("Setting for action is undefined");

        onSubmit(settingAction.setting);

        setSettingAction(undefined);
    }, [onSubmit, settingAction]);

    const onConfirmCancelHandler = useCallback(() => {
        setSettingAction(undefined);
    }, []);

    const onDeleteConfirmHandler = useCallback(() => {
        if (!onDelete) throw new TypeError("onDelete not provided");
        onDelete();
        setSettingAction(undefined);
    }, [onDelete]);

    useEffect(() => {
        if (settingAction) modalRef.current?.toggleModal(undefined, true);
        else modalRef.current?.toggleModal(undefined, false);
    }, [settingAction]);

    return (
        <>
            <Helmet>
                <title>
                    Admin | {entityType} {modeTitle}
                </title>
            </Helmet>
            <section className="grid-container margin-top-3 margin-bottom-5">
                <h2>
                    {modeTitle} {entityType}:{" "}
                    {entityId ?? entityType === "Organization" ? orgId : ""}
                    {mode !== "new" && (
                        <>
                            {" - "}
                            <USLink href={historyHref}>History</USLink>
                        </>
                    )}
                </h2>
            </section>

            {setting && (
                <>
                    <section className="grid-container margin-top-0"></section>
                    <Form
                        documentType="JSON"
                        isCompare={true}
                        mode={mode}
                        initialValues={setting}
                        onSubmit={onSubmitHandler}
                        isSubmitDisabled={isSubmitDisabled}
                        leftButtons={
                            <>
                                {onDelete && (
                                    <Button
                                        type={"button"}
                                        secondary={true}
                                        onClick={onDeleteHandler}
                                    >
                                        Delete...
                                    </Button>
                                )}
                            </>
                        }
                        rightButtons={
                            <>
                                <Button
                                    type={"button"}
                                    outline
                                    onClick={onCancel}
                                >
                                    Cancel
                                </Button>
                            </>
                        }
                    ></Form>

                    <Grid row>
                        <Grid col={3}>Meta:</Grid>
                        <Grid col={9}>
                            <MetaDisplay metaObj={setting} />
                            <br />
                        </Grid>
                    </Grid>
                    <ModalConfirmDialog
                        id={"actionConfirm"}
                        ref={modalRef}
                        heading={
                            settingAction?.action === "delete" ? (
                                <>
                                    Are you sure you want to delete {entityType}
                                    : {setting.name}?
                                </>
                            ) : (
                                <>
                                    Please verify changes before saving{" "}
                                    {entityType}: {setting.name}
                                </>
                            )
                        }
                        onCancel={onConfirmCancelHandler}
                        actionButton={
                            <ModalConfirmButton
                                type="button"
                                secondary={settingAction?.action === "delete"}
                                onClick={
                                    settingAction?.action === "delete"
                                        ? onDeleteConfirmHandler
                                        : onSubmitConfirmHandler
                                }
                            >
                                {settingAction?.action === "delete" ? (
                                    <>Delete</>
                                ) : (
                                    <>Submit</>
                                )}
                            </ModalConfirmButton>
                        }
                    >
                        <pre>
                            {JSON.stringify(
                                settingAction?.setting ?? {},
                                undefined,
                                2,
                            )}
                        </pre>
                    </ModalConfirmDialog>
                </>
            )}
            {entityType === "Organization" && (
                <>
                    <OrgSenderTable orgId={orgId} />
                    <OrgReceiverTable orgId={orgId} />
                </>
            )}

            <HipaaNotice />
        </>
    );
}

export interface SettingFormPageProps extends SettingFormPageSharedProps {}

export default function SettingFormPage({
    entityType,
    mode,
}: SettingFormPageProps) {
    const [isLoading, setIsLoading] = useState(false);
    const { orgId, entityId } = useParams<SettingFormPageParams>();

    if (entityType !== "Organization" && mode === "edit" && !entityId)
        throw new Error("Missing entity id");

    const navigate = useNavigate();
    const { toast } = useToast();

    let resource:
            | typeof OrgReceiverSettingsResource
            | typeof OrgSenderSettingsResource
            | typeof OrgSettingsResource,
        Form;

    switch (entityType) {
        case "Receiver": {
            resource = OrgReceiverSettingsResource;
            Form = ReceiverForm;
            break;
        }
        case "Sender": {
            resource = OrgSenderSettingsResource;
            Form = SenderForm;
            break;
        }
        case "Organization": {
            resource = OrgSettingsResource;
            Form = OrganizationForm;
            break;
        }
        default:
            throw new Error("Unknown entityType");
    }

    const [setting, setSetting] = useState<RSSetting | undefined>();
    const { fetch: fetchController } = useController();
    const { invalidate } = useController();

    const onErrorHandler = (e: Error) => {
        toast(e, "error");
    };

    const getSetting = async () => {
        return (await fetchController(resource.detail(), {
            orgId,
            entityId,
        })) as Promise<RSSetting>;
    };

    const invalidateCache = async () => {
        return await invalidate(resource.list(), {
            orgId,
            entityId,
        });
    };

    /*async function refresh() {
        const accessToken = authState.accessToken?.accessToken;
        const organization = activeMembership?.parsedName;

        const response = await fetch(
            `${config.RS_API_URL}/api/settings/organizations/${orgId}/receivers/${receiverId}`,
            {
                headers: {
                    ...fetchHeaders(),
                    Authorization: `Bearer ${accessToken}`,
                    Organization: organization!,
                },
            },
        );

        return (await response.json()) as RSReceiver;
    }*/

    async function refresh() {
        await invalidateCache();
        return await getSetting();
    }

    const remove = async () => {
        return await fetchController(resource.deleteSetting(), {
            orgId,
            entityId,
        });
    };

    const onSubmitHandler = async (setting: RSSetting) => {
        setIsLoading(true);

        if (mode === "edit") {
            try {
                await refresh();
            } catch (e: any) {
                const errorDetail = await getErrorDetailFromResponse(e);
                toast(
                    new Error(
                        `Reloading ${entityType} '${setting.name}' failed with: ${errorDetail}`,
                    ),
                    "error",
                );
            }
        }

        const id = mode === "clone" ? setting.name : entityId;
        try {
            await fetchController(
                resource.update(),
                { orgId, entityId: id },
                setting,
            );
        } catch (e: any) {
            setIsLoading(false);
            let errorDetail = await getErrorDetailFromResponse(e);
            toast(
                new Error(
                    `Updating ${entityType} '${setting.name}' failed with: ${errorDetail}`,
                ),
                "error",
            );
            return;
        }

        toast(`${entityType} '${setting.name}' has been updated`, "success");
        navigate(-1);
    };

    const onCancelHandler = async () => {
        navigate(-1);
    };

    const onDeleteHandler = async () => {
        if (!setting) throw new Error("No setting to delete");

        try {
            await remove();
        } catch (e: any) {
            toast(
                new Error(
                    `Deleting ${entityType} '${
                        setting.name
                    }' failed. ${e.toString()}`,
                ),
                "error",
            );
            return;
        }

        toast(`${entityType} '${setting.name}' has been deleted`, "success");

        // navigate back to list since this item was just deleted
        navigate(-1);
    };

    useEffect(() => {
        if (!setting && mode === "edit" && getSetting) {
            getSetting().then(setSetting);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <>
            {setting && (
                <SettingFormPageBase
                    orgId={orgId!}
                    entityId={entityId}
                    Form={Form}
                    setting={setting}
                    entityType={entityType}
                    mode={mode}
                    onError={onErrorHandler}
                    onSubmit={onSubmitHandler}
                    onCancel={onCancelHandler}
                    onDelete={onDeleteHandler}
                    isSubmitDisabled={isLoading}
                />
            )}
        </>
    );
}

export function Todo() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Suspense
                fallback={
                    <span className="text-normal text-base">
                        <Spinner />
                    </span>
                }
            ></Suspense>
        </NetworkErrorBoundary>
    );
}
