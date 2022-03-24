import { screen } from "@testing-library/react";
import { useContext } from "react";

import { orgServer } from "../__mocks__/OrganizationMockServer";
import { renderWithSession } from "../utils/CustomRenderUtils";

import { setStoredOrg, setStoredSenderName } from "./SessionStorageTools";
import { SessionStorageContext } from "./SessionStorageContext";

const TestComponent = () => {
    const { values } = useContext(SessionStorageContext);

    return (
        <>
            <div>{values.org}</div>
            <div>{values.senderName}</div>
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
