import { createContext, useContext } from "react";

export type SettingFormMode = "edit" | "clone" | "new";
export type SettingFormFieldJsonType = "whole" | "field";

export interface SettingFormCtx {
    mode: SettingFormMode;
    defaultValues: any;
    onFieldChange: (k: string, v: unknown) => void;
    getId: (v: string) => string;
    registerField: (k: string, jsonType?: SettingFormFieldJsonType) => void;
    json: string;
    isReadonly?: boolean;
}

export const SettingFormContext = createContext<SettingFormCtx>({} as any);

export function useSettingForm() {
    return useContext(SettingFormContext);
}
