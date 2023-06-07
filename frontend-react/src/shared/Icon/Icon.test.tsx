import { renderApp } from "../../utils/CustomRenderUtils";

import Icon from "./Icon";

describe("Icon", () => {
    test("renders", () => {
        renderApp(<Icon name="CheckCircle" />);
    });
});
