import { FormApi } from "@tanstack/react-form";
import { createContext, useContext } from "react";

import { RSSetting } from "../../../config/endpoints/settings";
import type { SettingFormMode } from "../SettingForm/SettingForm";
import { ObjectJsonHybrid } from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";

export type JsonFormValue<T> = T & {
    _raw: string;
};

export interface SettingFormCtx<
    T extends RSSetting = RSSetting,
    K extends (string & keyof T) | undefined = undefined,
> {
    mode: SettingFormMode;
    getId: (v: string) => string;
    registerJsonFields: (...args: (string & keyof T)[]) => void;
    form: FormApi<JsonFormValue<ObjectJsonHybrid<T, K>>, unknown>;
}

export const SettingFormContext = createContext<SettingFormCtx>({} as any);

export function useSettingForm<
    T extends RSSetting = RSSetting,
    K extends (string & keyof T) | undefined = undefined,
>() {
    return useContext(SettingFormContext) as unknown as SettingFormCtx<T, K>;
}
