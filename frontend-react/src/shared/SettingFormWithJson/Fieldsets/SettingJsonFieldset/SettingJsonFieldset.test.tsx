import { renderApp, screen } from "../../../../utils/CustomRenderUtils";

import SettingJsonFieldset from "./SettingJsonFieldset";

const fields = ["json"];

describe("SettingJsonFieldset", () => {
    describe("renders fields", () => {
        test.each(fields)("%s", (field) => {
            renderApp(<SettingJsonFieldset />);
            expect(screen.getByTestId(field)).toBeInTheDocument();
        });
    });
});
