import { Helmet } from "react-helmet";
import { useRef, useState } from "react";
import {
    Alert,
    Button,
    Grid,
    GridContainer,
    Label,
    TextInput,
} from "@trussworks/react-uswds";

import { showAlertNotification } from "../../components/AlertNotifications";

export enum FeatureFlagName {
    NUMBERED_PAGINATION = "numbered-pagination",
    VALUE_SETS_ADMIN = "value-sets",
}

/* feature flags are just and array of strings saved into a single localStorage variable */
const LOCALSTORAGE_KEY = "featureFlags";

function getSavedFeatureFlags(): string[] {
    const saved = window.localStorage.getItem(LOCALSTORAGE_KEY) || "";
    if (saved === "") {
        return [];
    }
    return saved.split("\t");
}

export function addFeatureFlag(flag: string) {
    const flagClean = flag.trim().toLowerCase();
    if (flagClean === "") {
        return;
    }

    // new Set() //   => remove duplication
    const set = new Set([...getSavedFeatureFlags(), flagClean]);
    const datastr = [...set.keys()].sort().join("\t");
    window.localStorage.setItem(LOCALSTORAGE_KEY, datastr);
}

function removeFeatureFlag(flag: string) {
    const flagLower = flag.trim().toLowerCase();
    const removed = getSavedFeatureFlags().filter((f) => f !== flagLower);
    window.localStorage.setItem(LOCALSTORAGE_KEY, removed.join("\t"));
}

// nothing outside of this file should need anything other than CheckFeatureFlag()...
// except for tests... so this is how we export but make it CLEAR it's just for testing
export const _exportForTesting = {
    LOCALSTORAGE_KEY,
    getSavedFeatureFlags,
    addFeatureFlag,
    removeFeatureFlag,
};

// PUBLIC functions
export function CheckFeatureFlag(feature: string): boolean {
    const lowercaseFeatureParam = feature.toLowerCase();
    const featuresEnabledStored = getSavedFeatureFlags();
    return featuresEnabledStored.includes(lowercaseFeatureParam);
}

export function FeatureFlagUIComponent() {
    const [allFeatures, setAllFeatures] = useState<string[]>(
        getSavedFeatureFlags()
    );
    const newFlagInputText = useRef<HTMLInputElement>(null);

    const deleteFlagClick = (flag: string) => {
        removeFeatureFlag(flag);
        setAllFeatures(getSavedFeatureFlags());
    };

    const addFlagClick = () => {
        const newFlag = newFlagInputText.current?.value || "";
        if (CheckFeatureFlag(newFlag)) {
            // already added.
            showAlertNotification(
                "info",
                `Item '${newFlag}' is already a feature flag.`
            );
            return;
        }
        addFeatureFlag(newFlagInputText.current?.value || "");
        setAllFeatures(getSavedFeatureFlags());
        // clear
        if (newFlagInputText.current?.value) {
            newFlagInputText.current.value = "";
        }
        showAlertNotification(
            "success",
            `Feature flag '${newFlag}' added. You will now see UI related to this feature.`
        );
    };

    return (
        <>
            <Helmet>
                <title>Feature Flags - {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-top-0">
                <h3>List of feature flags</h3>
                <GridContainer containerSize="desktop">
                    <Grid gap="lg" className="display-flex">
                        <Label
                            htmlFor="add-feature-flag"
                            className="text-bold padding-left-3 padding-right-3"
                        >
                            Add new feature string:
                        </Label>
                        <TextInput
                            type="text"
                            name="add-feature-flag"
                            id="add-feature-flag"
                            inputRef={newFlagInputText}
                        />
                        <Button
                            key="add-feature-flag"
                            type="button"
                            outline
                            size="small"
                            className="padding-bottom-1 padding-top-1"
                            onClick={() => addFlagClick()}
                        >
                            Add
                        </Button>
                    </Grid>

                    {allFeatures.map((flagname) => {
                        return (
                            <Grid gap="lg" className="margin-top-3">
                                <Alert type="success" slim noIcon className="">
                                    <b>{flagname}</b>
                                    <Button
                                        key={flagname}
                                        size="small"
                                        className="padding-bottom-1 padding-top-1 float-right"
                                        type="button"
                                        outline
                                        onClick={() =>
                                            deleteFlagClick(flagname)
                                        }
                                    >
                                        Delete
                                    </Button>
                                </Alert>
                            </Grid>
                        );
                    })}
                </GridContainer>
            </section>
        </>
    );
}
