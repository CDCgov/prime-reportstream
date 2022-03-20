import { screen } from "@testing-library/react";
import { useContext } from "react";

import { orgServer } from "../__mocks__/OrgContextMockServer";
import { renderWithOrgContext } from "../utils/CustomRenderUtils";
import { Organization } from "../network/api/OrgApi";

import { IOrgContext, OrgContext } from "./OrgContext";

export const dummyOrg: Organization = {
    name: "ignore",
    description: "FOR TESTING ONLY",
    jurisdiction: "FEDERAL",
    filters: [],
    meta: {
        version: 0,
        createdBy: "local@test.com",
        createdAt: "2022-01-28T13:55:15.428445-05:00",
    },
};

export const dummyPayload: IOrgContext = {
    values: {
        org: dummyOrg,
        oktaGroup: "ignore",
    },
    controller: {
        updateOktaOrg: (val: string) => {
            console.log(val);
        },
    },
};

const OrgConsumer = () => {
    const { values } = useContext(OrgContext);

    return (
        <>
            <span>{values.org?.name || "failed"}</span>
            <span>{values.org?.description || "failed"}</span>
            <span>{values.org?.jurisdiction || "failed"}</span>
        </>
    );
};

describe("OrgContext", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    beforeEach(() => {
        renderWithOrgContext(<OrgConsumer />);
    });

    test("Values are provided", async () => {
        const name = screen.getByText(dummyOrg.name);
        const desc = await screen.findByText(dummyOrg.description);
        const jurisdiction = await screen.findByText(dummyOrg.jurisdiction);
        const stateCode = await screen.findByText(dummyOrg?.stateCode || "");
        const countyName = await screen.findByText(dummyOrg?.countyName || "");

        expect(name).toBeInTheDocument();
        expect(desc).toBeInTheDocument();
        expect(jurisdiction).toBeInTheDocument();
        expect(stateCode).toBeInTheDocument();
        expect(countyName).toBeInTheDocument();
    });
});
