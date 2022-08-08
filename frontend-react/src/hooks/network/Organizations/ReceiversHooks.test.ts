import { act, renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MembershipController, MemberType } from "../../UseOktaMemberships";
import { SessionController } from "../../UseSessionStorage";
import { RSReceiver } from "../../../network/api/Organizations/Receivers";

import { useReceiversList } from "./ReceiversHooks";

const fakeSession = {
    oktaToken: {
        accessToken: "TOKEN",
    },
    memberships: {
        state: {
            active: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
                senderName: undefined,
            },
        },
    } as MembershipController,
    store: {} as SessionController, // TS yells about removing this because of types
};
const handlers = [
    rest.get(
        "https://test.prime.cdc.gov/api/settings/organizations/testOrg/receivers",
        (req, res, ctx) => {
            const array = [new RSReceiver(), new RSReceiver()];
            return res(ctx.status(200), ctx.json(array));
        }
    ),
];

const server = setupServer(...handlers);

describe("ReceiversHooks", () => {
    beforeAll(() => server.listen());
    afterEach(() => server.resetHandlers());
    afterAll(() => server.close());
    test("useReceiversList", async () => {
        mockSessionContext.mockReturnValue(fakeSession);
        const { result, waitForNextUpdate } = renderHook(() =>
            useReceiversList("testOrg")
        );
        expect(result.current.loading).toBeFalsy();
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.loading).toBeFalsy();
        expect(result.current.data).toHaveLength(2);
    });
});
