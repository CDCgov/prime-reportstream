import { IConfig, IConfiguration, SeverityLevel } from "@microsoft/applicationinsights-web";
import { AccessToken, AuthState, IDToken, OktaAuthOptions, RefreshToken } from "@okta/okta-auth-js";
import { WidgetOptions } from "@okta/okta-signin-widget";
import type { Feature } from "@okta/okta-signin-widget";
import type { IIdleTimerProps } from "react-idle-timer";

import site from "../content/site.json";
import type { ConsoleLevel } from "../utils/rsConsole/rsConsole";

const envVars = {
    OKTA_URL: import.meta.env.VITE_OKTA_URL,
    OKTA_CLIENT_ID: import.meta.env.VITE_OKTA_CLIENTID,
    RS_API_URL: import.meta.env.VITE_BACKEND_URL,
    MODE: import.meta.env.MODE,
};

const DEFAULT_FEATURE_FLAGS = import.meta.env.VITE_FEATURE_FLAGS ? import.meta.env.VITE_FEATURE_FLAGS.split(",") : [];

const OKTA_ISSUER = `${envVars.OKTA_URL}/oauth2/default`;

const sharedOktaConfig = {
    issuer: OKTA_ISSUER,
    clientId: envVars.OKTA_CLIENT_ID as string,
    redirectUri: `${window.location.origin}/login/callback`,
};

const config = {
    ...envVars,
    DEFAULT_FEATURE_FLAGS: DEFAULT_FEATURE_FLAGS as string[],
    IS_PREVIEW: envVars.MODE !== "production",
    API_ROOT: `${envVars.RS_API_URL}/api`,
    APPLICATION_INSIGHTS: {
        connectionString: import.meta.env.VITE_APPLICATIONINSIGHTS_CONNECTION_STRING ?? "instrumentationKey=test",
        loggingLevelConsole: import.meta.env.NODE_ENV === "development" ? 2 : 0,
        disableFetchTracking: false,
        enableAutoRouteTracking: true,
        loggingLevelTelemetry: 2,
        maxBatchInterval: 0,
        disableAjaxTracking: false,
        autoTrackPageVisitTime: true,
        enableCorsCorrelation: true,
        enableRequestHeaderTracking: true,
        enableResponseHeaderTracking: true,
        disableTelemetry: !import.meta.env.VITE_APPLICATIONINSIGHTS_CONNECTION_STRING,
        excludeRequestFromAutoTrackingPatterns: ["google-analytics.com"],
    } as const satisfies IConfiguration & IConfig,
    RSCONSOLE: {
        // Debug ignored by default
        reportableLevels: ["assert", "error", "info", "trace", "warn"] as ConsoleLevel[],
        severityLevels: {
            info: SeverityLevel.Information,
            warn: SeverityLevel.Warning,
            error: SeverityLevel.Error,
            debug: SeverityLevel.Verbose,
            assert: SeverityLevel.Error,
            trace: SeverityLevel.Warning,
        } as Record<ConsoleLevel, SeverityLevel>,
    },
    IDLE_TIMERS: {
        timeout: import.meta.env.VITE_IDLE_TIMEOUT ?? 1000 * 60 * 15, // 15 minutes
        debounce: 500,
        crossTab: true,
        syncTimers: 200,
        leaderElection: true,
    } as IIdleTimerProps,
    OKTA_AUTH: {
        ...sharedOktaConfig,
        postLogoutRedirectUri: window.location.origin,
        responseMode: "fragment",
        tokenManager: {
            storage: "localStorage",
        },
        scopes: ["openid", "email"],
        services: {
            autoRenew: false,
        },
        async transformAuthState(oktaAuth, authState) {
            let finalAuthState: AuthState = structuredClone(authState);
            const tokens = [authState.accessToken, authState.idToken, authState.refreshToken].filter(Boolean) as (
                | AccessToken
                | IDToken
                | RefreshToken
            )[];
            // Prevent pulling incorrect token from a different okta environment
            if (
                tokens.find(
                    (t) =>
                        ("issuer" in t && t.issuer !== OKTA_ISSUER) || ("claims" in t && t.claims.iss !== OKTA_ISSUER),
                )
            ) {
                oktaAuth.clearStorage();
                finalAuthState = {
                    ...authState,
                    accessToken: undefined,
                    idToken: undefined,
                    refreshToken: undefined,
                    isAuthenticated: false,
                };
            }

            return Promise.resolve(finalAuthState);
        },
    } satisfies OktaAuthOptions,
    OKTA_WIDGET: {
        ...sharedOktaConfig,
        logo: "/assets/cdc-logo.svg",
        language: "en",
        features: {
            registration: false, // Disable self-service registration flow
            rememberMe: false, // Setting to false will remove the checkbox to save username
            router: false, // router enabled allows the widget to change the URL (/signin/*), which we don't want
            webauthn: true, // enable webauthn (yubi, passkey, etc.)
            //selfServiceUnlock: true,
            //emailRecovery: true,
            //callRecovery: true,
            //smsRecovery: true,
            showPasswordToggleOnSignInPage: true,
            //autoPush: true,
        } satisfies Partial<Record<Feature, boolean>>,
        useClassicEngine: false,
        helpLinks: {
            help: site.forms.contactUs.url,
        },
        i18n: {
            // Overriding English properties
            // List available at: node_modules/@okta/okta-signin-widget/dist/labels/properties/login.properties
            en: {
                help: "Request username or get other help through our service request form.",
                signin: "Sign in",
                forgotpassword: "Reset password",
                "primaryauth.title": "Sign in",
                "primaryauth.username.placeholder": "Username or email",
                "primaryauth.submit": "Sign in",
                "error.username.required":
                    "Please enter a username. Your username should be the email address you registered with. Check your activation email.",
                "password.reset": "Reset password",
                "password.forgot.question.title": "Answer forgotten password challenge",
                "password.forgot.question.submit": "Reset password",
                "password.forgot.emailSent.title": "Email sent",
                "password.forgot.emailSent.desc":
                    "We sent an email to {0}. If the email is associated with an account, you will receive instructions on resetting your password. ",
                "password.forgot.sendEmail": "Request password reset",
                "email.mfa.title": "Verify with email authentication",
                "enroll.choices.description":
                    "ReportStream requires multifactor authentication to add an additional layer of security when signing in to your Okta account",
                "enroll.choices.setup": "Set up",
                "error.auth.lockedOut":
                    "Your account is locked because of too many failed attempts. Check your email for next steps to unlock.",
                "errors.E0000119":
                    "Your account is locked because of too many failed attempts. Check your email for next steps to unlock.",
                "errors.E0000004":
                    "Unable to sign in.  Check your username and password. Your account will be locked after 5 failed attempts.",
                "account.unlock.email.or.username.placeholder": "Username or email",
                "account.unlock.email.or.username.tooltip": "Username or email",
            },
        },
    } satisfies WidgetOptions,
    PAGE_META: {
        defaults: {
            title: import.meta.env.VITE_TITLE,
            description: import.meta.env.VITE_DESCRIPTION,
            openGraph: {
                image: {
                    src: import.meta.env.VITE_OPENGRAPH_DEFAULT_IMAGE_SRC,
                    altText: import.meta.env.VITE_OPENGRAPH_DEFAULT_IMAGE_ALTTEXT,
                },
            },
        },
    },
} as const;

export type AppConfig = typeof config;

export default config;
