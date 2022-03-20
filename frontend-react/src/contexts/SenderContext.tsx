import React, { createContext, PropsWithChildren, useState } from "react";

interface ISenderContext {
    sender: string;
    mode: string;
    update: (val: string) => void;
}

export const SenderContext = createContext<ISenderContext>({
    sender: "test",
    mode: "mode",
    update: (val: string) => {
        console.log("to please SonarCloud");
    },
});

const SenderProvider: React.FC<any> = (props: PropsWithChildren<any>) => {
    const [sender, setSender] = useState<string>("");
    const [mode, setMode] = useState<string>("");

    const payload: ISenderContext = {
        sender: sender,
        mode: mode,
        update(val: string): void {
            setSender(val);
        },
    };

    return (
        <SenderContext.Provider value={payload}>
            {props.children}
        </SenderContext.Provider>
    );
};

export default SenderProvider;
