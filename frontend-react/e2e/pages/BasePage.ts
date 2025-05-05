import { SideNavItem } from "../helpers/internal-links";
import { selectTestOrg } from "../helpers/utils";
import appInsightsConfig from "../mocks/appInsightsConfig.json" assert { type: "json" };
import { expect, Locator, Page, Request, Response, Route, TestArgs } from "../test";

export type RouteHandlers = Record<string, Parameters<Page["route"]>[1]>;
export type MockRouteCache = Record<string, RouteFulfillOptions>;
export type GotoOptions = Parameters<Page["goto"]>[1];

export interface BasePageProps {
    url: string;
    title: string;
    heading?: Locator;
}

export type RouteFulfillOptions = Exclude<Parameters<Route["fulfill"]>[0], undefined> & { isMock?: boolean };
export type RouteFulfillOptionsFn = (request: Request) => Promise<RouteFulfillOptions> | RouteFulfillOptions;
export type RouteHandlerFn = (route: Route, request: Request) => Promise<void>;
export type RouteHandlerFulfillOptions = RouteFulfillOptions | RouteFulfillOptionsFn;
export type RouteHandlerFulfillEntry = [url: string, fulfillOptions: RouteHandlerFulfillOptions];
export type ResponseHandlerEntry = [url: string, handler: (response: Response) => Promise<void> | void];
export type RouteHandlerEntry = [url: string, handler: RouteHandlerFn];

export interface GotoRouteHandlerOptions {
    mock?: RouteHandlers;
    handlers?: RouteHandlers;
    mockError?: boolean | RouteFulfillOptions;
}

export type BasePageTestArgs = TestArgs<"page" | "storageState"> & {
    isTestOrg?: boolean;
};

export abstract class BasePage {
    readonly page: Page;
    readonly url: string;
    readonly title: string;
    readonly testArgs: BasePageTestArgs;
    /**
     * Regular network routes with no special treatment in regards
     * to mocking.
     */
    readonly routeHandlers: Map<string, RouteHandlerFn>;
    /**
     * Mock network routes that will only be used when mocking
     * is allowed.
     */
    readonly mockRouteHandlers: Map<string, RouteHandlerFn>;
    /**
     * Handlers for network responses in FIFO order. WARNING: incorrect
     * ordering or additions of handlers for requests that never fire
     * will stall tests!
     */
    readonly responseHandlers: ResponseHandlerEntry[];
    protected _mockError?: RouteHandlerFulfillOptions;
    protected _mockRouteCache: MockRouteCache;

    readonly heading: Locator;
    readonly footer: Locator;

    constructor({ url, title, heading }: BasePageProps, testArgs: BasePageTestArgs) {
        this.page = testArgs.page;
        this.url = url;
        this.title = title;
        this.testArgs = testArgs;
        this.routeHandlers = new Map(this.createDefaultRouteHandlers());
        this.mockRouteHandlers = new Map();
        this.responseHandlers = [];
        this._mockRouteCache = {};
        this.heading = heading ?? this.page.locator("INVALID");
        this.footer = this.page.locator("footer");
    }

    get isMocked() {
        return !this.testArgs.isMockDisabled;
    }

    get mockError(): RouteHandlerFulfillOptions | undefined {
        return this._mockError;
    }

    set mockError(err: boolean | number | RouteHandlerFulfillOptions | undefined) {
        if (err == null || err === false) {
            this._mockError = undefined;
            return;
        }

        this._mockError =
            typeof err === "function" || typeof err === "object"
                ? err
                : {
                      status: typeof err === "number" ? err : 500,
                  };
    }

    /**
     * Override this method as needed to ensure tests do not hang waiting
     * for responses to network requests that will never occur.
     */
    get isPageLoadExpected() {
        return !this.mockError;
    }

    get isAdminSession() {
        return this.testArgs.storageState === "e2e/.auth/admin.json";
    }

    /**
     * Reloads the page.
     * Useful when setting a network error in a test.
     */
    async reload() {
        if (this.isAdminSession && this.testArgs.isTestOrg) {
            await this.selectTestOrg();
        }

        await this.route();

        return await this.handlePageLoad(this.page.reload());
    }

    async goto(opts?: GotoOptions) {
        if (this.isAdminSession && this.testArgs.isTestOrg) {
            await this.selectTestOrg();
        }

        await this.route();

        return await this.handlePageLoad(
            this.page.goto(this.url, {
                waitUntil: "domcontentloaded",
                ...opts,
            }),
        );
    }

    async testHeader(hasHeading = true) {
        await expect(this.page).toHaveTitle(this.title);
        if (hasHeading) {
            await expect(this.heading).toBeVisible();
        }
    }

    async testCard(card: { name: string }) {
        const cardHeader = this.page.locator(".usa-card__header", {
            hasText: card.name,
        });

        await expect(cardHeader).toBeVisible();
    }

    async testSidenav(navItems: SideNavItem[]) {
        const sideNav = this.page.getByTestId("sidenav");

        for (const navItem of navItems) {
            const link = sideNav.locator(`a`, { hasText: navItem.name });

            await expect(link).toBeVisible();
            await expect(link).toHaveAttribute("href", navItem.path);
        }
    }

    async testFooter() {
        await expect(this.page.locator("footer")).toBeAttached();
        await this.page.locator("footer").scrollIntoViewIfNeeded();
        await expect(this.page.locator("footer")).toBeInViewport();
        await expect(this.page.getByTestId("govBanner")).not.toBeInViewport();
        await this.page.evaluate(() => window.scrollTo(0, 0));
        await expect(this.page.getByTestId("govBanner")).toBeInViewport();
    }

    /**
     * Used to select the test org if logged-in user is Admin and the isTestOrg prop is set to true.
     * This is needed for smoke tests since they use live data.
     */
    async selectTestOrg() {
        await selectTestOrg(this.page);
    }

    async handlePageLoad(resP: Promise<Response | null>) {
        if (this.isPageLoadExpected) {
            await this.lifecycle_pageLoad();
            await this.handleNetworkResponses();
        }

        return await resP;
    }

    async handleNetworkResponses() {
        for (const [url, handler] of this.responseHandlers) {
            await handler(await this.page.waitForResponse(url));
        }
    }

    lifecycle_Route(): Promise<void> | void {
        return undefined as void;
    }

    async route() {
        await this.lifecycle_Route();
        const map = new Map([
            ...this.routeHandlers.entries(),
            // Ignore our mockRouteHandlers if mocking is disabled
            ...(this.isMocked ? this.mockRouteHandlers.entries() : []),
        ]);

        for (const [url, handler] of map) {
            await this.page.route(url, handler);
        }
    }

    lifecycle_pageLoad(): void | Promise<void> {
        return undefined as void;
    }

    /**
     * Helper function to push onto responseHandlers array.
     */
    addResponseHandlers(items: ResponseHandlerEntry[]) {
        this.responseHandlers.push(...items);
    }

    /**
     * Wraps route fulfill option objects or functions with additional logic for
     * mock error overrides and caching and adds to the mock route handler map.
     */
    addMockRouteHandlers(items: RouteHandlerFulfillEntry[]) {
        const wrapped = items.map(([url, _fulfillOptions]) => {
            const fn = async (request: Request) => {
                const fulfillOptions =
                    typeof _fulfillOptions === "function" ? await _fulfillOptions(request) : _fulfillOptions;
                const mockErrorFulfillOptions =
                    typeof this.mockError === "function" ? await this.mockError(request) : this.mockError;
                const mockCacheFulfillOptions = this.getMockCacheFulfillOptions(url, fulfillOptions);
                const mockOverrideFulfillOptions = mockErrorFulfillOptions ?? mockCacheFulfillOptions;

                return {
                    isMock: true,
                    ...mockOverrideFulfillOptions,
                };
            };
            return [url, fn] as [url: string, fn: typeof fn];
        });

        wrapped.forEach(([url, fn]) =>
            this.mockRouteHandlers.set(url, async (route, req) => route.fulfill(await fn(req))),
        );

        return wrapped;
    }

    /**
     * Helper function to convert RouteHandlerFulfillEntries to RouteHandlerEntries.
     */
    createRouteHandlers(items: RouteHandlerFulfillEntry[]): RouteHandlerEntry[] {
        return items.map(([url, _fulfill]) => {
            const handler = async (route: Route, request: Request) => {
                const fulfill = typeof _fulfill === "function" ? await _fulfill(request) : _fulfill;

                return route.fulfill(fulfill);
            };

            return [url, handler];
        });
    }

    /**
     * Add misc network requests that we want to prevent for tests here (NOT API MOCKS).
     */
    createDefaultRouteHandlers(): RouteHandlerEntry[] {
        return this.createRouteHandlers([
            // Azure Application Insights Tracking
            [
                "*.in.applicationinsights.azure.com/v2/track",
                (request) => {
                    const itemsCount = (request.postDataJSON() as any[]).length;
                    return {
                        json: {
                            itemsReceived: itemsCount,
                            itemsAccepted: itemsCount,
                            appId: null,
                            errors: [],
                        },
                    };
                },
            ],
            // Azure Application Insights Config
            [
                "https://js.monitor.azure.com/scripts/b/ai.config.*.cfg.json",
                {
                    json: appInsightsConfig,
                },
            ],
            // Google Analytics
            [
                "https://www.google-analytics.com/**",
                {
                    status: 204,
                },
            ],
        ]);
    }

    /**
     * Get or warm the cache for a particular mock URL's fulfillOptions. This
     * allows for dynamic options to persist across page reloads for consistency.
     */
    getMockCacheFulfillOptions(url: string, fulfillOptions: RouteFulfillOptions) {
        const cache = this._mockRouteCache[url];
        if (!cache) {
            this._mockRouteCache[url] = fulfillOptions;
            return fulfillOptions;
        }

        return cache;
    }
}
