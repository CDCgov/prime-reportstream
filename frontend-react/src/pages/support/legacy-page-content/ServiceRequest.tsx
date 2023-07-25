import { Helmet } from "react-helmet-async";

import { USExtLink } from "../../../components/USLink";

export const ServiceRequest = () => {
    return (
        <>
            <Helmet>
                <title>
                    Service request | Support | {import.meta.env.VITE_TITLE}
                </title>
            </Helmet>
            <h1>Service request</h1>
            <h2>Have an issue with an existing connection?</h2>
            <USExtLink href="https://app.smartsheetgov.com/b/form/ff33efa457be461c9893301d4c0ec12d">
                Use this form to open a ticket with our support team.
            </USExtLink>
        </>
    );
};
