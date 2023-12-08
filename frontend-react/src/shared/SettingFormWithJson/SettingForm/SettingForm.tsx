import { PropsWithChildren, useEffect, useMemo, useState } from "react";
import { Button, Grid, ButtonGroup } from "@trussworks/react-uswds";
import { useForm } from "react-hook-form";

import {
    CustomerStatus,
    Format,
    RSReceiver,
    RSSetting,
} from "../../../config/endpoints/settings";
import OrganizationFieldSet from "../Fieldsets/OrganizationFieldSet/OrganizationFieldSet";
import ReceiverFieldSet from "../Fieldsets/ReceiverFieldSet/ReceiverFieldSet";
import SenderFieldset from "../Fieldsets/SenderFieldSet/SenderFieldSet";
import SettingFieldset from "../Fieldsets/SettingFieldset/SettingFieldset";
import ServiceSettingFieldset from "../Fieldsets/ServiceSettingFieldSet/ServiceSettingFieldset";
import SettingJsonFieldset from "../Fieldsets/SettingJsonFieldset/SettingJsonFieldset";
import {
    ObjectJsonHybrid,
    checkJsonHybrid,
    createJsonHybrid,
} from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";
import { DiffCompare } from "../../DiffCompare/DiffCompare";
import {
    JsonFormValue,
    SettingFormContext,
    SettingFormCtx,
    useSettingForm,
    useSettingFormCreate,
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

export interface SettingFormProps<T> extends SettingFormSharedProps {
    form: SettingFormCtx;
    leftButtons?: JSX.Element;
    rightButtons?: JSX.Element;
}

export function SettingFormDiffCompare() {
    const form = useSettingForm();
    const { _raw, ...obj } = form.formState.defaultValues ?? {};
    const current = form.watch();
    return <DiffCompare a={obj} b={current} />;
}

export function SettingForm<T extends RSSetting = RSSetting>({
    form,
    children,
    ...props
}: SettingFormProps<T>) {
    let leftButtons, rightButtons, isReset, isSubmitDisabled;
    return (
        <SettingFormBase ctx={form}>
            <form
                className={styles.SettingForm}
                onSubmit={form.handleSubmit((v) => {
                    console.log("submit", v);
                })}
            >
                <Grid row>
                    <Grid col={2}>
                        {(form.documentType || form.isCompareAllowed) && (
                            <ButtonGroup type="segmented">
                                <Button
                                    type="button"
                                    base={form.view === "form"}
                                    outline
                                    onClick={() => form.setView("form")}
                                    disabled={!form.formState.isValid}
                                >
                                    Form
                                </Button>
                                <Button
                                    type="button"
                                    outline
                                    base={form.view === "document"}
                                    onClick={() => form.view("document")}
                                    disabled={!form.formState.isValid}
                                >
                                    {form.documentType}
                                </Button>
                                <Button
                                    type="button"
                                    base={form.view === "compare"}
                                    outline
                                    onClick={() => form.setView("compare")}
                                    disabled={!form.formState.isValid}
                                >
                                    Compare
                                </Button>
                            </ButtonGroup>
                        )}
                    </Grid>
                </Grid>
                <section hidden={form.view !== "form"}>
                    <SettingFieldset />
                    {children}
                </section>
                <section hidden={form.view !== "document"}>
                    <SettingJsonFieldset />
                </section>
            </form>
            {form.view === "compare" && <SettingFormDiffCompare />}
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
        </SettingFormBase>
    );
}

export function ReceiverFormInner({
    children,
    ...props
}: SettingFormProps<RSReceiver>) {
    return (
        <>
            <SettingFieldset />
            <ServiceSettingFieldset />
            <ReceiverFieldSet />
            {children}
        </>
    );
}

export function SenderFormInner({ children, ...props }: SettingFormProps) {
    return (
        <>
            <SettingFieldset />
            <ServiceSettingFieldset />
            <SenderFieldset />
            {children}
        </>
    );
}

export function OrganizationFormInner({
    children,
    ...props
}: SettingFormProps) {
    return (
        <>
            <SettingFieldset />
            <OrganizationFieldSet />
            {children}
        </>
    );
}
