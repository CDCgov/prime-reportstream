import { BasePage, type BasePageTestArgs } from "../../pages/BasePage";
import { test as baseTest, expect } from "../../test";

export interface MockPageFixtures {
    mockPage: MockPage;
}

class MockPage extends BasePage {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    data?: any;

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/",
                title: "",
            },
            testArgs,
        );
        this.addResponseHandlers([
            [
                "fake",
                async (res) => {
                    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
                    this.data = await res.json();
                },
            ],
        ]);
    }

    lifecycle_Route() {
        if (this.isMocked) {
            this.addMockRouteHandlers([["/fake", { json: { foo: "bar" } }]]);
        } else {
            const [url, handler] = this.createRouteHandlers([["/fake", { json: { bar: "foo" } }]])[0];
            this.routeHandlers.set(url, handler);
        }

        const [url, handler] = this.createRouteHandlers([
            [
                "/",
                {
                    body: "<body><h1>Fake</h1><script>fetch('/fake')</script></body>",
                    contentType: "text/html",
                },
            ],
        ])[0];

        this.routeHandlers.set(url, handler);
    }
}

const test = baseTest.extend<MockPageFixtures>({
    mockPage: async (
        {
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
        },
        use,
    ) => {
        const page = new MockPage({
            page: _page,
            isMockDisabled,
            adminLogin,
            senderLogin,
            receiverLogin,
            storageState,
            frontendWarningsLogPath,
            isFrontendWarningsLog,
        });
        await page.goto();
        await use(page);
    },
});

test.describe("mocking", () => {
    test.describe("enabled", () => {
        test.use({ isMockDisabled: false });

        test("renders", async ({ mockPage }) => {
            await expect(mockPage.page.getByText("Fake")).toBeVisible();
        });
        test("returns mocked data", ({ mockPage }) => {
            expect(mockPage.data).toEqual({ foo: "bar" });
        });
    });

    test.describe("mocking disabled", () => {
        test.use({ isMockDisabled: true });

        test("renders", async ({ mockPage }) => {
            await expect(mockPage.page.getByText("Fake")).toBeVisible();
        });
        test("returns real data", ({ mockPage }) => {
            expect(mockPage.data).toEqual({ bar: "foo" });
        });
    });
});
