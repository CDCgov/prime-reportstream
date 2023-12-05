import { PropsWithChildren, useMemo, useState } from "react";
import {
    GridContainer,
    Button,
    Grid,
    ButtonGroup,
} from "@trussworks/react-uswds";

import {
    RSOrganizationSettings,
    RSReceiver,
    RSSender,
} from "../../../config/endpoints/settings";
import {
    SettingFormContext,
    SettingFormCtx,
} from "../SettingFormContext/SettingFormContext";
import OrganizationFieldSet from "../Fieldsets/OrganizationFieldSet/OrganizationFieldSet";
import ReceiverFieldSet from "../Fieldsets/ReceiverFieldSet/ReceiverFieldSet";
import SenderFieldset from "../Fieldsets/SenderFieldSet/SenderFieldSet";
import SettingFieldset from "../Fieldsets/SettingFieldset/SettingFieldset";
import ServiceSettingFieldset from "../Fieldsets/ServiceSettingFieldSet/ServiceSettingFieldset";
import SettingJsonFieldset from "../Fieldsets/SettingJsonFieldset/SettingJsonFieldset";
import useObjectJsonHybridEdit from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";
import { DiffCompare } from "../../DiffCompare/DiffCompare";

export type SettingFormMode = "edit" | "new" | "clone";
export type SettingFormView = "form" | "document" | "compare";

export interface SettingFormSharedProps extends PropsWithChildren {
    onDelete?: () => void;
    onCancel?: () => void;
    isSave?: boolean;
    mode: SettingFormMode;
}

export interface SettingFormBaseProps<T extends object = any>
    extends SettingFormSharedProps {
    ctx: SettingFormCtx;
    onSave?: () => void;
    onChangeView: (v: SettingFormView) => void;
    formView: SettingFormView;
    isInvalid?: boolean;
}

export function SettingFormBase({
    ctx,
    children,
    onDelete,
    onCancel,
    onSave,
    isSave,
    onChangeView,
    formView,
    isInvalid,
}: SettingFormBaseProps) {
    return (
        <GridContainer containerSize={"desktop"}>
            <SettingFormContext.Provider value={ctx}>
                {children}
                <Grid row className="margin-top-2">
                    <Grid col={6}>
                        {onDelete && (
                            <Button
                                type={"button"}
                                secondary={true}
                                onClick={onDelete}
                            >
                                Delete...
                            </Button>
                        )}
                    </Grid>
                    <Grid col={6} className={"text-right"}>
                        <ButtonGroup>
                            <Button
                                type="button"
                                outline={true}
                                onClick={onCancel}
                            >
                                Cancel
                            </Button>
                            {onChangeView && (
                                <ButtonGroup type="segmented">
                                    <Button
                                        type="button"
                                        base={formView === "form"}
                                        outline
                                        onClick={() => onChangeView("form")}
                                        disabled={isInvalid}
                                    >
                                        Form
                                    </Button>
                                    <Button
                                        type="button"
                                        outline
                                        base={formView === "document"}
                                        onClick={() => onChangeView("document")}
                                        disabled={isInvalid}
                                    >
                                        JSON
                                    </Button>
                                    <Button
                                        type="button"
                                        base={formView === "compare"}
                                        outline
                                        onClick={() => onChangeView("compare")}
                                        disabled={isInvalid}
                                    >
                                        Compare
                                    </Button>
                                </ButtonGroup>
                            )}
                            {onSave && (
                                <Button
                                    form="edit-setting"
                                    type="submit"
                                    disabled={!isSave}
                                    onClick={onSave}
                                >
                                    Save
                                </Button>
                            )}
                        </ButtonGroup>
                    </Grid>
                </Grid>
            </SettingFormContext.Provider>
        </GridContainer>
    );
}

export interface SettingFormProps<T extends object = any>
    extends SettingFormSharedProps {
    isReadonly?: boolean;
    onSave?: (v: T) => void;
    initialValues: T;
}

export function SettingForm<T extends object>({
    initialValues,
    isReadonly,
    children,
    onSave,
    isSave: _isSave,
    mode,
    ...props
}: SettingFormProps<T>) {
    const [formView, setFormView] = useState<SettingFormView>("form");
    const [jsonKey, setJsonKey] = useState<string>();
    const [jsonFieldKeys, setJsonFieldKeys] = useState<string[]>([]);
    const { hybrid, hybridResult, json, jsonResult, setHybrid, setJson } =
        useObjectJsonHybridEdit(initialValues, jsonFieldKeys);
    const ctx = useMemo<SettingFormCtx>(() => {
        return {
            mode,
            isReadonly,
            defaultValues: hybrid,
            json,
            onFieldChange: (k, v: any) => {
                if (jsonKey === k) {
                    setJson(v);
                    return;
                }
                setHybrid((s) => ((s as any)[k] !== v ? { ...s, [k]: v } : s));
            },
            getId: (v) => `settingform-${v}`,
            registerField: (v, jsonType) => {
                if (jsonType) {
                    switch (jsonType) {
                        case "field":
                            setJsonFieldKeys((a) => {
                                return !a.includes(v) ? [...a, v] : a;
                            });
                            break;
                        case "whole":
                            setJsonKey(v);
                            break;
                    }
                }
            },
        };
    }, [hybrid, isReadonly, json, jsonKey, mode, setHybrid, setJson]);
    let isViewValid = true;
    let render;

    switch (formView) {
        case "form":
            render = (
                <>
                    <SettingFieldset />
                    {children}
                </>
            );
            isViewValid = hybridResult.errors.length === 0;
            break;
        case "document":
            render = <SettingJsonFieldset />;
            isViewValid = !jsonResult.error;
            break;
        case "compare":
            render = <DiffCompare a={initialValues} b={jsonResult.obj ?? {}} />;
            break;
    }
    const isSave = _isSave && isViewValid;

    return (
        <SettingFormBase
            mode={"edit"}
            ctx={ctx}
            onSave={
                onSave
                    ? () => jsonResult.obj && onSave(jsonResult.obj)
                    : undefined
            }
            onChangeView={setFormView}
            isSave={isSave}
            formView={formView}
            {...props}
        >
            {render}
        </SettingFormBase>
    );
}

export function ReceiverForm({
    children,
    ...props
}: SettingFormProps<RSReceiver>) {
    return (
        <SettingForm {...props}>
            <ServiceSettingFieldset />
            <ReceiverFieldSet />
            {children}
        </SettingForm>
    );
}

export function SenderForm({ children, ...props }: SettingFormProps<RSSender>) {
    return (
        <SettingForm {...props}>
            <ServiceSettingFieldset />
            <SenderFieldset />
            {children}
        </SettingForm>
    );
}

export function OrganizationForm({
    children,
    ...props
}: SettingFormProps<RSOrganizationSettings>) {
    return (
        <SettingForm {...props}>
            <OrganizationFieldSet />
            {children}
        </SettingForm>
    );
}
