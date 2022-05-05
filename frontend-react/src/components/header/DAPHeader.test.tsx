import routeData from "react-router";

import { useDAP } from "./DAPHeader";

describe("Test useDAP for DAPHeader component", () => {
    const useLocation = jest.spyOn(routeData, "useLocation");

    test("Check DAP for root homepage, login and TOS pages (public)", () => {
        useLocation.mockReturnValue({ pathname: "/" } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("development")).toBe(false);
        expect(useDAP(undefined)).toBe(false);
        useLocation.mockReturnValue({ pathname: "/login" } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("staging")).toBe(false);
        useLocation.mockReturnValue({ pathname: "/terms-of-service" } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("test")).toBe(false);
    });

    test("Check DAP for upload page (NOT public)", () => {
        useLocation.mockReturnValue({ pathname: "/upload" } as any);
        expect(useDAP("production")).toBe(false);
        expect(useDAP("staging")).toBe(false);
    });

    test("Check DAP for a 'Getting Started' page (public)", () => {
        useLocation.mockReturnValue({
            pathname: "/getting-started/public-health-departments/overview",
        } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("staging")).toBe(false);
        useLocation.mockReturnValue({
            pathname: "/getting-started/testing-facilities/csv-upload-guide",
        } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("staging")).toBe(false);
        useLocation.mockReturnValue({
            pathname: "/getting-started/anything",
        } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("staging")).toBe(false);
        useLocation.mockReturnValue({
            pathname: "/getting-storked/testing-facilities/csv-upload-guide",
        } as any);
        expect(useDAP("production")).toBe(false);
        expect(useDAP("staging")).toBe(false);
    });

    test("Check DAP for a 'How it works' page (public)", () => {
        useLocation.mockReturnValue({ pathname: "/how-it-works/about" } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("staging")).toBe(false);
        useLocation.mockReturnValue({
            pathname: "/how-it-works/systems-and-settings",
        } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("staging")).toBe(false);
        useLocation.mockReturnValue({
            pathname: "/how-it-works/anything",
        } as any);
        expect(useDAP("production")).toBe(true);
        expect(useDAP("staging")).toBe(false);
        useLocation.mockReturnValue({
            pathname: "/how-it-might-work/security-practices",
        } as any);
        expect(useDAP("production")).toBe(false);
        expect(useDAP("staging")).toBe(false);
    });
});
