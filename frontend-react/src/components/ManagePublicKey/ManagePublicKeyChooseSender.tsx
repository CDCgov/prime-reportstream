import { Button, Form, FormGroup, Select } from "@trussworks/react-uswds";
import { FormEvent, useState } from "react";

import { RSSender } from "../../config/endpoints/settings";

export interface ManagePublicKeyChooseSenderProps {
    senders: RSSender[];
    onSenderSelect: (sender: string, hasBack: boolean) => void;
}

export default function ManagePublicKeyChooseSender({ senders, onSenderSelect }: ManagePublicKeyChooseSenderProps) {
    const [selectedSender, setSelectedSender] = useState("");

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        onSenderSelect(selectedSender, true);
    }

    return (
        <div data-testid="ManagePublicKeyChooseSender">
            {senders?.length > 1 && (
                <Form name="sender-select" onSubmit={handleSubmit}>
                    <FormGroup>
                        <Select
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
                        </Select>
                        <Button key="submit-sender" type="submit" disabled={!selectedSender}>
                            Submit
                        </Button>
                    </FormGroup>
                </Form>
            )}
        </div>
    );
}
