import { screen } from "@testing-library/react";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { SessionCtx } from "../../contexts/SessionContext";
import { renderApp } from "../../utils/CustomRenderUtils";

import ReportStreamHeader from "./ReportStreamHeader";

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
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserSender: true },
        } as SessionCtx);
        renderApp(<ReportStreamHeader />);
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.queryByText("Dashboard")).not.toBeInTheDocument();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Receiver permissioned ReportStreamNavbar", () => {
        mockSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserReceiver: true },
        } as SessionCtx);
        renderApp(<ReportStreamHeader />);
        expect(screen.queryByText("Submissions")).not.toBeInTheDocument();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Receiver AND Sender (Transceiver) permissioned ReportStreamNavbar", () => {
        mockSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserTransceiver: true },
        } as SessionCtx);
        renderApp(<ReportStreamHeader />);
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Admin permissioned ReportStreamNavbar", () => {
        mockSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserAdmin: true, isAdminStrictCheck: true },
        } as SessionCtx);
        renderApp(<ReportStreamHeader />);
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.getByText("Admin")).toBeVisible();
    });
});
