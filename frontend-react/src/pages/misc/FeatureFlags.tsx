import { Alert, Button, Grid, GridContainer, Label, TextInput } from "@trussworks/react-uswds";
import { useCallback, useRef } from "react";
import { Helmet } from "react-helmet-async";

import config from "../../config";
import { FeatureFlagActionType } from "../../contexts/FeatureFlag/FeatureFlagProvider";
import useFeatureFlags from "../../contexts/FeatureFlag/useFeatureFlags";
import { showToast } from "../../contexts/Toast";

const { DEFAULT_FEATURE_FLAGS } = config;

export enum FeatureFlagName {
    FOR_TEST = "for-tests-only",
}

export function FeatureFlagsPage() {
    const newFlagInputText = useRef<HTMLInputElement>(null);

    const { featureFlags, checkFlags, dispatch } = useFeatureFlags();

    const addFlagClick = useCallback(() => {
        const newFlag = newFlagInputText.current?.value ?? "";
        if (checkFlags(newFlag)) {
            // already added.
            showToast(`Item '${newFlag}' is already a feature flag.`, "info");
            return;
        }
        dispatch({
            type: FeatureFlagActionType.ADD,
            payload: newFlag,
        });
        // clear
        if (newFlagInputText.current?.value) {
            newFlagInputText.current.value = "";
        }
        showToast(`Feature flag '${newFlag}' added. You will now see UI related to this feature.`, "success");
    }, [newFlagInputText, checkFlags, dispatch]);
    const deleteFlagClick = useCallback(
        (flagname: string) => {
            dispatch({
                type: FeatureFlagActionType.REMOVE,
                payload: flagname,
            });
        },
        [dispatch],
    );

    return (
        <>
            <Helmet>
                <title>Feature flags - Admin</title>
            </Helmet>
            <section className="grid-container margin-top-0">
                <h3>List of feature flags</h3>
                <GridContainer containerSize="desktop">
                    <Grid gap="lg" className="display-flex">
                        <Label htmlFor="add-feature-flag" className="text-bold padding-left-3 padding-right-3">
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
                            className="padding-bottom-1 padding-top-1"
                            onClick={() => addFlagClick()}
                        >
                            Add
                        </Button>
                    </Grid>

                    {featureFlags.map((flagname) => {
                        return (
                            <Grid gap="lg" className="margin-top-3" key={`feature-flag-${flagname}`}>
                                <Alert headingLevel="h4" type="success" slim noIcon className="">
                                    <b>{flagname}</b>
                                    {!DEFAULT_FEATURE_FLAGS.includes(flagname) && (
                                        <Button
                                            key={flagname}
                                            className="padding-bottom-1 padding-top-1 float-right"
                                            type="button"
                                            outline
                                            onClick={() => deleteFlagClick(flagname)}
                                        >
                                            Delete
                                        </Button>
                                    )}
                                </Alert>
                            </Grid>
                        );
                    })}
                </GridContainer>
            </section>
        </>
    );
}

export default FeatureFlagsPage;
