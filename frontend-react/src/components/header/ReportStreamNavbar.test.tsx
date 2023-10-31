import { screen } from "@testing-library/react";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { RSSessionContext } from "../../contexts/SessionContext";
import { renderApp } from "../../utils/CustomRenderUtils";

import { ReportStreamNavbar } from "./ReportStreamNavbar";

describe("SignInOrUser", () => {
    // Every set of users should have access to the following Navbar items
    afterEach(() => {
        expect(screen.getByText("Getting started")).toBeVisible();
        expect(screen.getByText("Developers")).toBeVisible();
        expect(screen.getByText("Your connection")).toBeVisible();
        expect(screen.getByText("Support")).toBeVisible();
        expect(screen.getByText("About")).toBeVisible();
    });

    test("renders Sender permissioned ReportStreamNavbar", () => {
        mockSessionContext.mockReturnValue({
            isUserSender: true,
        } as RSSessionContext);
        renderApp(<ReportStreamNavbar />);
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.queryByText("Dashboard")).not.toBeInTheDocument();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Receiver permissioned ReportStreamNavbar", () => {
        mockSessionContext.mockReturnValue({
            isUserReceiver: true,
        } as RSSessionContext);
        renderApp(<ReportStreamNavbar />);
        expect(screen.queryByText("Submissions")).not.toBeInTheDocument();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Receiver AND Sender (Transceiver) permissioned ReportStreamNavbar", () => {
        mockSessionContext.mockReturnValue({
            isUserTransceiver: true,
        } as RSSessionContext);
        renderApp(<ReportStreamNavbar />);
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Admin permissioned ReportStreamNavbar", () => {
        mockSessionContext.mockReturnValue({
            isUserAdmin: true,
            isAdminStrictCheck: true,
        } as RSSessionContext);
        renderApp(<ReportStreamNavbar />);
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.getByText("Admin")).toBeVisible();
    });
});
