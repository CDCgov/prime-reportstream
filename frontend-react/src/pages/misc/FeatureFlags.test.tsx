import { render, screen, fireEvent } from "@testing-library/react";

import { FeatureFlagActionType } from "../../contexts/FeatureFlagContext";
import { mockFeatureFlagContext } from "../../contexts/__mocks__/FeatureFlagContext";

import { FeatureFlagUIComponent } from "./FeatureFlags";

jest.mock("../../config", () => {
    return {
        default: {
            DEFAULT_FEATURE_FLAGS: "flag-3",
        },
        __esModule: true,
    };
});

describe("FeatureFlags", () => {
    test("displays a list of current feature flags", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            checkFlag: jest.fn(),
            featureFlags: ["flag-1", "flag-2", "flag-3"],
        });
        render(<FeatureFlagUIComponent />);

        const featureFlagAlerts = screen.getAllByTestId("alert");
        expect(featureFlagAlerts).toHaveLength(3);
        expect(screen.getAllByTestId("alert")[0]).toContainHTML(
            "<b>flag-1</b>"
        );
        expect(screen.getAllByTestId("alert")[1]).toContainHTML(
            "<b>flag-2</b>"
        );
        expect(screen.getAllByTestId("alert")[2]).toContainHTML(
            "<b>flag-3</b>"
        );
    });
    test("displays a remove button for feature flags not set at env level", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => {},
            checkFlag: jest.fn(),
            featureFlags: ["flag-1", "flag-2", "flag-3"],
        });
        render(<FeatureFlagUIComponent />);

        const featureFlagDeleteButtons = screen.getAllByRole("button");
        expect(featureFlagDeleteButtons).toHaveLength(3); // 1 add button + 2 delete buttons

        // hard to test more exactly than this but this somewhat confirms that we only get 2 delete buttons
        // since `flag-3` is added at env level
        expect(screen.getAllByRole("button")[0]).toHaveTextContent("Add");
        expect(screen.getAllByRole("button")[1]).toContainHTML("Delete");
        expect(screen.getAllByRole("button")[2]).toContainHTML("Delete");
    });
    test("calls dispatch on add button click with new feature flag name", () => {
        const mockDispatch = jest.fn();
        mockFeatureFlagContext.mockReturnValue({
            dispatch: mockDispatch,
            checkFlag: jest.fn(),
            featureFlags: ["flag-1"],
        });
        render(<FeatureFlagUIComponent />);

        const addButton = screen.getAllByRole("button")[0];
        const textInput = screen.getByRole("textbox");
        fireEvent.change(textInput, { target: { value: "flag-4" } });
        fireEvent.click(addButton);

        expect(mockDispatch).toHaveBeenCalledWith({
            type: FeatureFlagActionType.ADD,
            payload: "flag-4",
        });
    });
    test("does not call dispatch on add button click if flag already exists", () => {
        const mockDispatch = jest.fn();
        mockFeatureFlagContext.mockReturnValue({
            dispatch: mockDispatch,
            checkFlag: jest.fn(() => true),
            featureFlags: ["flag-1"],
        });
        render(<FeatureFlagUIComponent />);

        const addButton = screen.getAllByRole("button")[0];
        const textInput = screen.getByRole("textbox");
        fireEvent.change(textInput, {
            target: {
                value: "not relevant, test depends on checkFlag implementation",
            },
        });
        fireEvent.click(addButton);

        expect(mockDispatch).not.toHaveBeenCalled();
    });
});
