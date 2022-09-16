import * as ReactRouter from "react-router";

import { useDAP } from "./DAPHeader";

describe("Test useDAP for DAPHeader component", () => {
    const useLocation = jest.spyOn(ReactRouter, "useLocation");

    test("Check DAP for root homepage (public)", () => {
        useLocation.mockReturnValue({ pathname: "/" } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("development")).toBe(false);
        expect(useDAP(undefined)).toBe(false);
        expect(useDAP("")).toBe(false);
    });

    test("Check DAP for upload page (NOT public)", () => {
        useLocation.mockReturnValue({ pathname: "/upload" } as any);
        expect(useDAP("production")).toBe(false);
        expect(useDAP("staging")).toBe(false);
    });
});
