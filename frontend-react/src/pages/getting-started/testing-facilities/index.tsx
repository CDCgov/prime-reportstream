import { SideNav } from "@trussworks/react-uswds";
import {
    NavLink,
    Redirect,
    Route,
    Switch,
    useRouteMatch,
} from "react-router-dom";

import { CODES, ErrorPage } from "../../error/ErrorPage";

import { FacilitiesOverview } from "./Overview";

export const GettingStartedTestingFacilities = () => {
    let { path, url } = useRouteMatch();

    var itemsMenu = [
        <NavLink
            to={`${url}/overview`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Overview
        </NavLink>,
    ];

    return (
        <>
        <section className="border-bottom border-base-lighter margin-bottom-6">
            <div className="grid-container">
                <div className="grid-row grid-gap">
                    <div className="tablet:grid-col-12 margin-bottom-2">
                        <h1 className="text-ink"><span className="text-base">Getting started</span><br /> Organizations and testing facilities</h1>
                    </div>
                </div>
            </div>
        </section>
        <section className="grid-container margin-bottom-5">
            <div className="grid-row grid-gap">
                <div className="tablet:grid-col-4 margin-bottom-6">
                    <SideNav items={itemsMenu} />
                </div>
                <div className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Switch>
                        {/* Handles anyone going to /getting-started without extension */}
                        <Route exact path={path}>
                            <Redirect push to={`${path}/overview`} />
                        </Route>
                        <Route
                            path={`${path}/overview`}
                            component={FacilitiesOverview}
                        />
                        {/* Handles any undefined route */}
                        <Route
                            render={() => (
                                <ErrorPage code={CODES.NOT_FOUND_404} />
                            )}
                        />
                    </Switch>
                </div>
            </div>
        </section>
        </>
    );
};
