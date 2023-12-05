import { createContext, useContext } from "react";

import { RSSetting } from "../../config/endpoints/settings";

export interface SettingFormCtx<T extends RSSetting = RSSetting> {
    setting: Partial<T>;
    mode: "edit" | "clone";
    defaultValues: Partial<T>;
    setSetting: (v: T | ((s: T) => T)) => void;
    updateSettingProperty: (k: keyof T, v: T[typeof k]) => void;
    onJsonInvalid: (e: Error) => void;
}

export const SettingFormContext = createContext<SettingFormCtx>({} as any);

export function useSettingForm<T extends RSSetting = RSSetting>() {
    const ctx = useContext(SettingFormContext) as unknown as SettingFormCtx<T>;

    return ctx;
}
