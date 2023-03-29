import React, { useEffect, useState } from "react";
import { Form, Dropdown, Button, FormGroup } from "@trussworks/react-uswds";

import { useOrganizationSenders } from "../../hooks/UseOrganizationSenders";
import Spinner from "../Spinner";

export interface ManagePublicKeyChooseSenderProps {
    onSenderSelect: (sender: string) => void;
}

export default function ManagePublicKeyChooseSender({
    onSenderSelect,
}: ManagePublicKeyChooseSenderProps) {
    const { isLoading, senders } = useOrganizationSenders();
    const [selectedSender, setSelectedSender] = useState("");

    useEffect(() => {
        if (senders?.length === 1) {
            onSenderSelect(senders[0].name);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [senders]);

    function handleSubmit() {
        onSenderSelect(selectedSender);
    }

    return (
        <div data-testid="ManagePublicKeyChooseSender">
            {isLoading && <Spinner message="Loading..." />}
            {!isLoading && senders?.length > 1 && (
                <Form name="sender-select" onSubmit={handleSubmit}>
                    <FormGroup>
                        <Dropdown
                            id="senders-dropdown"
                            name="senders-dropdown"
                            onChange={(e) => {
                                setSelectedSender(e.target.value);
                            }}
                        >
                            <option value="">-Select-</option>
                            {senders?.map(({ name }) => (
                                <option key={name} value={name}>
                                    {name}
                                </option>
                            ))}
                        </Dropdown>
                        <Button
                            key="submit-sender"
                            type="submit"
                            disabled={!selectedSender}
                        >
                            Submit
                        </Button>
                    </FormGroup>
                </Form>
            )}
        </div>
    );
}
