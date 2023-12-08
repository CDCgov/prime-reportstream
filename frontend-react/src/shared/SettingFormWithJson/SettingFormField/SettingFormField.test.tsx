import { renderApp, screen } from "../../../utils/CustomRenderUtils";

import { SettingFormFieldRow } from "./SettingFormField";

describe("SettingFormFieldRow", () => {
    function setup() {
        renderApp(<SettingFormFieldRow>Test</SettingFormFieldRow>);
    }

    test("renders", () => {
        setup();
        expect(screen.getByText("Test")).toBeInTheDocument();
    });
});
