import { Helmet } from "react-helmet";

import config from "../../config";

const { APP_TITLE } = config;

export const BasicHelmet = ({ pageTitle }: { pageTitle?: string }) => {
    const title = pageTitle ? `${pageTitle} | ${APP_TITLE}` : APP_TITLE;
    return (
        <Helmet>
            <title>{title}</title>
        </Helmet>
    );
};
