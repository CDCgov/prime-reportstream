import { screen } from "@testing-library/react";

import { SessionCtx } from "../../contexts/Session";
import { renderApp } from "../../utils/CustomRenderUtils";
import { mockUseSessionContext } from "../contexts/Session/__mocks__";

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
        mockUseSessionContext.mockReturnValue({
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
        mockUseSessionContext.mockReturnValue({
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
        mockUseSessionContext.mockReturnValue({
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
        mockUseSessionContext.mockReturnValue({
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
