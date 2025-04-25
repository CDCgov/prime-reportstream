import { screen } from "@testing-library/react";

import { StaticAlert, StaticAlertType } from "./StaticAlert";
import { renderApp } from "../utils/CustomRenderUtils";

describe("StaticAlert", () => {
    test("renders correct class for success", async () => {
        renderApp(<StaticAlert type={StaticAlertType.Success} heading={"any"} />);

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--success");
    });

    test("renders correct class for error", async () => {
        renderApp(<StaticAlert type={StaticAlertType.Error} heading={"any"} />);

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--error");
    });
});
