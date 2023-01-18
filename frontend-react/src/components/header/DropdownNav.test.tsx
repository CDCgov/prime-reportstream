import { fireEvent, screen } from "@testing-library/react";
import React from "react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";
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
        const lastMileFailures = screen.getByText("Last Mile Failures");
        const receiverStatus = screen.getByText("Receiver Status Dashboard");
        const queryForNavItemValueSets = screen.queryByText("Value Sets");
        const validate = screen.queryByText("Validate");

        expect(settings).toBeInTheDocument();
        expect(featureFlags).toBeInTheDocument();
        expect(lastMileFailures).toBeInTheDocument();
        expect(receiverStatus).toBeInTheDocument();
        expect(queryForNavItemValueSets).toBeInTheDocument();
        expect(validate).toBeInTheDocument();
    });
});

describe("AdminDropdownNav - user-upload", () => {
    beforeEach(() => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            featureFlags: [],
            checkFlag: jest.fn((flag) => flag === "user-upload"),
        });
    });

    test("Flagged user-upload page is hidden by default", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            featureFlags: [],
            checkFlag: jest.fn((flag) => flag !== "user-upload"),
        });
        renderWithRouter(<AdminDropdown />);
        const userUpload = screen.queryByText("User Upload");

        // Assert they're hidden without flag
        expect(userUpload).not.toBeInTheDocument();
    });

    test("Flagged user-upload page is hidden by default", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            featureFlags: [],
            checkFlag: jest.fn((flag) => flag !== "user-upload"),
        });
        renderWithRouter(<AdminDropdown />);
        const userUpload = screen.queryByText("User Upload");

        // Assert they're hidden without flag
        expect(userUpload).not.toBeInTheDocument();
    });

    test("Flagged user-upload is shown when flag is set", () => {
        renderWithRouter(<AdminDropdown />);
        const userUpload = screen.queryByText("User Upload");

        // Assert not hidden
        expect(userUpload).toBeInTheDocument();
    });
});

describe("AdminDropdownNav - message-tracker", () => {
    beforeEach(() => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            featureFlags: [],
            checkFlag: jest.fn((flag) => flag === "message-tracker"),
        });
    });

    test("Flagged message-tracker page is hidden by default", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            featureFlags: [],
            checkFlag: jest.fn((flag) => flag !== "message-tracker"),
        });
        renderWithRouter(<AdminDropdown />);
        const queryForNavItemMessageIdSearch =
            screen.queryByText("Message Id Search");

        // Assert they're hidden without flag
        expect(queryForNavItemMessageIdSearch).not.toBeInTheDocument();
    });

    test("Flagged message-tracker page is hidden by default", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            featureFlags: [],
            checkFlag: jest.fn((flag) => flag !== "message-tracker"),
        });
        renderWithRouter(<AdminDropdown />);
        const queryForNavItemMessageIdSearch =
            screen.queryByText("Message Id Search");

        // Assert they're hidden without flag
        expect(queryForNavItemMessageIdSearch).not.toBeInTheDocument();
    });

    test("Flagged message-tracker page is shown when flag is set", () => {
        renderWithRouter(<AdminDropdown />);
        const queryForNavItemMessageIdSearch =
            screen.queryByText("Message Id Search");

        // Assert not hidden
        expect(queryForNavItemMessageIdSearch).toBeInTheDocument();
    });
});
