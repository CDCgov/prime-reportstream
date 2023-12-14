import { ButtonGroup, Button, Grid } from "@trussworks/react-uswds";
import { ComponentType, useMemo } from "react";

import {
    RsOrganizationEdit,
    RsReceiverEdit,
    RsSenderEdit,
    IRsService,
    IRsSetting,
    SampleScopedJwks,
    SampleTiming,
    SampleTranslation,
    SampleTransports,
    customerStatusChoices,
    formatChoices,
    jurisdictionChoices,
    processingTypeChoices,
} from "../../../config/endpoints/settings";
import MetaDisplay from "../../MetaDisplay/MetaDisplay";
import {
    FormFieldDefault,
    FormFieldMap,
    FormFieldShorthand,
    FormFieldset,
} from "../Fieldsets/SettingFormFieldset";
import { FormField } from "../SettingFormField/SettingFormField";
import {
    useForm,
    useFormState,
} from "../SettingFormContext/SettingFormContext";
import Alert from "../../Alert/Alert";

import styles from "./SettingForm.module.scss";

export interface ObjectJsonHybridFormFieldDefault<
    M extends Record<string, any> = any,
> extends FormFieldDefault<M> {
    isJson?: boolean;
}

export interface ObjectJsonHybridFormShorthand<
    M extends Record<string, any> = any,
> extends FormFieldShorthand<M> {}

export type ObjectJsonHybridFormField<M extends Record<string, any> = any> =
    | ObjectJsonHybridFormFieldDefault<M>
    | ObjectJsonHybridFormShorthand<M>;

const settingFieldset = [
    { name: "name", input: { type: "text" } },
    { name: "description", input: { type: "text" } },
] satisfies ObjectJsonHybridFormField<IRsSetting>[];
const serviceFieldset = [
    { name: "topic", input: { type: "text" } },
    {
        name: "customerStatus",
        input: { type: "select", choices: customerStatusChoices },
    },
] satisfies ObjectJsonHybridFormField<IRsService>[];
const organizationFieldset = [
    {
        name: "jurisdiction",
        input: { type: "select", choices: jurisdictionChoices },
    },
    { name: "countyName", input: { type: "text" } },
    { name: "stateCode", input: { type: "text" } },
    {
        name: "filters",
        input: { type: "textarea" },
        opts: { isJson: true },
    },
] satisfies ObjectJsonHybridFormField<RsOrganizationEdit>[];
const receiverFieldset = [
    {
        name: "translation",
        input: { type: "textarea" },
        objTooltip: { obj: SampleTranslation },
        opts: { isJson: true },
    },
    {
        name: "jurisdictionalFilter",
        input: { type: "textarea" },
        opts: { isJson: true },
    },
    {
        name: "qualityFilter",
        input: { type: "textarea" },
        opts: { isJson: true },
    },
    { name: "reverseTheQualityFilter", input: { type: "checkbox" } },
    { name: "routingFilter", input: { type: "text" } },
    { name: "processingModeFilter", input: { type: "text" } },
    { name: "deidentify", input: { type: "checkbox" } },
    {
        name: "timing",
        input: { type: "textarea" },
        objTooltip: { obj: SampleTiming },
        opts: { isJson: true },
    },
    {
        name: "transport",
        input: { type: "textarea" },
        objTooltip: {
            obj: SampleTransports,
            description: "Field accepts ONE of allowed types from sample.",
        },
        opts: { isJson: true },
    },
    { name: "externalName", input: { type: "text" } },
] satisfies ObjectJsonHybridFormField<RsReceiverEdit>[];
const senderFieldset = [
    {
        name: "format",
        input: { type: "select", choices: formatChoices },
        objTooltip: { obj: SampleTranslation },
    },
    { name: "schemaName", input: { type: "text" } },
    {
        name: "keys",
        input: { type: "textarea" },
        objTooltip: { obj: SampleScopedJwks },
        opts: { isJson: true },
    },
    {
        name: "processingType",
        input: { type: "select", choices: processingTypeChoices },
    },
    { name: "allowDuplicates", input: { type: "checkbox" } },
] satisfies ObjectJsonHybridFormField<RsSenderEdit>[];

export const organizationFields = new FormFieldMap<RsOrganizationEdit>([
    ...settingFieldset,
    ...organizationFieldset,
]);

export function OrganizationFieldset() {
    return <FormFieldset fields={organizationFields.list()} />;
}
export const receiverFields = new FormFieldMap<RsReceiverEdit>([
    ...settingFieldset,
    ...serviceFieldset,
    ...receiverFieldset,
]);

export function ReceiverFieldset() {
    return <FormFieldset fields={receiverFields.list()} />;
}

export const senderFields = new FormFieldMap<RsSenderEdit>([
    ...settingFieldset,
    ...serviceFieldset,
    ...senderFieldset,
]);

export function SenderFieldset() {
    return <FormFieldset fields={senderFields.list()} />;
}

export const SettingTypes = ["organization", "sender", "receiver"];
export const SettingTypeFieldsetMap = {
    organization: OrganizationFieldset,
    sender: SenderFieldset,
    receiver: ReceiverFieldset,
} satisfies Record<(typeof SettingTypes)[number], ComponentType<any>>;

export function SettingFormViewButtonGroupBase({
    view,
    documentType,
    isInvalid,
    onViewChange,
}: any) {
    return (
        <ButtonGroup type="segmented">
            <Button
                type="button"
                base={view === "form"}
                outline
                onClick={() => onViewChange("form")}
                disabled={isInvalid}
            >
                Form
            </Button>
            {documentType && (
                <Button
                    type="button"
                    outline
                    base={view === "document"}
                    onClick={() => onViewChange("document")}
                    disabled={isInvalid}
                >
                    {documentType}
                </Button>
            )}
            <Button
                type="button"
                base={view === "compare"}
                outline
                onClick={() => onViewChange("compare")}
                disabled={isInvalid}
            >
                Compare
            </Button>
        </ButtonGroup>
    );
}

export function SettingFormViewButtonGroup(props: any) {
    const { isValid } = useFormState();
    return <SettingFormViewButtonGroupBase {...props} isInvalid={!isValid} />;
}

export function SettingFormErrorAlertBase({ errors }: any) {
    return (
        <Alert type="error">
            <ul>
                {Object.entries(errors).map(([k, v]) => (
                    <li key={k}>
                        {k} ({v.type}): {v.message}
                    </li>
                ))}
            </ul>
        </Alert>
    );
}

export function SettingFormErrorAlert() {
    const { errors, isValid } = useFormState();
    return (
        !isValid &&
        Object.keys(errors).length > 0 && (
            <SettingFormErrorAlertBase errors={errors} />
        )
    );
}

export function SettingFormBase({
    setting,
    mode,
    settingType,
    onSubmit,
    onDelete,
    onCancel,
    onReset,
    onViewChange,
    isSubmitDisabled,
    isResetAllowed,
    view,
    documentType,
    isCompareAllowed,
}: any) {
    const FormFields = (SettingTypeFieldsetMap as any)[settingType];
    const { createdAt, createdBy, version } = setting;
    const settingMeta = { createdAt, createdBy, version };
    return (
        <>
            <SettingFormErrorAlert />
            <Grid row>
                <Grid col={2}>
                    {(documentType || isCompareAllowed) && (
                        <SettingFormViewButtonGroup
                            documentType={documentType}
                            view={view}
                            onViewChange={onViewChange}
                        />
                    )}
                </Grid>
            </Grid>
            <section hidden={view !== "form"}>
                <FormFields />
            </section>
            <section hidden={view !== "document"}>
                <FormField
                    name="_json"
                    input={{ type: "textarea" }}
                    opts={{
                        deps: Object.keys(setting),
                        documentType: "json",
                        validate: (v) => {
                            try {
                                JSON.parse(v);
                                return true;
                            } catch (e: any) {
                                return e.message;
                            }
                        },
                    }}
                />
            </section>

            {mode === "readonly" && (
                <Grid row>
                    <Grid col={3}>Meta:</Grid>
                    <Grid col={9}>
                        <MetaDisplay {...settingMeta} />
                        <br />
                    </Grid>
                </Grid>
            )}
            <Grid row>
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
                <Grid col={6} className="text-right">
                    <ButtonGroup>
                        <Button type={"button"} outline onClick={onCancel}>
                            Cancel
                        </Button>
                        {isResetAllowed && (
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
        </>
    );
}

export function SettingForm({
    mode,
    setting,
    isCompareAllowed,
    documentType,
    onSubmit,
    onCancel,
    onDelete,
    settingType,
}: any) {
    // exclude metadata from form object
    const settingWrite = useMemo(
        () =>
            Object.fromEntries(
                Object.entries(setting).filter(
                    ([k]) => !["createdAt", "createdBy", "version"].includes(k),
                ),
            ),
        [setting],
    );
    const form = useForm({
        initialValues: settingWrite,
        isCompareAllowed,
        mode,
    });
    return (
        <form.Form
            className={styles.SettingForm}
            onSubmit={(v) => {
                onSubmit(v);
            }}
        >
            <SettingFormBase
                setting={setting}
                isCompareAllowed={isCompareAllowed}
                isResetAllowed={false}
                mode={mode}
                documentType={documentType}
                onCancel={onCancel}
                onDelete={onDelete}
                settingType={settingType}
                onViewChange={form.setView}
                view={form.view}
            />
        </form.Form>
    );
}
