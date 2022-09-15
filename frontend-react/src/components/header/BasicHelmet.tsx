import { Helmet } from "react-helmet";

import config from "../../config";

const { APP_TITLE } = config;

export const BasicHelmet = ({ pageTitle }: { pageTitle: string }) => {
    return (
        <Helmet>
            <title>
                {pageTitle} | {APP_TITLE}
            </title>
        </Helmet>
    );
};
