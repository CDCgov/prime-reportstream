interface TsExportAnnotationTest {
    foo: string;
}

interface TsExportInterface {
    foo: string | undefined;
}

interface TsExportManualTest extends TsExportInterface {
    bar: string;
    foo: undefined;
}