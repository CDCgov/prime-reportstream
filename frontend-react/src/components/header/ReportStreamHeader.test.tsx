import { screen, waitFor } from "@testing-library/react";

import ReportStreamHeader from "./ReportStreamHeader";
import { RSSessionContext } from "../../contexts/Session/SessionProvider";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { renderApp } from "../../utils/CustomRenderUtils";

const mockUseSessionContext = jest.mocked(useSessionContext);

describe("SignInOrUser", () => {
    // Every set of users should have access to the following Navbar items
    function testNav() {
        expect(screen.getByText("Getting started")).toBeVisible();
        expect(screen.getByText("Developers")).toBeVisible();
        expect(screen.getByText("Your connection")).toBeVisible();
        expect(screen.getByText("Support")).toBeVisible();
        expect(screen.getByText("About")).toBeVisible();
    }

    test("renders Sender permissioned ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserSender: true },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() =>
            expect(screen.getByText("Submissions")).toBeVisible(),
        );
        expect(screen.queryByText("Dashboard")).not.toBeInTheDocument();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
        testNav();
    });

    test("renders Receiver permissioned ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserReceiver: true },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() =>
            expect(screen.getByText("Dashboard")).toBeVisible(),
        );
        expect(screen.queryByText("Submissions")).not.toBeInTheDocument();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
        testNav();
    });

    test("renders Receiver AND Sender (Transceiver) permissioned ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserTransceiver: true },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() =>
            expect(screen.getByText("Dashboard")).toBeVisible(),
        );
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
        testNav();
    });

    test("renders Admin permissioned ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserAdmin: true, isAdminStrictCheck: true },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() =>
            expect(screen.getByText("Submissions")).toBeVisible(),
        );
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.getByText("Admin")).toBeVisible();
        testNav();
    });
});
