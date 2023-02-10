const envVars = {
    APP_ENV: process.env.REACT_APP_ENV,
    OKTA_URL: process.env.REACT_APP_OKTA_URL,
    OKTA_CLIENT_ID: process.env.REACT_APP_OKTA_CLIENTID,
    RS_API_URL: process.env.REACT_APP_BACKEND_URL,
    APP_TITLE: process.env.REACT_APP_TITLE,
    CLIENT_ENV: process.env.REACT_APP_CLIENT_ENV,
};

const DEFAULT_FEATURE_FLAGS = process.env.REACT_APP_FEATURE_FLAGS
    ? process.env.REACT_APP_FEATURE_FLAGS.split(",")
    : [];

const config = {
    ...envVars,
    DEFAULT_FEATURE_FLAGS: DEFAULT_FEATURE_FLAGS as string[],
    IS_PREVIEW: envVars.OKTA_URL?.match(/oktapreview.com/) !== null,
    API_ROOT: `${envVars.RS_API_URL}/api`,
};

export default config;
