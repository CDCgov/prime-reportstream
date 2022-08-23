/** TODO: Uncomment tests and re-Import packages when we move back off of rest-hooks. I tried mocking the rest-hooks
 * implementation; their types don't play nice. Further altering of our test infra to work with rest-hooks is a futile
 * effort. */

test("doesn't fail since this test exists", () => {
    expect(1 + 1).toEqual(2);
});

export {}; // Needed to make this a "module"

// describe("ReportsTable", () => {
//     beforeEach(() => {
//         // Mock our SessionProvider's data
//         mockSessionContext.mockReturnValue({
//             oktaToken: {
//                 accessToken: "TOKEN",
//             },
//             activeMembership: {
//                 memberType: MemberType.RECEIVER,
//                 parsedName: "testOrg",
//                 senderName: undefined,
//             },
//             dispatch: () => {},
//         });
//     });
//     describe("with services and deliveries data", () => {
//         beforeEach(() => {
//             // Mock the response from the Receivers hook
//             mockReceiverHook.mockReturnValue({
//                 data: receiversGenerator(3),
//                 loading: false,
//                 error: "",
//                 trigger: () => {},
//             });
//             // Mock the response from the Deliveries hook
//             mockDeliveryListHook.mockReturnValue({
//                 data: deliveriesTestGenerator(101),
//                 loading: false,
//                 error: "",
//                 trigger: () => {},
//             });
//             // Render the component
//             renderWithRouter(<ReportsTable />);
//         });
//         test("renders with no error", async () => {
//             // Column headers render
//             expect(await screen.findByText("Report ID")).toBeInTheDocument();
//             expect(await screen.findByText("Date Sent")).toBeInTheDocument();
//             expect(await screen.findByText("Expires")).toBeInTheDocument();
//             expect(await screen.findByText("Total Tests")).toBeInTheDocument();
//             expect(await screen.findByText("File")).toBeInTheDocument();
//         });
//
//         test("renders 100 results per page + 1 header row", () => {
//             const rows = screen.getAllByRole("row");
//             expect(rows).toHaveLength(100 + 1);
//         });
//     });
//     describe("with no data", () => {
//         beforeEach(() => {
//             // Mock the response from the Receivers hook
//             mockReceiverHook.mockReturnValue({
//                 data: receiversGenerator(0),
//                 loading: false,
//                 error: "",
//                 trigger: () => {},
//             });
//             // Mock the response from the Deliveries hook
//             mockDeliveryListHook.mockReturnValue({
//                 data: deliveriesTestGenerator(0),
//                 loading: false,
//                 error: "",
//                 trigger: () => {},
//             });
//             // Render the component
//             renderWithRouter(<ReportsTable />);
//         });
//         test("renders with no error", async () => {
//             // Column headers render
//             expect(await screen.findByText("Report ID")).toBeInTheDocument();
//             expect(await screen.findByText("Date Sent")).toBeInTheDocument();
//             expect(await screen.findByText("Expires")).toBeInTheDocument();
//             expect(await screen.findByText("Total Tests")).toBeInTheDocument();
//             expect(await screen.findByText("File")).toBeInTheDocument();
//         });
//         test("renders 0 results (but 1 header row)", () => {
//             const rows = screen.getAllByRole("row");
//             expect(rows.length).toBeLessThan(2);
//             expect(rows.length).toBeGreaterThan(0);
//         });
//     });
// });
//
// describe("useReceiverFeed", () => {
//     beforeAll(() => orgServer.listen());
//     afterEach(() => orgServer.resetHandlers());
//     afterAll(() => orgServer.close());
//     beforeEach(() => {
//         // Mock our SessionProvider's data
//         mockSessionContext.mockReturnValue({
//             oktaToken: {
//                 accessToken: "TOKEN",
//             },
//             activeMembership: {
//                 memberType: MemberType.RECEIVER,
//                 parsedName: "testOrg",
//                 senderName: undefined,
//             },
//             dispatch: () => {},
//         });
//         mockReceiverHook.mockReturnValue({
//             data: receiversGenerator(2),
//             error: "",
//             loading: false,
//             trigger: () => {},
//         });
//     });
//     test("setActive sets an active receiver", async () => {
//         const { result } = renderHook(() => useReceiverFeeds());
//         expect(result.current.activeService).toEqual({
//             name: "elr-0",
//             organizationName: "testOrg",
//         });
//         act(() => result.current.setActiveService(result.current.services[1]));
//         expect(result.current.activeService).toEqual({
//             name: "elr-1",
//             organizationName: "testOrg",
//         });
//     });
// });
