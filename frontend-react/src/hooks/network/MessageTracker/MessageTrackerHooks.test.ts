import { renderHook } from "@testing-library/react-hooks";

import { messageTrackerServer } from "../../../__mocks__/MessageTrackerMockServer";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../UseOktaMemberships";
import { QueryWrapper } from "../../../utils/CustomRenderUtils";

import { useMessageDetails } from "./MessageTrackerHooks";

describe("MessageTrackerHooks", () => {
    beforeAll(() => messageTrackerServer.listen());
    afterEach(() => messageTrackerServer.resetHandlers());
    afterAll(() => messageTrackerServer.close());

    test("useMessageDetail", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
            },
            dispatch: () => {},
            initialized: true,
        });

        const { result, waitForNextUpdate } = renderHook(
            () => useMessageDetails("11"),
            { wrapper: QueryWrapper() }
        );
        await waitForNextUpdate();
        expect(result.current.messageDetails?.id).toEqual(11);
    });
});
