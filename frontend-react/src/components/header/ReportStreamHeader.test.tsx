import { screen } from "@testing-library/react";

import ReportStreamHeader from "./ReportStreamHeader";
import { RSSessionContext } from "../../contexts/Session/SessionProvider";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { renderApp } from "../../utils/CustomRenderUtils";
import { MemberType } from "../../utils/OrganizationUtils";

const mockUseSessionContext = vi.mocked(useSessionContext);

describe("SignInOrUser", () => {
    test("renders header with ReportStream label", () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserSender: true },
            activeMembership: {
                parsedName: "ignore",
                memberType: MemberType.SENDER,
                service: "default",
            },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        expect(screen.getByText("ReportStream")).toBeVisible();
    });
});
