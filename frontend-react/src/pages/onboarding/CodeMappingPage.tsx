import type { PropsWithChildren } from "react";
import CodeMappingForm from "../../components/CodeMapping/CodeMappingForm";

export type CodeMappingPageProps = PropsWithChildren;

const CodeMappingPage = (props: CodeMappingPageProps) => {
    return (
        <>
            <h1>Code mapping tool</h1>
            <CodeMappingForm />
            {props.children}
        </>
    );
};

export default CodeMappingPage;
