import styles from "./ReleaseNote.module.scss";

interface ReleaseNoteProps {
    header: string;
    sections: Array<{
        title: string;
        tag: "feature" | "bug" | "announcement" | "improvement";
        text: HTMLElementTagNameMap["p"];
    }>;
}

export const ReleaseNote = ({ header, sections }): ReleaseNoteProps => {
    return <></>;
};
