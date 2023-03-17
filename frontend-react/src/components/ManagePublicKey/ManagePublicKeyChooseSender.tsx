import React, { useState } from "react";
import { Form, Dropdown, Button, FormGroup } from "@trussworks/react-uswds";

import { UseOrganizationSenders } from "../../hooks/UseOrganizationSenders";
import Spinner from "../Spinner";

type Props = {
    onSenderSelect: any; //What type should this be???
};

export function ManagePublicKeyChooseSender({ onSenderSelect }: Props) {
    const { isLoading, senders } = UseOrganizationSenders();
    const [selectedSender, setSelectedSender] = useState(
        senders?.length === 1 ? senders[0].name : ""
    );

    if (senders?.length === 1) {
        onSenderSelect(selectedSender);
    }

    function handleSubmit() {
        onSenderSelect(selectedSender);
    }

    return (
        <>
            {isLoading && <Spinner message="Processing file..." />}
            {!isLoading && senders && (
                <Form name="senderSelect" onSubmit={handleSubmit}>
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
                            outline
                            className="padding-bottom-1 padding-top-1"
                            disabled={selectedSender === ""}
                        >
                            Submit
                        </Button>
                    </FormGroup>
                </Form>
            )}
        </>
    );
}
