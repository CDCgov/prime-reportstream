import { renderHook } from "@testing-library/react";

import { useFormContext } from "./SettingFormContext";

describe("SettingFormContext", () => {
    describe("useSettingForm", () => {
        test("returns context", () => {
            const ctx = renderHook(() => useFormContext());
            expect(ctx).not.toBeUndefined();
        });
    });
});
