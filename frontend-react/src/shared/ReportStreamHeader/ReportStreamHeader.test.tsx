import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";

import { ReportStreamHeaderBase } from "./ReportStreamHeader";

vi.mock("../SenderModeBanner/SenderModeBanner");

describe("ReportStreamHeader", () => {
    test("renders children", () => {
        render(<ReportStreamHeaderBase>Test</ReportStreamHeaderBase>);
        expect(screen.getByText("Test")).toBeInTheDocument();
    });
});
