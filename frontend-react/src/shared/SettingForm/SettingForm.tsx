import React, {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import { Button, Grid, GridContainer } from "@trussworks/react-uswds";

import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import {
    getErrorDetailFromResponse,
    getVersionWarning,
    VersionWarningType,
} from "../../utils/misc";
import {
    ModalConfirmDialog,
    ModalConfirmRef,
} from "../../components/ModalConfirmDialog";
import { RSService } from "../../config/endpoints/settings";

import {
    ConfirmSaveModal,
    ConfirmSaveModalRef,
} from "./ConfirmSaveModal/ConfirmSaveModal";
import { TextInputComponent } from "./SettingFormFields/SettingFormFields";
import ReceiverFieldSet from "./ReceiverFieldSet/ReceiverFieldSet";
import { SettingFormContext, SettingFormCtx } from "./SettingFormContext";

export interface SettingFormRowProps {
    label: JSX.Element;
    field: JSX.Element;
}

export function SettingFormRow({ label, field }: SettingFormRowProps) {
    return (
        <Grid row>
            <Grid col={3}>{label}</Grid>
            <Grid col={9}>{field}</Grid>
        </Grid>
    );
}

export interface SettingFormProps extends React.PropsWithChildren {
    entityName: string;
    setting: Partial<RSService>;
    mode: "edit" | "clone";
    onDelete: () => void;
    remove: () => Promise<void>;
    onError: (e: Error) => void;
    refresh: () => Promise<void>;
    save: (setting: RSService) => Promise<void>;
    onSave: (setting: RSService) => void;
    onCancel: () => void;
    onJsonInvalid: (e: Error) => void;
}

export function SettingForm({
    entityName,
    setting,
    onDelete,
    onError,
    refresh,
    remove,
    save,
    onCancel,
    onSave,
    mode,
    children,
    onJsonInvalid,
}: SettingFormProps) {
    const [settingEdit, setSettingEdit] = useState(() => ({
        ...setting,
        name: mode === "edit" ? setting.name : "",
    }));
    const currentJson = JSON.stringify(setting, jsonSortReplacer, 2);
    const settingEditJson = JSON.stringify(settingEdit, jsonSortReplacer, 2);
    const confirmModalRef = useRef<ConfirmSaveModalRef>(null);
    const [loading, setLoading] = useState(false);
    const modalRef = useRef<ModalConfirmRef>(null);

    const ShowDeleteConfirm = () => {
        if (setting.name)
            modalRef?.current?.showModal({
                title: "Confirm Delete",
                message:
                    "Deleting a setting will only mark it deleted. It can be accessed via the revision history",
                okButtonText: "Delete",
                itemId: setting.name,
            });
    };

    const deleteHandler = async (id: string) => {
        try {
            await remove();
            onDelete();
        } catch (e: any) {
            onError(new Error(`Deleting item '${id}' failed. ${e.toString()}`));
        }
    };

    useEffect(() => {
        if (mode === "edit" && setting.version !== settingEdit.version) {
            onError(new Error(getVersionWarning(VersionWarningType.POPUP)));
            confirmModalRef?.current?.setWarning(
                getVersionWarning(VersionWarningType.FULL, setting),
            );
            confirmModalRef?.current?.disableSave();
        }
    }, [mode, onError, setting, settingEdit.version]);

    const showCompareConfirm = async () => {
        try {
            if (mode === "edit") {
                // fetch original version
                setLoading(true);
                await refresh();
                setLoading(false);
            }

            confirmModalRef?.current?.showModal();
        } catch (e: any) {
            setLoading(false);
            const errorDetail = await getErrorDetailFromResponse(e);
            onError(
                new Error(
                    `Reloading ${entityName} '${setting.name}' failed with: ${errorDetail}`,
                ),
            );
        }
    };

    const saveHandler = useCallback(async () => {
        try {
            setLoading(true);

            if (mode === "edit") {
                await refresh();
            }

            const modalJson = confirmModalRef?.current?.getEditedText();

            if (!modalJson) {
                throw new Error("JSON is empty");
            }
            const newReceiver = JSON.parse(modalJson);
            await save(newReceiver);

            setLoading(false);
            confirmModalRef?.current?.hideModal();

            onSave(newReceiver);
        } catch (e: any) {
            setLoading(false);
            let errorDetail = await getErrorDetailFromResponse(e);
            onError(
                new Error(
                    `Updating ${entityName} '${setting.name}' failed with: ${errorDetail}`,
                ),
            );
        }
    }, [entityName, mode, onError, onSave, refresh, save, setting.name]);

    const onCancelHandler = useCallback(() => {
        onCancel();
    }, [onCancel]);

    const ctx = useMemo<SettingFormCtx<RSService>>(
        () => ({
            setting: settingEdit,
            mode,
            defaultValues: setting,
            setSetting: setSettingEdit,
            updateSettingProperty: (
                k: keyof RSService,
                v: RSService[typeof k],
            ) => {
                setSettingEdit((s) => ({ ...s, [k]: v }));
            },
            onJsonInvalid,
        }),
        [mode, onJsonInvalid, setting, settingEdit],
    );

    return (
        <SettingFormContext.Provider value={ctx}>
            <section className="grid-container margin-top-0">
                <GridContainer containerSize={"desktop"}>
                    <TextInputComponent
                        fieldname={"name"}
                        label={"Name"}
                        defaultvalue={settingEdit.name ?? null}
                        savefunc={(v) => ctx.updateSettingProperty("name", v)}
                        disabled={mode === "edit"}
                        required
                    />
                    <TextInputComponent
                        fieldname={"topic"}
                        label={"Topic"}
                        defaultvalue={settingEdit.topic ?? null}
                        savefunc={(v) => ctx.updateSettingProperty("topic", v)}
                        required
                    />
                    {children}
                    <Grid row className="margin-top-2">
                        <Grid col={6}>
                            {mode === "edit" ? (
                                <Button
                                    type={"button"}
                                    secondary={true}
                                    data-testid={"receiverSettingDeleteButton"}
                                    onClick={ShowDeleteConfirm}
                                >
                                    Delete...
                                </Button>
                            ) : null}
                        </Grid>
                        <Grid col={6} className={"text-right"}>
                            <Button
                                type="button"
                                outline={true}
                                onClick={onCancelHandler}
                            >
                                Cancel
                            </Button>
                            <Button
                                form="edit-setting"
                                type="submit"
                                data-testid="submit"
                                disabled={loading}
                                onClick={showCompareConfirm}
                            >
                                Edit json and save...
                            </Button>
                        </Grid>
                    </Grid>
                    <ConfirmSaveModal
                        uniquid={"settingform"}
                        ref={confirmModalRef}
                        onConfirm={saveHandler}
                        oldjson={currentJson}
                        newjson={settingEditJson}
                    />
                </GridContainer>
                <ModalConfirmDialog
                    id={"deleteConfirm"}
                    onConfirm={deleteHandler}
                    ref={modalRef}
                ></ModalConfirmDialog>
            </section>
        </SettingFormContext.Provider>
    );
}

export interface SettingEntityForm
    extends Omit<SettingFormProps, "entityName"> {}

export function ReceiverForm(props: SettingEntityForm) {
    return (
        <SettingForm entityName="Receiver" {...props}>
            <ReceiverFieldSet />
        </SettingForm>
    );
}

export function SenderForm(props: SettingEntityForm) {
    return (
        <SettingForm entityName="Sender" {...props}>
            <ReceiverFieldSet />
        </SettingForm>
    );
}

export function OrganizationForm(props: SettingEntityForm) {
    return (
        <SettingForm entityName="Organization" {...props}>
            <ReceiverFieldSet />
        </SettingForm>
    );
}
