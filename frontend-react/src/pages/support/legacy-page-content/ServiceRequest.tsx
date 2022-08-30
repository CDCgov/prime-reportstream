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
            <h2>Foo foo help here text</h2>

            <iframe
                title="ReportStream support form iFrame"
                className="form-support__smartsheet"
                width="100%"
                height="1200"
                frameBorder="0"
                src="https://app.smartsheetgov.com/b/form/52036af51e6e42fbb4e058423185b304"
            ></iframe>
        </>
    );
};
