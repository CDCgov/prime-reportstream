import { SideNav } from "@trussworks/react-uswds";
import {
    NavLink,
    Redirect,
    Route,
    Switch,
    useRouteMatch,
} from "react-router-dom";

import { CODES, ErrorPage } from "../../error/ErrorPage";

import { PhdOverview } from "./Overview";
import { ELRChecklist } from "./ElrChecklist";
import { DataDownloadGuide } from "./DataDownloadGuide";

export const GettingStartedPublicHealthDepartments = () => {
    let { path, url } = useRouteMatch();

    var itemsMenu = [
        <NavLink
            to={`${url}/overview`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Overview
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
    ];

    return (
        <>
            <section className="border-bottom border-base-lighter margin-bottom-6">
                <div className="grid-container">
                    <div className="grid-row grid-gap">
                        <div className="tablet:grid-col-12 margin-bottom-05">
                            <h1 className=" text-ink mobile:padding-top-0">
                                <span className="text-base">
                                    Getting started
                                </span>
                                <br /> Public health departments
                            </h1>
                        </div>
                    </div>
                </div>
            </section>
            <section className="grid-container margin-bottom-5">
                <div className="grid-row grid-gap">
                    <div className="tablet:grid-col-4 margin-bottom-6">
                        <SideNav items={itemsMenu} />
                    </div>
                    <div className="tablet:grid-col-8 usa-prose">
                        <Switch>
                            {/* Handles anyone going to /getting-started without extension */}
                            <Route exact path={path}>
                                <Redirect push to={`${path}/overview`} />
                            </Route>
                            <Route
                                path={`${path}/overview`}
                                component={PhdOverview}
                            />
                            <Route
                                path={`${path}/elr-checklist`}
                                component={ELRChecklist}
                            />
                            <Route
                                path={`${path}/data-download-guide`}
                                component={DataDownloadGuide}
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
