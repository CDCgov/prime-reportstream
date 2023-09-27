import { OktaAuthOptions, OktaAuth } from "@okta/okta-auth-js";
import { WidgetOptions } from "@okta/okta-signin-widget";

import config from "./config";

const { OKTA_URL, OKTA_CLIENT_ID, APP_ENV } = config;

const oktaAuthConfig: OktaAuthOptions = {
    issuer: `${OKTA_URL}/oauth2/default`,
    clientId: OKTA_CLIENT_ID as string,
    redirectUri: window.location.origin + "/login/callback",
    postLogoutRedirectUri: window.location.origin,
    responseMode: "fragment",
    tokenManager: {
        storage: APP_ENV === "test" ? "memory" : "localStorage",
    },
    scopes: ["openid", "email"],
    services: {
        autoRenew: false,
    },
};

const oktaSignInConfig: WidgetOptions = {
    logo: "/assets/cdc-logo.svg",
    language: "en",
    features: {
        registration: false, // Disable self-service registration flow
        rememberMe: false, // Setting to false will remove the checkbox to save username
        router: true, // Leave this set to true for the API demo
        // TODO: bring up enabling these once we have OktaPreview
        // securityImage: true, // helps prevent spoofing, may require CORS to allow it through.
        // mfaOnlyFlow: true, // government requires mfa
    },
    baseUrl: OKTA_URL as string,
    clientId: OKTA_CLIENT_ID as string,
    redirectUri: `${window.location.origin}/login/callback`,
    authParams: {
        issuer: `${OKTA_URL}/oauth2/default`,
    },
    scopes: ["openid", "email"],
    useClassicEngine: true,
    helpLinks: {
        help: "https://app.smartsheetgov.com/b/form/da894779659b45768079200609b3a599",
    },
    i18n: {
        // Overriding English properties
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
            "password.forgot.question.title":
                "Answer forgotten password challenge",
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
};

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);
OKTA_AUTH.start();

export { oktaAuthConfig, oktaSignInConfig, OKTA_AUTH };
