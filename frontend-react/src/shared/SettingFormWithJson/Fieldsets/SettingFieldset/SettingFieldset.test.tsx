import { RSSetting } from "../../../../config/endpoints/settings";
import { renderApp, screen } from "../../../../utils/CustomRenderUtils";

import SettingFieldset from "./SettingFieldset";

const fields = ["name", "description"] satisfies (keyof RSSetting)[];

describe("SettingFieldset", () => {
    describe("renders fields", () => {
        test.each(fields)("%s", (field) => {
            renderApp(<SettingFieldset />);
            expect(screen.getByTestId(field)).toBeInTheDocument();
        });
    });
});
