import React, { useState } from "react";
import { useResource } from "rest-hooks";
import {
    Button,
    ButtonGroup,
    Dropdown,
    Label,
    Table,
    TextInput,
} from "@trussworks/react-uswds";
import { NavLink, useHistory } from "react-router-dom";
import { Helmet } from "react-helmet";

import OrgSettingsResource from "../../resources/OrgSettingsResource";
import { getStoredOrg, setStoredOrg } from "../../contexts/SessionStorageTools";
import { useSessionContext } from "../../contexts/SessionContext";
import {
    MembershipActionType,
    MemberType,
} from "../../hooks/UseOktaMemberships";

export function OrgsTable() {
    const orgs: OrgSettingsResource[] = useResource(
        OrgSettingsResource.list(),
        {}
    ).sort((a, b) => a.name.localeCompare(b.name));
    const [filter, setFilter] = useState("");
    const currentOrg = getStoredOrg();
    const history = useHistory();
    const { memberships } = useSessionContext();

    const handleSelectOrgClick = (orgName: string) => {
        setStoredOrg(orgName);
        memberships.dispatch({
            type: MembershipActionType.ADMIN_OVERRIDE,
            payload: {
                parsedName: orgName,
            },
        });
    };

    const handleSetUserType = (type: MemberType) => {
        memberships.dispatch({
            type: MembershipActionType.ADMIN_OVERRIDE,
            payload: {
                memberType: type,
            },
        });
    };

    const handleEditOrgClick = (orgName: string) => {
        // editing... maybe we should keep current org in sync? Switch to the "safe org"?
        // updateOrganization(orgName);
        history.push(`/admin/orgsettings/org/${orgName}`);
    };

    const saveListToCSVFile = () => {
        // generate a lines that has "value","value","value"... each line is a
        const csvbody = orgs
            .filter((eachOrg) => eachOrg.filterMatch(filter))
            .map((eachOrg) =>
                [
                    `"`,
                    [
                        eachOrg.name,
                        eachOrg.description,
                        eachOrg.jurisdiction,
                        eachOrg.stateCode,
                        eachOrg.countyName,
                        new Date(eachOrg.meta.createdAt).toDateString(),
                    ].join(`","`),
                    `"`,
                ].join("")
            )
            .join(`\n`); // join result of .map() lines
        const csvheader = `Name,Description,Jurisdiction,State,County,Created\n`;
        const filecontent = [
            "data:text/csv;charset=utf-8,", // this makes it a csv file
            csvheader,
            csvbody,
        ].join("");
        window.open(encodeURI(filecontent), "prime-orgs.csv", "noopener");
    };

    return (
        <>
            <Helmet>
                <title>Admin-Organizations</title>
            </Helmet>
            <section
                id="orgsettings"
                className="grid-container margin-bottom-5"
            >
                <h2>Organizations ({orgs.length})</h2>
                <form autoComplete="off" className="grid-row">
                    <div className="flex-fill">
                        <Label
                            className="font-sans-xs usa-label"
                            htmlFor="input-filter"
                        >
                            Filter:
                        </Label>
                        <TextInput
                            id="input-filter"
                            name="input-filter"
                            type="text"
                            autoComplete="off"
                            aria-autocomplete="none"
                            autoFocus
                            onChange={(evt) => setFilter(evt.target.value)}
                        />
                    </div>
                    <div className="flex-fill margin-x-2">
                        <Label
                            className="font-sans-xs usa-label"
                            htmlFor="input-filter"
                        >
                            Mimic user type:
                        </Label>
                        <Dropdown
                            name="user-type-select"
                            defaultValue={memberships.state.active?.memberType}
                            className="rs-input"
                            onChange={(e) =>
                                handleSetUserType(e.target.value as MemberType)
                            }
                            id="user-type-select"
                        >
                            {Object.values(MemberType).map((type) => (
                                <option>{type}</option>
                            ))}
                        </Dropdown>
                    </div>
                    <NavLink
                        to={"/admin/new/org"}
                        className="usa-button flex-align-self-end height-5"
                    >
                        Create New Organization
                    </NavLink>
                    <Button
                        key={`savelist`}
                        onClick={() => saveListToCSVFile()}
                        type="button"
                        size="small"
                        className="usa-button usa-button--outline usa-button--small flex-align-self-end height-5"
                    >
                        Save List to CSV
                    </Button>
                </form>
                <Table
                    key="orgsettingstable"
                    aria-label="Organizations"
                    striped
                    fullWidth
                >
                    <thead>
                        <tr>
                            <th scope="col">Name</th>
                            <th scope="col">Description</th>
                            <th scope="col">Jurisdiction</th>
                            <th scope="col">State</th>
                            <th scope="col">County</th>
                            <th scope="col"> </th>
                        </tr>
                    </thead>
                    <tbody id="tBodyFac" className="font-mono-2xs">
                        {orgs
                            .filter((eachOrg) => eachOrg.filterMatch(filter))
                            .map((eachOrg) => (
                                <tr key={`sender-row-${eachOrg.name}`}>
                                    <td>
                                        <span
                                            className={
                                                eachOrg.name === currentOrg
                                                    ? "font-heading-sm text-bold"
                                                    : "font-heading-sm"
                                            }
                                        >
                                            {eachOrg.name}
                                        </span>
                                    </td>
                                    <td>{eachOrg?.description || "-"}</td>
                                    <td>{eachOrg.jurisdiction || ""}</td>
                                    <td>{eachOrg.stateCode || ""}</td>
                                    <td>{eachOrg.countyName || ""}</td>
                                    <td>
                                        <ButtonGroup type="segmented">
                                            <Button
                                                key={`${eachOrg.name}_select`}
                                                onClick={() =>
                                                    handleSelectOrgClick(
                                                        `${eachOrg.name}`
                                                    )
                                                }
                                                type="button"
                                                size="small"
                                                className="padding-1 usa-button--outline"
                                            >
                                                Set
                                            </Button>
                                            <Button
                                                key={`${eachOrg.name}_edit`}
                                                onClick={() =>
                                                    handleEditOrgClick(
                                                        `${eachOrg.name}`
                                                    )
                                                }
                                                type="button"
                                                size="small"
                                                className="padding-1 usa-button--outline"
                                            >
                                                Edit
                                            </Button>
                                        </ButtonGroup>
                                    </td>
                                </tr>
                            ))}
                    </tbody>
                </Table>
            </section>
        </>
    );
}
