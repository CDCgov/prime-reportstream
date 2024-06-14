import { BasePage, type BasePageTestArgs } from "../pages/BasePage";
import { test as baseTest, expect, Response } from "../test";

export interface MockPageFixtures {
    mockPage: MockPage;
}

class MockPage extends BasePage {
    data?: any;

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/",
                title: "",
            },
            testArgs,
        );
    }

    resetRouteHandler() {
        super.resetRouteHandler();

        if (this.isMocked) {
            this.addMockRouteHandlers([["/fake", { json: { foo: "bar" } }]]);
        } else {
            this.addRouteHandlers([["/fake", { json: { bar: "foo" } }]]);
        }

        this.addRouteHandlers([
            [
                "/",
                () => {
                    return {
                        body: "<body><h1>Fake</h1><script>fetch('/fake')</script></body>",
                        contentType: "text/html",
                    };
                },
            ],
        ]);
    }

    async handlePageLoad(res: Promise<Response | null>) {
        const mockRes = await this.page.waitForResponse("fake");
        this.data = await mockRes.json();

        return await super.handlePageLoad(res);
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
