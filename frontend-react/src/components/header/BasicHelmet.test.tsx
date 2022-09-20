import { render, waitFor } from "@testing-library/react";

import { BasicHelmet } from "./BasicHelmet";

jest.mock("../../config", () => {
    return {
        default: {
            APP_TITLE: "APP_TITLE",
        },
        __esModule: true,
    };
});

describe("BasicHelmet", () => {
    test("Renders with passed page title", async () => {
        render(<BasicHelmet pageTitle="A PAGE TITLE" />, {
            container: document.head,
        });
        await waitFor(() =>
            expect(document.title).toEqual("A PAGE TITLE | APP_TITLE")
        );
    });

    test("Renders without passed title", async () => {
        render(<BasicHelmet />);

        await waitFor(() => expect(document.title).toEqual("APP_TITLE"));
    });
});
