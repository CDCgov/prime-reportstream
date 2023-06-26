import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import AlertIcon, { getIconName } from "./AlertIcon";
import type { AlertIconProps } from "./AlertIcon";

describe("AlertIcon", () => {
    test("renders with defaults", () => {
        renderApp(<AlertIcon type="info" />);
        const icon = screen.getByRole("img");
        expect(icon).toBeInTheDocument();
    });

    test.each([
        "info",
        "warning",
        "tip",
        "error",
        "success",
    ] as AlertIconProps["type"][])("renders default icon type: %s", (type) => {
        renderApp(<AlertIcon type={type} />);
        expect(screen.getByLabelText(getIconName(type))).toBeInTheDocument();
    });

    test("renders with custom icon", () => {
        renderApp(<AlertIcon type="warning" icon="Alarm" />);
        expect(screen.getByLabelText("Alarm")).toBeInTheDocument();
    });
});
