import { fireEvent, screen } from "@testing-library/react";

import { FeatureFlagsPage } from "./FeatureFlags";
import { FeatureFlagActionType } from "../../contexts/FeatureFlag/FeatureFlagProvider";
import useFeatureFlags from "../../contexts/FeatureFlag/useFeatureFlags";
import { renderApp } from "../../utils/CustomRenderUtils";

vi.mock("../../config", async (importActual) => {
    const originalModule = await importActual<typeof import("../../config")>();
    return {
        ...originalModule,
        default: {
            ...originalModule.default,
            DEFAULT_FEATURE_FLAGS: "flag-3",
        },
        __esModule: true,
    };
});

const mockFeatureFlagContext = vi.mocked(useFeatureFlags);

describe("FeatureFlags", () => {
    test("displays a list of current feature flags", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => void 0,
            checkFlags: vi.fn(),
            featureFlags: ["flag-1", "flag-2", "flag-3"],
        });
        renderApp(<FeatureFlagsPage />);

        const featureFlagAlerts = screen.getAllByTestId("alert");
        expect(featureFlagAlerts).toHaveLength(3);
        expect(screen.getAllByTestId("alert")[0]).toContainHTML(
            "<b>flag-1</b>",
        );
        expect(screen.getAllByTestId("alert")[1]).toContainHTML(
            "<b>flag-2</b>",
        );
        expect(screen.getAllByTestId("alert")[2]).toContainHTML(
            "<b>flag-3</b>",
        );
    });
    test("displays a remove button for feature flags not set at env level", () => {
        mockFeatureFlagContext.mockReturnValue({
            dispatch: () => void 0,
            checkFlags: vi.fn(),
            featureFlags: ["flag-1", "flag-2", "flag-3"],
        });
        renderApp(<FeatureFlagsPage />);

        const featureFlagDeleteButtons = screen.getAllByRole("button");
        expect(featureFlagDeleteButtons).toHaveLength(3); // 1 add button + 2 delete buttons

        // hard to test more exactly than this but this somewhat confirms that we only get 2 delete buttons
        // since `flag-3` is added at env level
        expect(screen.getAllByRole("button")[0]).toHaveTextContent("Add");
        expect(screen.getAllByRole("button")[1]).toContainHTML("Delete");
        expect(screen.getAllByRole("button")[2]).toContainHTML("Delete");
    });
    test("calls dispatch on add button click with new feature flag name", () => {
        const mockDispatch = vi.fn();
        mockFeatureFlagContext.mockReturnValue({
            dispatch: mockDispatch,
            checkFlags: vi.fn(),
            featureFlags: ["flag-1"],
        });
        renderApp(<FeatureFlagsPage />);

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
        const mockDispatch = vi.fn();
        mockFeatureFlagContext.mockReturnValue({
            dispatch: mockDispatch,
            checkFlags: vi.fn(() => true),
            featureFlags: ["flag-1"],
        });
        renderApp(<FeatureFlagsPage />);

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
