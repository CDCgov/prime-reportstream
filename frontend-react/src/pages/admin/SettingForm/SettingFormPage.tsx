import { useNavigate, useParams } from "react-router";
import { NetworkErrorBoundary, useController } from "rest-hooks";
import {
    Suspense,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import { Helmet } from "react-helmet-async";
import { ModalRef } from "@trussworks/react-uswds";

import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";
import { RSService, RSSetting } from "../../../config/endpoints/settings";
import {
    ReceiverForm,
    SenderForm,
    OrganizationForm,
    SettingFormProps,
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
import { jsonSortReplacer } from "../../../utils/JsonSortReplacer";
import {
    getVersionWarning,
    VersionWarningType,
    getErrorDetailFromResponse,
} from "../../../utils/misc";

import {
    ModalConfirmButton,
    ModalConfirmDialog,
} from "./ModalConfirmDialog/ModalConfirmDialog";
import { SettingFormItem } from "../../../shared/SettingFormWithJson/SettingFormContext/SettingFormContext";

export type SettingFormPageParams = {
    orgId: string;
    entityId: string;
};

export interface SettingFormPageSharedProps {
    entityType: "Receiver" | "Sender" | "Organization";
    mode: "edit" | "new";
}

export interface SettingFormPageBaseProps extends SettingFormPageSharedProps {
    Form: (props: SettingFormProps<any>) => JSX.Element;
    setting: RSSetting;
    orgId: string;
    entityId?: string;
    onError: (e: Error) => void;
    remove: () => Promise<void>;
    onDelete: () => void;
    refresh: () => Promise<void>;
    save: (v: RSSetting) => Promise<void>;
    onSave: (v: RSSetting) => void;
    onCancel: () => void;
}

export function SettingFormPageBase({
    entityType,
    mode,
    setting,
    orgId,
    entityId,
    onError,
    remove,
    onDelete,
    refresh,
    save,
    Form,
    onCancel,
    onSave,
}: SettingFormPageBaseProps) {
    const initialSettingRef = useRef(setting);
    const modeTitle = `${mode[0].toUpperCase()}${mode.slice(1)}`;

    let historyHref = `/admin/organizations/${orgId}`;
    if (entityType !== "Organization") {
        historyHref += `/${entityType.toLocaleLowerCase()}/${entityId}`;
    }
    historyHref += "/history";

    const modalRef = useRef<ModalRef>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [settingAction, setSettingAction] = useState<{
        action: "delete" | "save";
        setting?: SettingFormItem<RSSetting>;
    }>();

    useEffect(() => {
        if (
            mode === "edit" &&
            setting.version !== initialSettingRef.current.version
        ) {
            onError(new Error(getVersionWarning(VersionWarningType.POPUP)));
        }
    }, [mode, onError, setting]);

    const onDeleteHandler = () => {
        if (setting.name) {
            modalRef?.current?.toggleModal();
            setSettingAction({ action: "delete" });
        }
    };

    const onDeleteConfirmHandler = async (id: string) => {
        try {
            await remove();
            onDelete();
        } catch (e: any) {
            onError(new Error(`Deleting item '${id}' failed. ${e.toString()}`));
        }
    };

    const onSaveHandler = async (setting: SettingFormItem<RSSetting>) => {
        try {
            if (mode === "edit") {
                // fetch original version
                setIsLoading(true);
                await refresh();
                setIsLoading(false);
            }
            modalRef?.current?.toggleModal();
            setSettingAction({ action: "save", setting });
        } catch (e: any) {
            setIsLoading(false);
            const errorDetail = await getErrorDetailFromResponse(e);
            onError(
                new Error(
                    `Reloading ${entityType} '${setting.name}' failed with: ${errorDetail}`,
                ),
            );
        }
    };

    const onSaveConfirmHandler = useCallback(async () => {
        try {
            if (settingAction?.action !== "save")
                throw new TypeError("Invalid setting action");
            if (!settingAction?.setting)
                throw new TypeError("Setting for action is undefined");
            setIsLoading(true);

            if (mode === "edit") {
                await refresh();
            }

            await save(settingAction.setting);
            setIsLoading(false);

            modalRef?.current?.toggleModal();
        } catch (e: any) {
            setIsLoading(false);
            let errorDetail = await getErrorDetailFromResponse(e);
            onError(
                new Error(
                    `Updating ${entityType} '${setting.name}' failed with: ${errorDetail}`,
                ),
            );
        }
    }, [
        entityType,
        mode,
        onError,
        refresh,
        save,
        setting.name,
        settingAction?.action,
        settingAction?.setting,
    ]);

    const onCancelHandler = useCallback(() => {
        onCancel();
    }, [onCancel]);

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
                        mode={mode}
                        initialValues={setting}
                        onCancel={onCancelHandler}
                        onDelete={onDeleteHandler}
                        onSave={onSaveHandler}
                        isSave={!isLoading}
                    />
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
                        actionButton={
                            <ModalConfirmButton
                                type="button"
                                secondary={settingAction?.action === "delete"}
                                onClick={
                                    settingAction?.action === "delete"
                                        ? onDeleteConfirmHandler
                                        : onSaveConfirmHandler
                                }
                            >
                                {settingAction?.action === "delete" ? (
                                    <>Delete</>
                                ) : (
                                    <>Save</>
                                )}
                            </ModalConfirmButton>
                        }
                    >
                        <pre>
                            {JSON.stringify(settingAction?.setting ?? {})}
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
    mode: _mode,
}: SettingFormPageProps) {
    const { orgId, entityId } = useParams<SettingFormPageParams>();
    const mode = (_mode === "new" && entityId ? "clone" : _mode) as
        | "edit"
        | "clone";

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

    const { getSetting, ...props } = useMemo(() => {
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

        const save = async (newEntity: RSService) => {
            const id = mode === "clone" ? newEntity.name : entityId;
            await fetchController(
                resource.update(),
                { orgId, entityId: id },
                newEntity,
            );
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
            await getSetting();
        }

        const remove = async () => {
            await fetchController(resource.deleteSetting(), {
                orgId,
                entityId,
            });
        };

        if (!setting) {
            return {
                remove,
                refresh,
                save,
                invalidateCache,
                getSetting,
                onErrorHandler,
                onCancelHandler: () => void 0,
                onDeleteHandler: () => void 0,
                onSaveHandler: () => void 0,
            } as any;
        }

        const onSaveHandler = async () => {
            toast(
                `${entityType} '${setting.name}' has been updated`,
                "success",
            );
            navigate(-1);
        };

        const onCancelHandler = async () => {
            navigate(-1);
        };

        const onDeleteHandler = () => {
            toast(
                `${entityType} '${setting.name}' has been deleted`,
                "success",
            );

            // navigate back to list since this item was just deleted
            navigate(-1);
        };

        return {
            remove,
            refresh,
            save,
            invalidateCache,
            onErrorHandler,
            onCancelHandler,
            onDeleteHandler,
            onSaveHandler,
            getSetting,
        };
    }, [
        entityId,
        entityType,
        fetchController,
        invalidate,
        mode,
        navigate,
        orgId,
        resource,
        setting,
        toast,
    ]);

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
                    {...props}
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
