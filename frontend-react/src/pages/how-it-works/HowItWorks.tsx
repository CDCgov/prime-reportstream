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
import { ELRChecklist } from "./ElrChecklist";
import { GettingStarted } from "./GettingStarted";
import { WhereWereLive } from "./WhereWereLive";
import { WebReceiverGuide } from "./WebReceiverGuide";
import { SystemsAndSettings } from "./SystemsAndSettings";

export const HowItWorks = () => {
    let { path, url } = useRouteMatch();

    var itemsMenu = [
        <NavLink
            to={`${url}/getting-started`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Getting started
        </NavLink>,
        <NavLink
            to={`${url}/elr-checklist`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            ELR onboarding checklist
        </NavLink>,
        <NavLink
            to={`${url}/data-download-guide`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Data download website guide
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
        <section className="grid-container margin-bottom-5">
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
                        <Route
                            path={`${path}/getting-started`}
                            component={GettingStarted}
                        />
                        <Route
                            path={`${path}/elr-checklist`}
                            component={ELRChecklist}
                        />
                        <Route
                            path={`${path}/data-download-guide`}
                            component={WebReceiverGuide}
                        />
                        <Route
                            path={`${path}/where-were-live`}
                            component={WhereWereLive}
                        />
                        <Route
                            path={`${path}/systems-and-settings`}
                            component={SystemsAndSettings}
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
    );
};
