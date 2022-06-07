import { screen } from "@testing-library/react";
import { useContext } from "react";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import { renderWithSession } from "../utils/CustomRenderUtils";

import { setStoredOrg, setStoredSenderName } from "./SessionStorageTools";
import { SessionContext } from "./SessionContext";

const TestComponent = () => {
    const { memberships, store } = useContext(SessionContext);

    return (
        <>
            <div>{store.values.org}</div>
            <div>{store.values.senderName}</div>
            <div>{memberships.state.active?.parsedName || ""}</div>
            <div>{memberships.state.active?.memberType || ""}</div>
        </>
    );
};

beforeAll(() => {
    orgServer.listen();
    setStoredOrg("testOrg");
    setStoredSenderName("testSender");
});
afterEach(() => orgServer.resetHandlers());
afterAll(() => orgServer.close());

beforeEach(() => {
    renderWithSession(<TestComponent />);
});

describe("SessionStorageContext", () => {
    test("default values", async () => {
        const orgDiv = await screen.findByText("testOrg");
        const senderDiv = await screen.findByText("testSender");
        expect(orgDiv).toBeInTheDocument();
        expect(senderDiv).toBeInTheDocument();
    });
});
