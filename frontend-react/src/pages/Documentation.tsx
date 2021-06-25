import { SideNav } from "@trussworks/react-uswds";
import { NavLink, Route, Switch, useRouteMatch } from "react-router-dom";
import { About } from "./documentation/About";
import { ELRIntegration } from "./documentation/ELRIntegration";
import { SecurityPractices } from "./documentation/SecurityPractices";
import { WebReceiverGuide } from "./documentation/WebReceiverGuide";


export const Documentation = () => {
  let { path, url } = useRouteMatch();

  var itemsMenu = [
    <NavLink to={`${url}/about`} activeClassName="usa-current" className="usa-nav__link">About</NavLink>,
    <NavLink to={`${url}/elr`} activeClassName="usa-current" className="usa-nav__link">ELR integration guide</NavLink>,
    <NavLink to={`${url}/wrg`} activeClassName="usa-current" className="usa-nav__link">Web receiver guide</NavLink>,
    <NavLink to={`${url}/security`} activeClassName="usa-current" className="usa-nav__link">Security practices</NavLink>,      
  ];

  return (
    <section className="grid-container margin-bottom-5">
      <div className="grid-row grid-gap">
        <div className="tablet:grid-col-3">
        <SideNav items={itemsMenu} />
        </div>
        <div className="tablet:grid-col-9 usa-prose rs-documentation">
        <Switch>
          <Route path={path} exact component={About} />
          <Route path={`${path}/about`} component={About} />
          <Route path={`${path}/elr`} component={ELRIntegration} />
          <Route path={`${path}/wrg`} component={WebReceiverGuide} />
          <Route path={`${path}/security`} component={SecurityPractices} />
        </Switch>
        </div>
      </div>
    </section>
  );
};
