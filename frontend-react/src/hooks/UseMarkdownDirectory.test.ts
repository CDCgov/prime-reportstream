import { renderHook } from "@testing-library/react-hooks";
import { PathParams, rest, RestRequest } from "msw";
import { setupServer } from "msw/node";

import useMarkdownDirectory from "./UseMarkdownDirectory";

interface MockParams extends PathParams {
    fileName: string;
}
const testFile = (file: string) => `testFile: ${file}`;
const mockNetworkHandlers = [
    // Handles a POST /login request
    rest.get<RestRequest<any, MockParams>>(
        "/testDir/:fileName",
        (req, res, ctx) => {
            const { fileName } = req.params;
            //@ts-ignore
            return res(ctx.text(testFile(fileName)));
        }
    ),
];
const mockNetworkServer = setupServer(...mockNetworkHandlers);

beforeAll(() => mockNetworkServer.listen());
afterEach(() => mockNetworkServer.resetHandlers());
afterAll(() => mockNetworkServer.close());

describe("UseMarkdownDirectory", () => {
    test("renders with params", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useMarkdownDirectory({
                fromDir: "/testDir",
                files: ["testFile.md", "testFile2.md", "realFileOmg.md"],
            })
        );
        await waitForNextUpdate();
        expect(result.current.mdFiles.length).toEqual(3);
        expect(result.current.mdFiles[0]).toEqual("testFile: testFile.md");
        expect(result.current.mdFiles[1]).toEqual("testFile: testFile2.md");
        expect(result.current.mdFiles[2]).toEqual("testFile: realFileOmg.md");
    });
});
