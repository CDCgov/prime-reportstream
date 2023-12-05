import { Grid } from "@trussworks/react-uswds";
import { ChangeEvent, PropsWithChildren, useCallback, useEffect } from "react";

import {
    SettingFormMode,
    useSettingForm,
} from "../SettingFormContext/SettingFormContext";

import "./SettingFormField.scss";

export interface SettingFormFieldRowProps extends PropsWithChildren {
    label?: JSX.Element;
}

export function SettingFormFieldRow({
    label,
    children,
}: SettingFormFieldRowProps) {
    return (
        <Grid row>
            {label && <Grid col={3}>{label}</Grid>}
            <Grid col={label ? 9 : 12}>{children}</Grid>
        </Grid>
    );
}

export interface SettingFormFieldChildrenProps {
    mode: SettingFormMode;
    name: string;
    defaultValue: string;
    id: string;
    onChange: (v: any) => void;
    disabled: boolean;
    className?: string;
}

export interface SettingFormFieldProps {
    onChange: (e: ChangeEvent<any>) => unknown;
    name: string;
    render: (props: SettingFormFieldChildrenProps) => JSX.Element;
    jsonType?: "whole" | "field";
}
export function SettingFormField({
    name,
    render,
    onChange: _onChange,
    jsonType,
}: SettingFormFieldProps) {
    const {
        onFieldChange,
        registerField,
        defaultValues,
        mode,
        json,
        isReadonly = false,
        getId,
    } = useSettingForm();
    const id = getId(name);
    const onChange = useCallback(
        (e: ChangeEvent<any>) => {
            onFieldChange(name, _onChange(e));
        },
        [_onChange, name, onFieldChange],
    );
    useEffect(() => {
        if (jsonType) registerField(name, jsonType);
    }, [jsonType, name, registerField]);

    return (
        <>
            {render({
                defaultValue: jsonType === "whole" ? json : defaultValues[name],
                id,
                name,
                onChange,
                mode,
                disabled: isReadonly,
                className: "rs-input",
            })}
        </>
    );
}
