import { renderHook } from "@testing-library/react-hooks";

import {
    messageTrackerServer,
    MOCK_MESSAGE_SENDER_DATA,
} from "../../../__mocks__/MessageTrackerMockServer";
import { QueryWrapper } from "../../../utils/CustomRenderUtils";

import { useMessageSearch } from "./MessageTrackerHooks";

describe("useMessageSearch", () => {
    beforeAll(() => messageTrackerServer.listen());
    afterEach(() => messageTrackerServer.resetHandlers());
    afterAll(() => messageTrackerServer.close());

    test("returns expected data values when fetching messages", async () => {
        const { result } = renderHook(() => useMessageSearch(), {
            wrapper: QueryWrapper(),
        });
        const messages = await result.current.search("alaska1");
        expect(messages.length).toEqual(3);
        expect(messages[0].reportId).toEqual(
            MOCK_MESSAGE_SENDER_DATA[0].reportId
        );
        expect(messages[1].reportId).toEqual(
            MOCK_MESSAGE_SENDER_DATA[1].reportId
        );
        expect(messages[2].reportId).toEqual(
            MOCK_MESSAGE_SENDER_DATA[2].reportId
        );
    });
});
