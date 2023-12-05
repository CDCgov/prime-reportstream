import { renderHook } from "@testing-library/react";

import { useSettingForm } from "./SettingFormContext";

describe("SettingFormContext", () => {
    describe("useSettingForm", () => {
        test("returns context", () => {
            const ctx = renderHook(() => useSettingForm());
            expect(ctx).not.toBeUndefined();
        });
    });
});
