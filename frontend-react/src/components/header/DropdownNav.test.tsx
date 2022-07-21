import { fireEvent, screen } from "@testing-library/react";
import React from "react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { AdminDropdown } from "./DropdownNav";

class TestLocalStorage {
    store: Map<string, string | string[]> = new Map();

    constructor(kvPairs?: Array<{ k: string; v: string }>) {
        if (kvPairs) {
            kvPairs.forEach(({ k, v }) => this.store.set(k, v));
        }
    }

    getItem(key: string) {
        return this.store.get(key);
    }

    setItem(key: string, value: string | string[]) {
        this.store.set(key, value);
    }

    removeItem(key: string) {
        this.store.delete(key);
    }

    clear() {
        this.store = new Map();
    }
}

const mockLocalStorage = new TestLocalStorage();
jest.mock("../../pages/misc/FeatureFlags", () => {
    const originalModule = jest.requireActual("../../pages/misc/FeatureFlags");
    return {
        ...originalModule,
        CheckFeatureFlag: (feature: string) => {
            return (
                mockLocalStorage.getItem("featureFlags")?.includes(feature) ||
                false
            );
        },
    };
});

describe("AdminDropdownNav", () => {
    test("Admin menu expands and contracts on click and selection", () => {
        renderWithRouter(<AdminDropdown />);
        expect(screen.getByRole("button")).toHaveAttribute(
            "aria-expanded",
            "false"
        );
        fireEvent.click(screen.getByRole("button"));
        expect(screen.getByRole("button")).toHaveAttribute(
            "aria-expanded",
            "true"
        );
        fireEvent.click(screen.getByText("Feature Flags"));
        expect(screen.getByRole("button")).toHaveAttribute(
            "aria-expanded",
            "false"
        );
    });

    test("Current admin pages", () => {
        renderWithRouter(<AdminDropdown />);
        const settings = screen.getByText("Organization Settings");
        const featureFlags = screen.getByText("Feature Flags");

        expect(settings).toBeInTheDocument();
        expect(featureFlags).toBeInTheDocument();
    });

    test("Flagged admin pages are hidden", () => {
        renderWithRouter(<AdminDropdown />);
        const queryForNavItem = screen.queryByText("Value Sets");

        // Assert they're hidden without flag
        expect(queryForNavItem).not.toBeInTheDocument();
    });

    test("Flagged admin pages are shown when flag is set", () => {
        mockLocalStorage.setItem("featureFlags", ["value-sets"]);

        renderWithRouter(<AdminDropdown />);
        const queryForNavItem = screen.queryByText("Value Sets");

        // Assert not hidden
        expect(queryForNavItem).toBeInTheDocument();
    });
});
