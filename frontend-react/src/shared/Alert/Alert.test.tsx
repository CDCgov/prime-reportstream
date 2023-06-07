import { renderApp } from "../../utils/CustomRenderUtils";

import Alert from "./Alert";

describe("Alert", () => {
    test("renders", () => {
        renderApp(<Alert type="info" headingLevel="h3" />);
    });
});
