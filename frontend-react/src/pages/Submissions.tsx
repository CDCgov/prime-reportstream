
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";
import { ErrorPage } from "./error/ErrorPage";

function Submissions() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Submissions | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
        </NetworkErrorBoundary>
    );
}

export default Submissions;