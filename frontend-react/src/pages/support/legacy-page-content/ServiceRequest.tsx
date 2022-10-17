import { Helmet } from "react-helmet";

export const ServiceRequest = () => {
    return (
        <>
            <Helmet>
                <title>
                    Service request | Support | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <h1>Service request</h1>
            <h2>Have an issue with an existing connection?</h2>
            <a
                target="_blank"
                href="https://app.smartsheetgov.com/b/form/ff33efa457be461c9893301d4c0ec12d"
                rel="noreferrer"
            >
                Use this form to open a ticket with our support team.
            </a>
        </>
    );
};
