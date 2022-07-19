import { SideNav } from "@trussworks/react-uswds";
import {
    NavLink,
    Redirect,
    Route,
    Switch,
    useRouteMatch,
} from "react-router-dom";

import { CODES, ErrorPage } from "../error/ErrorPage";

import { SecurityPractices } from "./SecurityPractices";
import { WhereWereLive } from "./WhereWereLive";
import { SystemAndSettings } from "./SystemAndSettings";
import { About } from "./About";

export const HowItWorks = () => {
    let { path, url } = useRouteMatch();

    var itemsMenu = [
        <NavLink
            to={`${url}/about`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            About
        </NavLink>,
        <NavLink
            to={`${url}/where-were-live`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Where we're live
        </NavLink>,
        <NavLink
            to={`${url}/systems-and-settings`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Systems and settings
        </NavLink>,
        <NavLink
            to={`${url}/security-practices`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Security practices
        </NavLink>,
    ];

    return (
        <>
            <section className="grid-container tablet:margin-top-6 margin-bottom-5">
                <div className="grid-row grid-gap">
                    <div className="tablet:grid-col-4 margin-bottom-6">
                        <SideNav items={itemsMenu} />
                    </div>
                    <div className="tablet:grid-col-8 usa-prose rs-documentation">
                        <Switch>
                            {/* Handles anyone going to /how-it-works without extension */}
                            <Route exact path={path}>
                                <Redirect push to={`${path}/getting-started`} />
                            </Route>
                            <Route path={`${path}/about`} component={About} />
                            <Route
                                path={`${path}/where-were-live`}
                                component={WhereWereLive}
                            />
                            <Route
                                path={`${path}/systems-and-settings`}
                                component={SystemAndSettings}
                            />
                            <Route
                                path={`${path}/security-practices`}
                                component={SecurityPractices}
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
