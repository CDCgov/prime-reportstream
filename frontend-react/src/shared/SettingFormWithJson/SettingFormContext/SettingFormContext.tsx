import {
    ComponentProps,
    ComponentType,
    Dispatch,
    SetStateAction,
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
} from "react";
import {
    Path,
    UseControllerProps,
    UseFormReturn as _UseFormReturn,
    useForm as _useForm,
    useController as _useController,
    FormProvider as HookFormProvider,
    useFormState as _useFormState,
    UseWatchProps,
    useWatch as _useWatch,
    UseFormStateProps,
} from "react-hook-form";
import { Form as USWDSForm } from "@trussworks/react-uswds";

import {
    checkJsonHybrid,
    cleanupJsonObject,
    createJsonHybrid,
} from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";

export type FormMode = "edit" | "new" | "clone" | "readonly";
export type FormView = "form" | "document" | "compare";
export type FormDocumentType = "json" | "yaml";

function stringify(obj: object, type: FormDocumentType = "json") {
    switch (type) {
        case "json": {
            const str = JSON.stringify(obj, undefined, 2);
            if (str === "{}" || str === "[]" || str === "null") {
                return "";
            }
            return str;
        }
        case "yaml":
            throw new Error("Not implemented");
        default:
            throw new TypeError(`Unknown type: ${type}`);
    }
}

export interface SettingFormCtx<M extends Record<string, any> = any> {
    hookForm: _UseFormReturn<M>;
    jsonKeys: (keyof M)[];
    addJsonKeys: (...args: Path<M>[]) => void;
    documentField?: FormDocumentField;
    setDocumentField: (name: string, type: FormDocumentType) => void;
    view: FormView;
    setView: Dispatch<SetStateAction<FormView>>;
    mode: FormMode;
    setMode: Dispatch<SetStateAction<FormMode>>;
    isResetAllowed: boolean;
    setIsResetAllowed: Dispatch<SetStateAction<boolean>>;
    isCompareAllowed: boolean;
    setIsCompareAllowed: Dispatch<SetStateAction<boolean>>;
}

export interface UseFormReturn<M extends Record<string, any> = any>
    extends SettingFormCtx<M> {
    /**
     * Form component helper that scaffolds providers.
     */
    Form: ComponentType<
        Omit<ComponentProps<typeof USWDSForm>, "onSubmit"> & {
            onSubmit: (v: M) => void;
        }
    >;
    /**
     * Form state helper component.
     * @example
     * <form.Subscribe
     *   formState={{isValid}}
     *   render={({isValid}) =>
     *     <button
     *       type="button"
     *       disabled={!isValid}>
     *         Conditional button
     *     </button>}
     * />
     */
    Subscribe: ComponentType<any>;
}

export const SettingFormContext = createContext<SettingFormCtx>({} as any);

export interface FormDocumentField {
    name: string;
    type: FormDocumentType;
}

export interface UseFormProps<T extends Record<string, any>> {
    initialValues?: T;
    mode?: FormMode;
    view?: FormView;

    documentField?: FormDocumentField;
    isResetAllowed?: boolean;
    isCompareAllowed?: boolean;
}

/**
 * Specialized form hook that allows fields to register as json string fields
 * to represent fields that do not have a dedicated widget. Allows a field
 * to register as a document field (ex: json) for raw text editing that syncs
 * both ways.
 */
export function useForm<T extends Record<string, any>>({
    initialValues = {} as any,
    mode: _mode = "new",
    view: _view = "form",
    documentField: _documentField,
    isResetAllowed: _isResetAllowed = false,
    isCompareAllowed: _isCompareAllowed = true,
}: UseFormProps<T>) {
    /**
     * Our form object that can represent field objects as json strings is considered
     * a JsonHybrid. Attention must be made as to whether you should be using this
     * JsonHybrid object directly or after converting it back into the original shape.
     * This is especially true for the Document -> Form / Form -> Document sync.
     */
    const [isCompareAllowed, setIsCompareAllowed] = useState(_isCompareAllowed);
    const [isResetAllowed, setIsResetAllowed] = useState(_isResetAllowed);
    const [mode, setMode] = useState(_mode);
    const [view, setView] = useState(_view);
    const [documentField, setDocumentField] = useState(_documentField);
    const setDocumentFieldFn = useCallback(
        (name: string, type: FormDocumentType) => {
            setDocumentField((f) => {
                if (name !== f?.name || type !== f?.type) {
                    return { name, type };
                }
                return f;
            });
        },
        [],
    );
    const [jsonKeys, setJsonKeys] = useState<(keyof T)[]>([]);
    const defaultValues = useMemo(() => {
        const values = createJsonHybrid(
            // cleanup empty objects from initial values
            cleanupJsonObject(initialValues),
            jsonKeys,
        );
        if (!documentField) return values;
        return {
            ...values,
            [documentField.name]: stringify(initialValues),
        };
    }, [initialValues, jsonKeys, documentField]);

    const form = _useForm({
        mode: "onChange",
        defaultValues: defaultValues as any,
    });

    const { isValid, touchedFields, isValidating } = form.formState;

    // keep default values sync'd
    useEffect(() => {
        form.reset(defaultValues);
    }, [initialValues, defaultValues, form]);

    // sync document or rest of form depending on initiator (touched)
    useEffect(() => {
        if (documentField) {
            // ignore valid state for document field, so that
            // we don't softlock from failed validation after
            // form sync
            if (touchedFields[documentField.name]) {
                // document field was updated, sync form from
                // hybridized object if parsable
                const str = form.getValues(documentField.name);
                let obj;
                try {
                    obj = createJsonHybrid(JSON.parse(str));
                } catch (e: any) {}
                if (obj) {
                    for (const [k, v] of Object.entries(obj)) {
                        form.setValue(k, v);
                    }
                }
            } else if (!isValidating && isValid) {
                // JSON validation passed and
                // form field(s) were updated, sync document
                // from unhybridized object
                const { obj } = checkJsonHybrid(
                    Object.fromEntries(
                        Object.entries(form.getValues()).filter(
                            ([k]) => k !== documentField.name,
                        ),
                    ) as typeof defaultValues,
                    jsonKeys,
                );
                const str = stringify(obj);
                form.setValue(documentField.name, str);
            }
            // reset touched state so we know when to sync again
            form.control._updateFormState({ touchedFields: {} });
        }
    }, [documentField, form, isValid, isValidating, jsonKeys, touchedFields]);

    const state: SettingFormCtx<typeof defaultValues> = useMemo<
        SettingFormCtx<typeof defaultValues>
    >(() => {
        return {
            hookForm: form,
            addJsonKeys: (...args: Path<typeof defaultValues>[]) => {
                const newKeys = args.filter((a) => !jsonKeys.includes(a));
                for (const jsonKey of newKeys) {
                    const iValueStr = stringify(initialValues[jsonKey] ?? {});
                    form.setValue(jsonKey, iValueStr as any);
                    setJsonKeys((k) => [...k, jsonKey]);
                }
            },
            jsonKeys,
            documentField,
            setDocumentField: setDocumentFieldFn,
            view,
            setView,
            mode,
            setMode,
            isResetAllowed,
            setIsResetAllowed,
            isCompareAllowed,
            setIsCompareAllowed,
        };
    }, [
        form,
        documentField,
        setDocumentFieldFn,
        view,
        mode,
        isResetAllowed,
        isCompareAllowed,
        jsonKeys,
        initialValues,
    ]);

    const hookState = useMemo(() => {
        return {
            ...state,
            Form: ({ onSubmit, ...props }) => {
                return (
                    <FormProvider form={state}>
                        <USWDSForm
                            {...props}
                            onSubmit={state.hookForm.handleSubmit((v) => {
                                // unhybridize, remove document fields, and cleanup empty objects
                                const obj = cleanupJsonObject(
                                    Object.fromEntries(
                                        Object.entries(
                                            checkJsonHybrid(v, state.jsonKeys)
                                                .obj,
                                        ).filter(
                                            ([k]) => k !== documentField?.name,
                                        ),
                                    ),
                                );

                                onSubmit(obj);
                            })}
                        />
                    </FormProvider>
                );
            },
            Subscribe: ({
                formState,
                render,
            }: {
                formState?: UseFormStateProps<typeof defaultValues>;
                render: (
                    state: ReturnType<
                        typeof useFormState<typeof defaultValues>
                    >,
                ) => JSX.Element;
            }) => {
                const state = useFormState<typeof defaultValues>(formState);
                return render(state);
            },
        } satisfies UseFormReturn<typeof defaultValues>;
    }, [documentField?.name, state]);

    return hookState;
}

export function useFormContext<T extends Record<string, any> = any>() {
    return useContext(SettingFormContext) as unknown as SettingFormCtx<T>;
}

export function useController({
    isJson,
    documentType,
    ...props
}: UseControllerProps & { isJson?: boolean; documentType?: FormDocumentType }) {
    const { addJsonKeys, setDocumentField } = useFormContext();
    const controller = _useController({ ...props });

    useEffect(() => {
        if (isJson) {
            addJsonKeys(props.name);
        }
        if (documentType) {
            setDocumentField(props.name, documentType);
        }
    }, [addJsonKeys, documentType, isJson, props.name, setDocumentField]);

    return controller;
}

export function FormProvider({ children, form }: any) {
    return (
        <SettingFormContext.Provider value={form}>
            <HookFormProvider {...form.hookForm}>{children}</HookFormProvider>
        </SettingFormContext.Provider>
    );
}

export const useFormState = _useFormState;
export const useWatch = _useWatch;
