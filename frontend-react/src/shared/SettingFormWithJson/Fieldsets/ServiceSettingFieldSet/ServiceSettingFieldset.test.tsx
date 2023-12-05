import { RSService } from "../../../../config/endpoints/settings";
import { renderApp, screen } from "../../../../utils/CustomRenderUtils";

import ServiceSettingFieldset from "./ServiceSettingFieldset";

const fields = ["topic", "customerStatus"] satisfies (keyof RSService)[];

describe("ServiceSettingFieldSet", () => {
    describe("renders fields", () => {
        test.each(fields)("%s", (field) => {
            renderApp(<ServiceSettingFieldset />);
            expect(screen.getByTestId(field)).toBeInTheDocument();
        });
    });
});
