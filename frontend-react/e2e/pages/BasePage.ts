import appInsightsConfig from "../mocks/appInsightsConfig.json" assert { type: "json" };
import { Locator, Page, Request, Response, Route, TestArgs } from "../test";

export type RouteHandlers = Record<string, Parameters<Page["route"]>[1]>;
export type MockRouteCache = Record<string, RouteFulfillOptions>;
export type GotoOptions = Parameters<Page["goto"]>[1];

export interface BasePageProps {
    url: string;
    title: string;
    heading?: Locator;
}

export type RouteFulfillOptions = Exclude<
    Parameters<Route["fulfill"]>[0],
    undefined
> & { isMock?: boolean };
export type RouteFulfillOptionsFn = (
    request: Request,
) => Promise<RouteFulfillOptions> | RouteFulfillOptions;
export type RouteHandlerFn = (route: Route, request: Request) => Promise<void>;
export type RouteHandlerFulfillOptions =
    | RouteFulfillOptions
    | RouteFulfillOptionsFn;
export type RouteHandlerEntry = [
    url: string,
    fulfillOptions: RouteHandlerFulfillOptions,
];

export interface GotoRouteHandlerOptions {
    mock?: RouteHandlers;
    handlers?: RouteHandlers;
    mockError?: boolean | RouteFulfillOptions;
}

export type BasePageTestArgs = TestArgs<"page" | "storageState">;

export abstract class BasePage {
    readonly page: Page;
    readonly url: string;
    readonly title: string;
    readonly testArgs: BasePageTestArgs;
    readonly routeHandlers: Map<string, Parameters<Page["route"]>[1]>;
    protected _mockError?: RouteHandlerFulfillOptions;
    protected _mockRouteCache: MockRouteCache;

    readonly heading: Locator;
    readonly footer: Locator;

    constructor(
        { url, title, heading }: BasePageProps,
        testArgs: BasePageTestArgs,
    ) {
        this.page = testArgs.page;
        this.url = url;
        this.title = title;
        this.testArgs = testArgs;
        this.routeHandlers = new Map();
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

    set mockError(
        err: boolean | number | RouteHandlerFulfillOptions | undefined,
    ) {
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
    get isErrorExpected() {
        return !!this.mockError;
    }

    async reload() {
        await this.route();

        return await this.handlePageLoad(this.page.reload());
    }

    async goto(opts?: GotoOptions) {
        await this.route();

        return await this.handlePageLoad(
            this.page.goto(this.url, {
                waitUntil: "domcontentloaded",
                ...opts,
            }),
        );
    }

    /**
     * Override this method to add additional route handlers.
     */
    resetRouteHandler() {
        this.routeHandlers.clear();
        this.addDefaultRouteHandlers();
    }

    /**
     * Route handler object is reset before routing (to allow for tests
     * to change conditions on the fly and then initiate a navigation
     * to see the new conditions).
     */
    async route() {
        this.resetRouteHandler();

        for (const [url, handler] of this.routeHandlers.entries()) {
            await this.page.route(url, handler);
        }
    }

    /**
     * Takes the promise of a page navigation action (ex: goto, reload, etc.). Override
     * this method if you need to set up access to network request responses before
     * awaiting the navigation.
     */
    async handlePageLoad(res: Promise<Response | null>) {
        return await res;
    }

    /**
     * Adds additional logic check to ensure mock handlers do not run when explicitly
     * disabled.
     */
    addRouteHandlers(items: RouteHandlerEntry[]) {
        for (const [url, _fulfillOptions] of items) {
            const handler: RouteHandlerFn = async (route, request) => {
                const { isMock, ...fulfillOptions } =
                    typeof _fulfillOptions === "function"
                        ? await _fulfillOptions(request)
                        : _fulfillOptions;

                if (isMock && !this.isMocked)
                    throw new Error("Mocks are disabled");

                return await route.fulfill(fulfillOptions);
            };
            this.routeHandlers.set(url, handler);
        }
    }

    /**
     * Adds additional logic for mock error overrides and caching.
     */
    addMockRouteHandlers(items: RouteHandlerEntry[]) {
        return this.addRouteHandlers(
            items.map(([url, _fulfillOptions]) => {
                if (typeof _fulfillOptions === "object") {
                    return [
                        url,
                        {
                            isMock: true,
                            ..._fulfillOptions,
                        },
                    ];
                }

                const fn: RouteFulfillOptionsFn = async (request) => {
                    const fulfillOptions =
                        typeof _fulfillOptions === "function"
                            ? await _fulfillOptions(request)
                            : _fulfillOptions;
                    const mockErrorFulfillOptions =
                        typeof this.mockError === "function"
                            ? await this.mockError(request)
                            : this.mockError;
                    const mockCacheFulfillOptions =
                        this.getMockCacheFulfillOptions(url, fulfillOptions);
                    const mockOverrideFulfillOptions =
                        mockErrorFulfillOptions ?? mockCacheFulfillOptions;

                    return {
                        isMock: true,
                        ...mockOverrideFulfillOptions,
                    };
                };
                return [url, fn];
            }),
        );
    }

    /**
     * Add misc network requests that we want to prevent for tests here (NOT API MOCKS).
     */
    addDefaultRouteHandlers() {
        return this.addRouteHandlers([
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
    getMockCacheFulfillOptions(
        url: string,
        fulfillOptions: RouteFulfillOptions,
    ) {
        const cache = this._mockRouteCache[url];
        if (!cache) {
            this._mockRouteCache[url] = fulfillOptions;
            return fulfillOptions;
        }

        return cache;
    }
}
