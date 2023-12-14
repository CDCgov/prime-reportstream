import { renderApp, screen } from "../../utils/CustomRenderUtils";

describe.skip("EditableCompare", () => {
    test("renders", () => {
        renderApp(<></>);
        const _ = screen.getByText("test");
    });
});
