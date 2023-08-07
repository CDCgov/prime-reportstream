import React, { Suspense, useCallback, useState } from "react";
import { Grid, GridContainer, Accordion } from "@trussworks/react-uswds";
import { AccordionItemProps } from "@trussworks/react-uswds/lib/components/Accordion/Accordion";
import { useParams } from "react-router-dom";
import { Helmet } from "react-helmet-async";

import HipaaNotice from "../../components/HipaaNotice";
import {
    SettingRevision,
    SettingRevisionParams,
    useSettingRevisionEndpointsQuery,
} from "../../network/api/Organizations/SettingRevisions";
import Spinner from "../../components/Spinner";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { jsonSortReplacer } from "../../utils/JsonSortReplacer";
import { formatDate, groupBy } from "../../utils/misc";
import { StaticCompare } from "../../components/StaticCompare";

type AccordionClickHandler = (
    key: string,
    itemClickedKey: string,
    data: SettingRevision,
) => void;

/**
 * Accordion components need data in specific format
 * See: https://trussworks.github.io/react-uswds/?path=/story/components-accordion--borderless
 * **/
const dataToAccordionItems = (props: {
    key: string; // used for React key and passed back to the onClickHandler
    selectedKey: string;
    onClickHandler: AccordionClickHandler;
    data: SettingRevision[];
}): AccordionItemProps[] => {
    const results: AccordionItemProps[] = [];
    if (props.data.length === 0) {
        return [];
    }

    // should come back sorted by name and version from server. Sort by name, then by version
    props.data.sort((a, b) =>
        a.name === b.name
            ? a.version - b.version
            : a.name.localeCompare(b.name),
    );

    // group the data to make generating the prop content logic easier to follow
    const grouped = groupBy(props.data, (each) => each.name);

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

/** this extends SettingRevisionsParams so it can be passed down without having to be recomposed, these include
 * the org and settingType cgi params */
interface MainComponentProps extends SettingRevisionParams {
    leftSelectedListItem: string;
    rightSelectedListItem: string;
    onClickHandler: AccordionClickHandler;
}

/**
 * Nest loading into a component that can spin so the bulk of the page loads while
 * the network request happens.
 */
const MainRevHistoryComponent = (props: MainComponentProps) => {
    const { data, isLoading, isError } =
        useSettingRevisionEndpointsQuery(props);
    const msg = isError
        ? "Failed to load data"
        : isLoading
        ? "Loading..."
        : "Data not found"; // should not be used because `!data` test below but useful for unit test debugging
    return (
        <Grid col={"fill"} className={"rs-maxwidth-vw80"}>
            <Grid row gap="md" className={"rs-accord-list-row"}>
                <Grid className={"rs-list-diffs-items"}>
                    {!data ? (
                        <div>{msg}</div>
                    ) : (
                        <Accordion
                            bordered={false}
                            items={dataToAccordionItems({
                                key: "left",
                                selectedKey: props.leftSelectedListItem,
                                onClickHandler: props.onClickHandler,
                                data,
                            })}
                        />
                    )}
                </Grid>
                <Grid className={"rs-list-diffs-items"}>
                    {!data ? null : (
                        <Accordion
                            bordered={false}
                            items={dataToAccordionItems({
                                key: "right",
                                selectedKey: props.rightSelectedListItem,
                                onClickHandler: props.onClickHandler,
                                data,
                            })}
                        />
                    )}
                </Grid>
            </Grid>
        </Grid>
    );
};

/** main page, not exported here because it should only be loaded via AdminRevHistoryWithAuth() **/
const AdminRevHistory = () => {
    const { org, settingType } = useParams<SettingRevisionParams>(); // props past to page via the route/url path args
    const [leftJson, setLeftJson] = useState("");
    const [rightJson, setRightJson] = useState("");
    // used to highlight which item is selected.
    const [leftSelectedListItem, setLeftSelectedListItem] = useState("");
    const [rightSelectedListItem, setRightSelectedListItem] = useState("");
    const [leftItem, setLeftItem] = useState<SettingRevision | null>(null);
    const [rightItem, setRightItem] = useState<SettingRevision | null>(null);

    const onClickHandler: AccordionClickHandler = useCallback(
        (key: string, itemClickedKey: string, data: SettingRevision) => {
            const normalizeJson = (jsonStr: string): string =>
                JSON.stringify(JSON.parse(jsonStr), jsonSortReplacer, 2);
            const prettyJson = normalizeJson(data.settingJson);

            switch (key) {
                case "left":
                    setLeftSelectedListItem(itemClickedKey);
                    setLeftJson(prettyJson);
                    setLeftItem(data);
                    break;
                case "right":
                    setRightSelectedListItem(itemClickedKey);
                    setRightJson(prettyJson);
                    setRightItem(data);
                    break;
            }
        },
        [],
    );

    return (
        <>
            <Helmet>
                <title>Revision History</title>
            </Helmet>

            <section className="grid-container margin-top-0">
                <h4>
                    Settings Revision History for "{org}" {settingType}
                </h4>
                <section className="margin-bottom-5">
                    Select different versions from each list to compare.
                </section>

                <GridContainer
                    className={"rs-revision-history rs-maxwidth-vw80"}
                >
                    <Grid row className={"rs-list-diffs-container"}>
                        <Suspense fallback={<Spinner />}>
                            <MainRevHistoryComponent
                                org={org || ""}
                                settingType={settingType || "organization"}
                                leftSelectedListItem={leftSelectedListItem}
                                rightSelectedListItem={rightSelectedListItem}
                                onClickHandler={onClickHandler}
                            />
                        </Suspense>
                    </Grid>
                    <Grid row>
                        <Grid col={"fill"}>
                            <StaticCompare
                                leftText={leftJson}
                                rightText={rightJson}
                                jsonDiffMode={true}
                            />
                        </Grid>
                    </Grid>
                    <Grid row>
                        <Grid col={"fill"}>
                            <Grid row gap="md">
                                <Grid data-testid={"meta-left-data"}>
                                    Flags: <br />
                                    {!leftItem ? null : (
                                        <>
                                            isDeleted:{" "}
                                            {String(leftItem.isDeleted)}
                                            <br /> isActive:{" "}
                                            {String(leftItem.isActive)}
                                        </>
                                    )}
                                </Grid>
                            </Grid>
                        </Grid>
                        <Grid col={"fill"}>
                            <Grid row gap="md">
                                <Grid data-testid={"meta-right-data"}>
                                    Flags: <br />
                                    {!rightItem ? null : (
                                        <>
                                            isDeleted:{" "}
                                            {String(rightItem.isDeleted)}
                                            <br /> isActive:{" "}
                                            {String(rightItem.isActive)}
                                        </>
                                    )}
                                </Grid>
                            </Grid>
                        </Grid>
                    </Grid>
                </GridContainer>
            </section>
            <HipaaNotice />
        </>
    );
};

/** required for React Url Path **/
export function AdminRevHistoryWithAuth() {
    return (
        <AuthElement
            element={<AdminRevHistory />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}

export const _exportForTesting = {
    dataToAccordionItems,
    AdminRevHistory,
    MainRevHistoryComponent,
};
