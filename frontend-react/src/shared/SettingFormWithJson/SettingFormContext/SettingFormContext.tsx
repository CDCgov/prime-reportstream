import {
    ComponentProps,
    createContext,
    useCallback,
    useContext,
    useMemo,
    useState,
} from "react";
import { useForm } from "react-hook-form";
import { Form } from "@trussworks/react-uswds";
import { DevTool } from "@hookform/devtools";

import type {
    SettingFormMode,
    SettingFormView,
} from "../SettingForm/SettingForm";
import { createJsonHybrid } from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";

function stringify(obj: object, type: "json" /*| "yaml"*/ = "json") {
    switch (type) {
        case "json":
            return JSON.stringify(obj, undefined, 2);
    }
}

export type JsonFormValue<T> = T & {
    _raw: string;
};

export type SettingFormCtx = Omit<
    ReturnType<typeof useSettingFormCreate>,
    "Provider"
>;

export const SettingFormContext = createContext<SettingFormCtx>({});

export interface UseSettingFormProps<T extends Record<string, any>> {
    initialValues?: T;
    mode?: SettingFormMode;
    view?: SettingFormView;

    documentType?: "JSON"; // | "yaml"
    isResetAllowed?: boolean;
    isCompareAllowed?: boolean;
}

export function useSettingFormCreate<T extends Record<string, any>>({
    initialValues = {} as any,
    mode: _mode = "new",
    view: _view = "form",
    documentType: _documentType,
    isResetAllowed: _isResetAllowed = false,
    isCompareAllowed: _isCompareAllowed = true,
}: UseSettingFormProps<T>) {
    const [isCompareAllowed, setIsCompareAllowed] = useState(_isCompareAllowed);
    const [isResetAllowed, setIsResetAllowed] = useState(_isResetAllowed);
    const [mode, setMode] = useState(_mode);
    const [view, setView] = useState(_view);
    const [documentType, setDocumentType] = useState(_documentType);
    const [jsonKeys, setJsonKeys] = useState<(string & keyof T)[]>([]);
    const defaultValues = useMemo(
        () => ({
            ...createJsonHybrid(initialValues, jsonKeys),
            _raw: stringify(initialValues),
        }),
        [initialValues, jsonKeys],
    );

    const form = useForm({
        defaultValues,
    });
    const result = useMemo(
        () => ({
            ...form,
            register: (
                name: Parameters<typeof form.register>[0],
                {
                    isJson,
                    ...opts
                }: Parameters<typeof form.register>[1] &
                    ({ isJson?: boolean } | undefined) = {},
            ) => {
                if (isJson) setJsonKeys((k) => [...k, name]);
                return {
                    ...form.register(name, {
                        ...opts,
                        disabled: opts.disabled || mode === "readonly",
                    }),
                    id: `settingform-${name}`,
                };
            },
            documentType,
            setDocumentType,
            view,
            setView,
            mode,
            setMode,
            isResetAllowed,
            setIsResetAllowed,
            isCompareAllowed,
            setIsCompareAllowed,
        }),
        [documentType, form, isCompareAllowed, isResetAllowed, mode, view],
    );
    const FormProvider = useCallback(
        ({
            children,
            onSubmit,
            ...props
        }: Omit<ComponentProps<typeof Form>, "onSubmit"> & {
            onSubmit: Parameters<typeof result.handleSubmit>[0];
        }) => {
            console.log("form prov", {
                result,
                children,
                onSubmit,
                SettingFormContext,
            });
            return (
                <Form
                    onSubmit={result.handleSubmit((v) => {
                        onSubmit(v);
                    })}
                    {...props}
                >
                    <SettingFormContext.Provider value={result}>
                        {children}
                    </SettingFormContext.Provider>
                    <DevTool control={result.control} />{" "}
                    {/* set up the dev tool */}
                </Form>
            );
        },
        [result],
    );
    return {
        ...result,
        Form: FormProvider,
    };
}

export function useSettingForm<T extends Record<string, any> = any>() {
    return useContext(SettingFormContext) as unknown as SettingFormCtx;
}
