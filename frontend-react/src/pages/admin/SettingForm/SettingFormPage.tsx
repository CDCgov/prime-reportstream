import { useNavigate, useParams } from "react-router";
import { NetworkErrorBoundary, useController } from "rest-hooks";
import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { Helmet } from "react-helmet-async";
import { ModalRef } from "@trussworks/react-uswds";

import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";
import { IRsSetting, RsSettingType } from "../../../config/endpoints/settings";
import { SettingForm } from "../../../shared/SettingFormWithJson/SettingForm/SettingForm";
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
import { FormMode } from "../../../shared/SettingFormWithJson/SettingFormContext/SettingFormContext";

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
    mode: FormMode;
}

export interface SettingFormPageBaseProps extends SettingFormPageSharedProps {
    setting?: RsSettingType;
    orgId: string;
    entityId?: string;
    onError: (e: Error) => void;
    onDelete?: () => void;
    onSubmit: (v: RsSettingType) => void;
    onCancel: () => void;
    isSubmitDisabled?: boolean;
}

export interface SettingFormPageAction {
    action: "delete" | "save";
    setting: RsSettingType;
}

export function SettingFormPageBase({
    entityType,
    mode,
    setting,
    orgId,
    entityId,
    onError,
    onDelete,
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

    const onSubmitHandler = async (setting: RsSettingType) => {
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
                    <section className="grid-container margin-top-0">
                        <SettingForm
                            setting={setting}
                            onDelete={onDeleteHandler}
                            onCancel={onCancel}
                            onSubmit={onSubmitHandler}
                            settingType={entityType.toLowerCase()}
                            isSubmitDisabled={isSubmitDisabled}
                            mode={mode}
                            isCompareAllowed={true}
                            documentType="json"
                        />
                    </section>
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
        | typeof OrgSettingsResource;

    switch (entityType) {
        case "Receiver": {
            resource = OrgReceiverSettingsResource;
            break;
        }
        case "Sender": {
            resource = OrgSenderSettingsResource;
            break;
        }
        case "Organization": {
            resource = OrgSettingsResource;
            break;
        }
        default:
            throw new Error("Unknown entityType");
    }

    const [setting, setSetting] = useState<RsSettingType | undefined>();
    const { fetch: fetchController } = useController();
    const { invalidate } = useController();

    const onErrorHandler = useCallback(
        (e: Error) => {
            toast(e, "error");
        },
        [toast],
    );

    const getSetting = useCallback(async () => {
        return (await fetchController(resource.detail(), {
            orgId,
            entityId,
        })) as Promise<RsSettingType>;
    }, [entityId, fetchController, orgId, resource]);

    const invalidateCache = useCallback(async () => {
        return await invalidate(resource.list(), {
            orgId,
            entityId,
        });
    }, [entityId, invalidate, orgId, resource]);

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

    const refresh = useCallback(async () => {
        await invalidateCache();
        return await getSetting();
    }, [getSetting, invalidateCache]);

    const remove = useCallback(async () => {
        return await fetchController(resource.deleteSetting(), {
            orgId,
            entityId,
        });
    }, [entityId, fetchController, orgId, resource]);

    const onSubmitHandler = useCallback(
        async (setting: IRsSetting) => {
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

            toast(
                `${entityType} '${setting.name}' has been updated`,
                "success",
            );
            navigate(-1);
        },
        [
            entityId,
            entityType,
            fetchController,
            mode,
            navigate,
            orgId,
            refresh,
            resource,
            toast,
        ],
    );

    const onCancelHandler = useCallback(async () => {
        navigate(-1);
    }, [navigate]);

    const onDeleteHandler = useCallback(async () => {
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
    }, [entityType, navigate, remove, setting, toast]);

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
