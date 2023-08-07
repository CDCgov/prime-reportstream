import { fireEvent, screen } from "@testing-library/react";
import React from "react";

import { renderApp } from "../../utils/CustomRenderUtils";
import { mockFeatureFlagContext } from "../../contexts/__mocks__/FeatureFlagContext";

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

describe("AdminDropdownNav - value-sets", () => {
    beforeEach(() => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            featureFlags: [],
            checkFlag: jest.fn((flag) => flag === "value-sets"),
        });
    });
    test("Admin menu expands and contracts on click and selection", () => {
        renderApp(<AdminDropdown />);
        expect(screen.getByRole("button")).toHaveAttribute(
            "aria-expanded",
            "false",
        );
        fireEvent.click(screen.getByRole("button"));
        expect(screen.getByRole("button")).toHaveAttribute(
            "aria-expanded",
            "true",
        );
        fireEvent.click(screen.getByText("Feature Flags"));
        expect(screen.getByRole("button")).toHaveAttribute(
            "aria-expanded",
            "false",
        );
    });

    test("Current admin pages", () => {
        renderApp(<AdminDropdown />);
        const settings = screen.getByText("Organization Settings");
        const featureFlags = screen.getByText("Feature Flags");
        const lastMileFailures = screen.getByText("Last Mile Failures");
        const messageTracker = screen.getByText("Message Id Search");
        const receiverStatus = screen.getByText("Receiver Status Dashboard");
        const queryForNavItemValueSets = screen.queryByText("Value Sets");
        const validate = screen.queryByText("Validate");

        expect(settings).toBeInTheDocument();
        expect(featureFlags).toBeInTheDocument();
        expect(lastMileFailures).toBeInTheDocument();
        expect(messageTracker).toBeInTheDocument();
        expect(receiverStatus).toBeInTheDocument();
        expect(queryForNavItemValueSets).toBeInTheDocument();
        expect(validate).toBeInTheDocument();
    });
});
