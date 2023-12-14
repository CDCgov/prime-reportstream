import { Grid } from "@trussworks/react-uswds";
import {
    ComponentProps,
    ComponentType,
    ForwardedRef,
    HTMLAttributes,
    PropsWithChildren,
    forwardRef,
} from "react";

import "./SettingFormField.scss";
import {
    FormFieldDefault,
    FormFieldType,
    FormFieldTypeMap,
} from "../Fieldsets/SettingFormFieldset";
import { useController } from "../SettingFormContext/SettingFormContext";
import { isObjStringEmpty } from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";

export interface SettingFormFieldRowProps extends PropsWithChildren {
    label?: JSX.Element;
}

export type FormFieldProps<T extends FormFieldType> = PropsWithChildren &
    Pick<FormFieldDefault, "name" | "input" | "opts"> & {
        input: { type: T };
    } & HTMLAttributes<FormFieldTypeElement<T>>;

export type FormFieldBaseProps<T extends FormFieldType> = PropsWithChildren & {
    isInvalid?: boolean;
    type: T;
    name: string;
    choices: T extends "select" ? (string | [string, string])[] : undefined;
} & HTMLAttributes<FormFieldTypeElement<T>>;

type FormFieldTypeElement<T extends FormFieldType> = T extends "textarea"
    ? HTMLTextAreaElement
    : T extends "select"
    ? HTMLSelectElement
    : T extends "text" | "checkbox"
    ? HTMLInputElement
    : T;

export const FormFieldBase = forwardRef(function FormFieldBase<
    const T extends FormFieldType,
>(
    { isInvalid, type, choices, ...props }: FormFieldBaseProps<T>,
    ref: ForwardedRef<FormFieldTypeElement<T>>,
) {
    let Component: ComponentType<any>, fieldProps;

    switch (type) {
        case "textarea":
            Component = FormFieldTypeMap[type];
            fieldProps = {
                error: isInvalid,
            } as ComponentProps<(typeof FormFieldTypeMap)[typeof type]>;
            break;
        case "text":
            Component = FormFieldTypeMap[type];
            fieldProps = {
                type,
                validationStatus: isInvalid ? "error" : undefined,
            } as ComponentProps<(typeof FormFieldTypeMap)[typeof type]>;
            break;
        default:
            Component = FormFieldTypeMap[type];
            break;
    }

    return (
        <Component {...fieldProps} {...props} ref={ref}>
            {type === "select"
                ? choices?.map((c) => {
                      const [value, label] =
                          typeof c === "string" ? [c, undefined] : c;
                      return (
                          <option key={value} value={value}>
                              {label ?? value}
                          </option>
                      );
                  })
                : undefined}
        </Component>
    );
});

export const FormField = forwardRef(function FormField<
    const T extends FormFieldType,
>(
    {
        name,
        input: { type, choices },
        opts: { isJson, documentType, ...opts } = {},
        ...props
    }: FormFieldProps<T>,
    ref: ForwardedRef<FormFieldTypeElement<T>>,
) {
    const { field, fieldState } = useController({
        name,
        isJson,
        documentType,
        rules: {
            validate: {
                json: (v) => {
                    if (!isJson || !v) return;

                    if (isJson && typeof v !== "string")
                        return `Expected json string: ${JSON.stringify(v)}`;

                    try {
                        JSON.parse(v);
                        return true;
                    } catch (e: any) {
                        return e.message;
                    }
                },
                choices: (v) => {
                    if (choices) {
                        if (
                            choices.find(
                                (c) =>
                                    (typeof c === "string" && c === v) ||
                                    (Array.isArray(c) && c[0] === v),
                            )
                        )
                            return true;
                        return "Invalid choice";
                    }
                },
            },
            ...opts,
        },
    });

    const formProps = {
        ...field,
        onBlur: (e: any) => {
            if (isJson && isObjStringEmpty(e.currentTarget.value)) {
                // keep empty objects out on blur.
                // we create a fake event with the cleared string
                // and pass to onChange
                const changeE = {
                    ...e,
                    currentTarget: { ...e.currentTarget, value: "" },
                    target: { ...e.target, value: "" },
                };
                field.onChange(changeE);
            }
            field.onBlur();
        },
        type,
        choices,
        isInvalid: fieldState.invalid,
    };

    return <FormFieldBase {...formProps} name={name} {...props} ref={ref} />;
});

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
