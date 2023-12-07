import { PropsWithChildren, useEffect, useMemo, useState } from "react";
import { Button, Grid, ButtonGroup } from "@trussworks/react-uswds";
import { useForm } from "@tanstack/react-form";

import { RSSetting } from "../../../config/endpoints/settings";
import OrganizationFieldSet from "../Fieldsets/OrganizationFieldSet/OrganizationFieldSet";
import ReceiverFieldSet from "../Fieldsets/ReceiverFieldSet/ReceiverFieldSet";
import SenderFieldset from "../Fieldsets/SenderFieldSet/SenderFieldSet";
import SettingFieldset from "../Fieldsets/SettingFieldset/SettingFieldset";
import ServiceSettingFieldset from "../Fieldsets/ServiceSettingFieldSet/ServiceSettingFieldset";
import SettingJsonFieldset from "../Fieldsets/SettingJsonFieldset/SettingJsonFieldset";
import {
    checkJsonHybrid,
    createJsonHybrid,
} from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";
import { DiffCompare } from "../../DiffCompare/DiffCompare";
import {
    SettingFormContext,
    SettingFormCtx,
} from "../SettingFormContext/SettingFormContext";

import styles from "./SettingForm.module.scss";

export type SettingFormMode = "edit" | "new" | "clone" | "readonly";
export type SettingFormView = "form" | "document" | "compare";

export interface SettingFormSharedProps extends PropsWithChildren {
    isSubmitDisabled?: boolean;
}

export interface SettingFormBaseProps<T extends RSSetting = RSSetting>
    extends SettingFormSharedProps {
    ctx: SettingFormCtx<T>;
}

export function SettingFormBase<T extends RSSetting>({
    ctx,
    children,
}: SettingFormBaseProps<T>) {
    return (
        <SettingFormContext.Provider value={ctx}>
            {children}
        </SettingFormContext.Provider>
    );
}

export interface SettingFormProps<T extends RSSetting = RSSetting>
    extends SettingFormSharedProps {
    isReadonly?: boolean;
    initialValues?: T;
    documentType?: "JSON"; // | "yaml"
    isCompare?: boolean;
    onSubmit: (v: T) => void;
    mode: SettingFormMode;
    isReset?: boolean;
    leftButtons?: JSX.Element;
    rightButtons?: JSX.Element;
}

function stringify(obj: object, type: "json" /*| "yaml"*/ = "json") {
    switch (type) {
        case "json":
            return JSON.stringify(obj, undefined, 2);
    }
}

export function SettingForm<T extends RSSetting = RSSetting>({
    initialValues = {} as any,
    isReadonly,
    documentType,
    isCompare,
    isSubmitDisabled: _isSubmitDisabled,
    children,
    mode,
    isReset,
    onSubmit,
    leftButtons,
    rightButtons,
    ...props
}: SettingFormProps<T>) {
    const [jsonKeys, setJsonKeys] = useState<(string & keyof T)[]>([]);
    const defaultValues = useMemo(
        () => ({
            ...createJsonHybrid<RSSetting>(initialValues, jsonKeys as any),
            _raw: stringify(initialValues),
        }),
        [initialValues, jsonKeys],
    );

    const form = useForm({
        defaultValues: defaultValues,
        onSubmit: (v) => {
            const {
                obj: { _raw, ...finalObj },
                errors,
            } = checkJsonHybrid(v, []);
            if (errors.length === 0) onSubmit(finalObj as any);
        },
    });
    const isFormValid = form.useStore((s) => s.isFormValid);
    const ctx = useMemo<SettingFormCtx<T>>(
        () => ({
            form,
            mode,
            getId: (v: string) => `settingform-${v}`,
            registerJsonFields: (...fields) => {
                setJsonKeys((k) => [...k, ...fields]);
            },
        }),
        [form, mode],
    );
    const isSubmitDisabled = _isSubmitDisabled && !isFormValid;
    const [formView, setFormView] = useState<"form" | "document" | "compare">(
        "form",
    );
    const test = form.useStore(({ fieldMeta, values }) => ({
        fieldMeta,
        values,
    }));

    // [ document -> form / form -> document ] logic
    useEffect(() => {
        // ALWAYS reset `isTouched` on fields being updated or face pain by recursion
        const {
            fieldMeta,
            values: { _raw, ...obj },
        } = test;
        let isRawStale = false;
        for (const [k, v] of Object.entries(fieldMeta)) {
            // Iterate through valid touched fields
            if (
                v.isTouched &&
                !v.isValidating &&
                v.touchedErrors.length === 0
            ) {
                // document field updated, immediately update form from parse
                if (k === "_raw") {
                    const rawObj = createJsonHybrid(JSON.parse(_raw), jsonKeys);
                    form.store.setState((s) => ({
                        ...s,
                        values: { ...rawObj, _raw },
                        fieldMeta: {
                            ...s.fieldMeta,
                            _raw: {
                                ...s.fieldMeta._raw,
                                isTouched: false,
                            },
                        },
                    }));
                    return;
                }

                // other form field updated
                isRawStale = true;
            }
        }
        // document field is stale, create original shape from current state and store string form
        if (isRawStale) {
            const { obj: fullObj } = checkJsonHybrid(obj, jsonKeys);
            if (fullObj)
                form.store.setState((s) => ({
                    ...s,
                    values: {
                        ...s.values,
                        _raw: stringify(fullObj),
                    },
                    fieldMeta: Object.fromEntries(
                        Object.entries(s.fieldMeta).map(([k, v]) =>
                            k === "_raw"
                                ? [k, v]
                                : [k, { ...v, isTouched: false }],
                        ),
                    ) as typeof s.fieldMeta,
                }));
        }
    }, [form, jsonKeys, test]);

    return (
        <SettingFormBase ctx={ctx}>
            <form.Provider>
                <form
                    className={styles.SettingForm}
                    onSubmit={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        form.handleSubmit();
                    }}
                >
                    <Grid row>
                        <Grid col={2}>
                            {(documentType || isCompare) && (
                                <ButtonGroup type="segmented">
                                    <Button
                                        type="button"
                                        base={formView === "form"}
                                        outline
                                        onClick={() => setFormView("form")}
                                        disabled={!isFormValid}
                                    >
                                        Form
                                    </Button>
                                    <Button
                                        type="button"
                                        outline
                                        base={formView === "document"}
                                        onClick={() => setFormView("document")}
                                        disabled={!isFormValid}
                                    >
                                        {documentType}
                                    </Button>
                                    <Button
                                        type="button"
                                        base={formView === "compare"}
                                        outline
                                        onClick={() => setFormView("compare")}
                                        disabled={!isFormValid}
                                    >
                                        Compare
                                    </Button>
                                </ButtonGroup>
                            )}
                        </Grid>
                    </Grid>
                    <section hidden={formView !== "form"}>
                        <SettingFieldset />
                        {children}
                    </section>
                    <section hidden={formView !== "document"}>
                        <SettingJsonFieldset />
                    </section>
                </form>
                {formView === "compare" && (
                    <DiffCompare a={initialValues} b={{}} />
                )}
                <Grid row>
                    <Grid col={6}>{leftButtons}</Grid>
                    <Grid col={6} className="text-right">
                        <ButtonGroup>
                            {rightButtons}
                            {isReset && (
                                <Button type="reset" outline>
                                    Reset
                                </Button>
                            )}
                            <Button type="submit" disabled={isSubmitDisabled}>
                                Submit
                            </Button>
                        </ButtonGroup>
                    </Grid>
                </Grid>
            </form.Provider>
        </SettingFormBase>
    );
}

export function ReceiverForm({ children, ...props }: SettingFormProps) {
    return (
        <SettingForm {...props}>
            <ServiceSettingFieldset />
            <ReceiverFieldSet />
            {children}
        </SettingForm>
    );
}

export function SenderForm({ children, ...props }: SettingFormProps) {
    return (
        <SettingForm {...props}>
            <ServiceSettingFieldset />
            <SenderFieldset />
            {children}
        </SettingForm>
    );
}

export function OrganizationForm({ children, ...props }: SettingFormProps) {
    return (
        <SettingForm {...props}>
            <OrganizationFieldSet />
            {children}
        </SettingForm>
    );
}
