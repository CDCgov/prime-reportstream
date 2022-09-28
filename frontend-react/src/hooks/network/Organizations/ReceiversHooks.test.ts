import { act, renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { receiversGenerator } from "../../../network/api/Organizations/Receivers";

import { useReceiversList } from "./ReceiversHooks";

const fakeSession = {
    oktaToken: {
        accessToken: "TOKEN",
    },
    activeMembership: {
        memberType: MemberType.RECEIVER,
        parsedName: "testOrg",
        senderName: undefined,
    },
    dispatch: () => {},
    initialized: true,
};
const handlers = [
    rest.get(
        "https://test.prime.cdc.gov/api/settings/organizations/testOrg/receivers",
        (req, res, ctx) => {
            const array = receiversGenerator(3);
            return res(ctx.status(200), ctx.json(array));
        }
    ),
];

const server = setupServer(...handlers);

describe("ReceiversHooks", () => {
    beforeAll(() => server.listen());
    afterEach(() => server.resetHandlers());
    afterAll(() => server.close());
    beforeEach(() => {
        mockSessionContext.mockReturnValue(fakeSession);
    });
    test("useReceiversList", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useReceiversList("testOrg")
        );
        // enforce { requiresTrigger: true }
        expect(result.current.loading).toBeFalsy();
        // trigger call
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.loading).toBeFalsy();
        // received the array
        expect(result.current.data).toHaveLength(3);
    });
});
