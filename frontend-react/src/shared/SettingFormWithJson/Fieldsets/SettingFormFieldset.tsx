import { Fieldset, Label } from "@trussworks/react-uswds";
import { ComponentProps, ForwardRefExoticComponent, useMemo } from "react";
import { SetOptional } from "type-fest";
import startCase from "lodash.startcase";
import { RegisterOptions } from "react-hook-form";

import { reportStreamFilterDefinitionChoices } from "../../../config/endpoints/settings";
import { FormDocumentType } from "../SettingFormContext/SettingFormContext";
import { Checkbox, Select, TextInput, Textarea } from "../../FormInput";
import {
    FormField,
    SettingFormFieldRow,
} from "../SettingFormField/SettingFormField";
import ObjectExampleTooltip from "../Tooltips/ObjectExampleTooltip";

export const FormFieldTypeMap = {
    text: TextInput,
    textarea: Textarea,
    select: Select,
    checkbox: Checkbox,
} satisfies Record<string, ForwardRefExoticComponent<any>>;

export type FormFieldType = keyof typeof FormFieldTypeMap;

export interface FormFieldInputBase {
    type: FormFieldType;
}

export interface FormFieldSelectInput extends FormFieldInputBase {
    type: "select";
    choices: (string | [string, string])[];
}

export interface FormFieldInputDefault extends FormFieldInputBase {
    type: "text" | "textarea" | "checkbox";
    choices?: (string | [string, string])[];
}

export type FormFieldInput = FormFieldInputDefault | FormFieldSelectInput;

export interface FormFieldDefault<M extends Record<string, any> = any> {
    name: string & keyof M;
    input: FormFieldInput;
    order: number;
    label?: string;
    opts?: RegisterOptions<M> & {
        isJson?: boolean;
        documentType?: FormDocumentType;
    };
    objTooltip?: {
        obj: object;
        description?: string;
    };
}

export type FormFieldShorthand<M extends Record<string, any> = any> =
    SetOptional<FormFieldDefault<M>, "order">;

export type FormFieldItem<M extends Record<string, any> = any> =
    | FormFieldDefault<M>
    | FormFieldShorthand<M>;

export class FormFieldMap<
    M extends Record<string, any> = Record<string, any>,
> extends Map<string, FormFieldDefault<M>> {
    constructor(
        fields?: [string, FormFieldDefault<M>][] | FormFieldShorthand<M>[],
    ) {
        let init;
        if (fields) {
            if ("name" in fields[0]) {
                init = fields.map<[string, FormFieldDefault<M>]>((f, i) => {
                    if (!("name" in f))
                        throw new TypeError("Invalid item in form field array");
                    const { order, label, ...field } = f;
                    return [
                        field.name,
                        {
                            ...field,
                            order: order ?? i,
                            label: label ?? startCase(field.name),
                        },
                    ];
                });
            } else {
                init = fields.map((m) => {
                    if ("name" in m)
                        throw new TypeError("Invalid item in form field array");
                    return m;
                });
            }
        }
        super(init);
    }

    add(...fields: FormFieldShorthand[]) {
        for (const { order, label, ...field } of fields) {
            if (this.has(field.name))
                throw new Error(`Field ${field.name} already exists`);

            this.set(field.name, {
                ...field,
                order: order ?? this.size,
                label: label ?? startCase(field.name),
            });
        }
    }

    list() {
        return Array.from(this.values());
    }
}

export interface FormFieldsetProps<M extends Record<string, any> = any>
    extends SetOptional<ComponentProps<typeof Fieldset>, "children"> {
    fields: FormFieldDefault<M>[];
}

export function FormFieldset({
    fields,
    children,
    ...props
}: FormFieldsetProps) {
    const sorted = useMemo(
        () => fields.toSorted((a, b) => a.order - b.order),
        [fields],
    );
    return (
        <Fieldset {...props}>
            {sorted.map((f) => {
                return (
                    <SettingFormFieldRow
                        key={f.name}
                        label={
                            f.label ?? f.objTooltip ? (
                                <>
                                    {f.label && (
                                        <Label
                                            htmlFor={f.name}
                                            requiredMarker={!!f.opts?.required}
                                        >
                                            {f.label}
                                        </Label>
                                    )}
                                    {f.objTooltip && (
                                        <ObjectExampleTooltip
                                            {...f.objTooltip}
                                        />
                                    )}
                                </>
                            ) : undefined
                        }
                    >
                        <FormField {...f} />
                    </SettingFormFieldRow>
                );
            })}
            {children}
        </Fieldset>
    );
}

export const filterHint = `Available Filters: ${reportStreamFilterDefinitionChoices.join(
    ", ",
)}`;
