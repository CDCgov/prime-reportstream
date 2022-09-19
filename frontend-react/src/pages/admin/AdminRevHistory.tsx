import { Helmet } from "react-helmet";
import React, { Suspense, useCallback, useState } from "react";
import { Grid, GridContainer, Accordion } from "@trussworks/react-uswds";
import { AccordionItemProps } from "@trussworks/react-uswds/lib/components/Accordion/Accordion";
import { useParams } from "react-router-dom";
import { NetworkErrorBoundary, useResource } from "rest-hooks";

import HipaaNotice from "../../components/HipaaNotice";
import { SettingRevision } from "../../network/api/Organizations/SettingRevisions";
import OrganizationResource, {
    SettingRevisionParams,
} from "../../resources/OrgSettingRevisionsResource";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import { formatDate } from "../../utils/misc";
import { StaticCompare } from "../../components/StaticCompare";

/** Accordion components need data in specific format **/
const dataToAccordionItems = (props: {
    key: string; // used for React key and passed back to the onClickHandler
    selectedKey: string;
    onClickHandler: (
        key: string,
        itemClickedKey: string,
        data: SettingRevision
    ) => void;
    data: SettingRevision[];
}): AccordionItemProps[] => {
    const results: AccordionItemProps[] = [];
    if (props.data.length === 0) {
        return [];
    }

    // should come back sorted by name and version from server. Sort by name, then by version
    props.data.sort((a, b) =>
        a.name === b.name ? a.version - b.version : a.name.localeCompare(b.name)
    );

    // group the data to make generating the prop content logic easier to follow
    const grouped: { [key: string]: SettingRevision[] } = {};
    props.data.forEach((item) => {
        const name = item.name;
        if (!grouped[name]) {
            // key to array map. create initial entry for this group if needed
            grouped[name] = [];
        }
        grouped[name].push(item);
    });

    // turn each group into a html list and add to the content of the accordion
    for (let [key, settings] of Object.entries(grouped)) {
        const items = settings.map((eachSetting) => {
            const itemKey = `key-${eachSetting.id}`;
            const selectedCss =
                itemKey === props.selectedKey ? "rs-accord-row-selected" : "";
            return (
                <Grid
                    row
                    gap={"lg"}
                    className={`font-mono-2xs rs-cursor-arrow ${selectedCss}`}
                    key={itemKey}
                    onClick={() =>
                        props.onClickHandler(props.key, itemKey, eachSetting)
                    }
                >
                    <Grid col="auto" className={"font-mono-xs"}>
                        {eachSetting.version}
                    </Grid>
                    <Grid col="fill" className={"text-no-wrap"}>
                        {formatDate(eachSetting.createdAt)}
                    </Grid>
                    <Grid col="auto">{eachSetting.createdBy}</Grid>
                </Grid>
            );
        });
        // see AccordionItemProps
        results.push({
            id: `accord-item-${key}`,
            title: key,
            content: items,
            headingLevel: "h5",
            className: "rs-accord",
            expanded: true,
        });
    }
    return results;
};

/** this extends SettingRevisionsParams so it can be passed to down without having to be recomposed, these include
 * the org and settingtype cgi params */
interface MainComponentProps extends SettingRevisionParams {
    leftSelectedListItem: string;
    rightSelectedListItem: string;
    onClickHandler: (
        key: string,
        itemClickedKey: string,
        data: SettingRevision
    ) => void;
}

/**
 * Nest loading into a component that can spin so the bulk of the page loads while
 * the network request happens.
 */
const MainRevHistoryComponent = (props: MainComponentProps) => {
    const data: OrganizationResource[] = useResource(
        OrganizationResource.list(),
        props
    );

    return (
        <Grid col={"fill"} className={"rs-maxwidth-vw80"}>
            <Grid row gap="md" className={"rs-accord-list-row"}>
                <Grid className={"rs-list-diffs-items"}>
                    <Accordion
                        bordered={false}
                        items={dataToAccordionItems({
                            key: "left",
                            selectedKey: props.leftSelectedListItem,
                            onClickHandler: props.onClickHandler,
                            data,
                        })}
                    />
                </Grid>
                <Grid className={"rs-list-diffs-items"}>
                    <Accordion
                        bordered={false}
                        items={dataToAccordionItems({
                            key: "right",
                            selectedKey: props.rightSelectedListItem,
                            onClickHandler: props.onClickHandler,
                            data,
                        })}
                    />
                </Grid>
            </Grid>
        </Grid>
    );
};

function AdminRevHistory() {
    const { orgname, settingtype } = useParams<SettingRevisionParams>(); // props past to page via the route/url path args
    const [leftJson, setLeftJson] = useState("");
    const [rightJson, setRightJson] = useState("");
    // used to highlight which item is selected.
    const [leftSelectedListItem, setLeftSelectedListItem] = useState("");
    const [rightSelectedListItem, setRightSelectedListItem] = useState("");
    const onClickHandler = useCallback(
        (key: string, itemClickedKey: string, data: SettingRevision) => {
            const normalizeJson = (jsonStr: string): string =>
                JSON.stringify(JSON.parse(jsonStr), jsonSortReplacer, 2);
            const prettyJson = normalizeJson(data.settingJson);

            switch (key) {
                case "left":
                    setLeftSelectedListItem(itemClickedKey);
                    setLeftJson(prettyJson);
                    break;
                case "right":
                    setRightSelectedListItem(itemClickedKey);
                    setRightJson(prettyJson);
                    break;
            }
        },
        []
    );

    return (
        <>
            <Helmet>
                <title>Admin Revision History</title>
            </Helmet>

            <section className="grid-container margin-top-0">
                <h4>
                    Settings Revision History for "{orgname}" {settingtype}
                </h4>
                <section className="margin-bottom-5">
                    Select different versions from each list to compare.
                </section>

                <GridContainer
                    className={"rs-revision-history rs-maxwidth-vw80"}
                >
                    <Grid row className={"rs-list-diffs-container"}>
                        <Suspense fallback={<Spinner />}>
                            <NetworkErrorBoundary
                                fallbackComponent={() => (
                                    <ErrorPage type="message" />
                                )}
                            >
                                <MainRevHistoryComponent
                                    orgname={orgname || ""}
                                    settingtype={settingtype || "organization"}
                                    leftSelectedListItem={leftSelectedListItem}
                                    rightSelectedListItem={
                                        rightSelectedListItem
                                    }
                                    onClickHandler={onClickHandler}
                                />
                            </NetworkErrorBoundary>
                        </Suspense>
                    </Grid>
                    <Grid row>
                        <Grid col={"fill"}>
                            <StaticCompare
                                leftText={leftJson}
                                rightText={rightJson}
                            />
                        </Grid>
                    </Grid>
                </GridContainer>
            </section>
            <HipaaNotice />
        </>
    );
}

/** required for React Url Path **/
export function AdminRevHistoryWithAuth() {
    return (
        <AuthElement
            element={<AdminRevHistory />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
