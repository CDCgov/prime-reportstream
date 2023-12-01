import { useNavigate, useParams } from "react-router";
import { NetworkErrorBoundary, useController } from "rest-hooks";
import { Suspense, useEffect, useMemo, useState } from "react";
import { Helmet } from "react-helmet-async";

import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import { RSService, RSSetting } from "../../config/endpoints/settings";
import {
    ReceiverForm,
    SenderForm,
    OrganizationForm,
} from "../../shared/SettingForm/SettingForm";
import { useToast } from "../../contexts/Toast";
import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import OrgSettingsResource from "../../resources/OrgSettingsResource";
import { USLink } from "../../components/USLink";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import { OrgReceiverTable } from "../../components/Admin/OrgReceiverTable";
import { OrgSenderTable } from "../../components/Admin/OrgSenderTable";
import HipaaNotice from "../../components/HipaaNotice";

export type SettingFormPageParams = {
    orgId: string;
    entityId: string;
};

function useSetting(
    entityType: "Receiver" | "Sender" | "Organization",
    action: "edit" | "clone",
    orgId: string,
    entityId?: string,
) {
    if (entityType !== "Organization" && action === "edit" && !entityId)
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

    const {
        invalidateCache,
        onErrorHandler,
        refresh,
        remove,
        save,
        getSetting,
        onCancelHandler,
        onDeleteHandler,
        onSaveHandler,
    } = useMemo(() => {
        const onErrorHandler = (e: Error) => {
            toast(e, "error");
        };

        const getSetting = async () => {
            return (await fetchController(resource.detail(), {
                orgId,
                entityId,
                action,
            })) as Promise<RSSetting>;
        };

        const invalidateCache = async () => {
            return await invalidate(resource.list(), {
                orgId,
                entityId,
                action,
            });
        };

        const save = async (newEntity: RSService) => {
            const id = action === "clone" ? newEntity.name : entityId;
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
        action,
        entityId,
        entityType,
        fetchController,
        invalidate,
        navigate,
        orgId,
        resource,
        setting,
        toast,
    ]);

    useEffect(() => {
        if (!setting && action === "edit" && getSetting) {
            getSetting().then((s: RSSetting) => setSetting(s));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return {
        Form,
        setting,
        remove,
        refresh,
        save,
        invalidateCache,
        getSetting,
        onCancel: onCancelHandler,
        onDelete: onDeleteHandler,
        onError: onErrorHandler,
        onSave: onSaveHandler,
        onJsonInvalid: (e: Error) => onErrorHandler(e),
    };
}

export interface SettingFormPageProps {
    entityType: "Receiver" | "Sender" | "Organization";
    mode: "edit" | "new";
}

export default function SettingFormPage({
    entityType,
    mode: _mode,
}: SettingFormPageProps) {
    const { orgId, entityId } = useParams<SettingFormPageParams>();
    const mode = (_mode === "new" && entityId ? "clone" : _mode) as
        | "edit"
        | "clone";
    const {
        Form,
        setting: _setting,
        ...props
    } = useSetting(entityType, mode, orgId!, entityId);
    const modeTitle = `${mode[0].toUpperCase()}${mode.slice(1)}`;

    let historyHref = `/admin/organizations/${orgId}`;
    if (entityType !== "Organization") {
        historyHref += `/${entityType.toLocaleLowerCase()}/${entityId}`;
    }
    historyHref += "/history";

    const setting = _mode === "new" ? {} : _setting;

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
                    {_mode !== "new" && (
                        <>
                            {" - "}
                            <USLink href={historyHref}>History</USLink>
                        </>
                    )}
                </h2>
            </section>
            <NetworkErrorBoundary
                fallbackComponent={() => <ErrorPage type="page" />}
            >
                <Suspense
                    fallback={
                        <span className="text-normal text-base">
                            <Spinner />
                        </span>
                    }
                >
                    {setting && (
                        <Form mode={mode} setting={setting} {...props} />
                    )}
                    {entityType === "Organization" && (
                        <>
                            <OrgSenderTable orgId={orgId!} />
                            <OrgReceiverTable orgId={orgId!} />
                        </>
                    )}
                </Suspense>
            </NetworkErrorBoundary>

            <HipaaNotice />
        </>
    );
}
