import { SideNav } from "@trussworks/react-uswds";
import {
    NavLink,
    useRouteMatch
} from "react-router-dom";

export const SupportContact = () => {
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
            to={`${url}/account-registration-guide`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            Account registration guide
        </NavLink>,
        <NavLink
            to={`${url}/csv-upload-guide`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            CSV upload guide
        </NavLink>,
        <NavLink
            to={`${url}/csv-schema`}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            CSV schema documentation
        </NavLink>,
    ];

    return (
        <>
            <section className="border-bottom border-base-lighter margin-bottom-6">
                <div className="grid-container">
                    <div className="grid-row grid-gap">
                        <div className="tablet:grid-col-12 margin-bottom-05">
                            <h1 className="text-ink">
                                
                                    Contact
                                
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
                        Contact
                    </div>
                </div>
            </section>
        </>
    );
};
