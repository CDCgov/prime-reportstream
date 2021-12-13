
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";
import { ErrorPage } from "./error/ErrorPage";
import { useOktaAuth } from "@okta/okta-react";
import { GLOBAL_STORAGE_KEYS } from "../components/GlobalContextProvider";

function Submissions() {
    const { authState } = useOktaAuth();
    const organization = localStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG);

    let submissions:[]= [];
    fetch(
        `${process.env.REACT_APP_BACKEND_URL}/api/history/${organization}/submissions`,
        {
            headers: {
                Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
            },
        }
    )
    .then(res => res.json())
    .then(submissionsJson => {
        submissions = submissionsJson;
    })

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Submissions | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            {submissions}
        </NetworkErrorBoundary>
    );
}

export default Submissions;