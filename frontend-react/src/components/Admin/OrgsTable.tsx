import React, { useState } from "react";
import { useResource } from "rest-hooks";
import {
    Button,
    ButtonGroup,
    Label,
    Table,
    TextInput,
} from "@trussworks/react-uswds";
import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet";

import OrgSettingsResource from "../../resources/OrgSettingsResource";
import { useSessionContext } from "../../contexts/SessionContext";
import {
    MembershipActionType,
    MemberType,
    MembershipSettings,
} from "../../hooks/UseOktaMemberships";
import { USNavLink } from "../USLink";

export function OrgsTable() {
    const orgs: OrgSettingsResource[] = useResource(
        OrgSettingsResource.list(),
        {}
    ).sort((a, b) => a.name.localeCompare(b.name));
    const [filter, setFilter] = useState("");
    const navigate = useNavigate();
    const { activeMembership, dispatch } = useSessionContext();
    const currentOrg = activeMembership?.parsedName;

    const handleSelectOrgClick = (orgName: string) => {
        const { service, memberType } = activeMembership || {};

        let payload: Partial<MembershipSettings> = {
            parsedName: orgName,
        };
        if (
            memberType === MemberType.SENDER ||
            memberType === MemberType.PRIME_ADMIN
        ) {
            payload.service = service || "default";
        }
        dispatch({
            type: MembershipActionType.ADMIN_OVERRIDE,
            payload,
        });
    };

    const handleEditOrgClick = (orgName: string) => {
        // editing... maybe we should keep current org in sync? Switch to the "safe org"?
        // updateOrganization(orgName);
        navigate(`/admin/orgsettings/org/${orgName}`);
    };

    const saveListToCSVFile = () => {
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
                    ].join(`","`),
                    `"`,
                ].join("")
            )
            .join(`\n`); // join result of .map() lines
        // Note that this csv previously included a `Created` column with a createdAt
        // date taken from organization metadata. Currently this metadata is not being returned
        // in the API call for organizations, so we have removed the created column. It
        // should be added back whenever this API handler is adjusted to send back metadata - DWS
        const csvheader = `Name,Description,Jurisdiction,State,County\n`;
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
                    <USNavLink
                        href={"/admin/new/org"}
                        className="usa-button flex-align-self-end height-5"
                    >
                        Create New Organization
                    </USNavLink>
                    <Button
                        key={`savelist`}
                        onClick={() => saveListToCSVFile()}
                        type="button"
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
