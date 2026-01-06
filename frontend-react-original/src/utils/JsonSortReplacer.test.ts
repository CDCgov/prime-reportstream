import { jsonSortReplacer } from "./JsonSortReplacer";

test("JsonSortReplacer to sort basic json data correctly", () => {
    const result = JSON.stringify(
        { c: 1, a: { d: 0, c: 1, e: { a: 0, 1: 4 } } },
        jsonSortReplacer,
    );
    expect(result).toBe(`{"a":{"c":1,"d":0,"e":{"1":4,"a":0}},"c":1}`);
});

test("JsonSortReplacer to sort array json data correctly", () => {
    // now try arrays with nested arrays
    const result = JSON.stringify(
        { c: 1, a: [["c", "b", "a"], 1, 3, ["cc", "bb", "aa"]] },
        jsonSortReplacer,
    );
    expect(result).toBe(`{"a":[1,3,["a","b","c"],["aa","bb","cc"]],"c":1}`);
});

test("JsonSortReplacer to sort complex json data correctly", () => {
    const COMPLEX_JSON = {
        name: "giang",
        organizationName: "waters",
        topic: "covid-19",
        customerStatus: "active",
        translation: {
            schemaName: "waters/waters-covid-19",
            format: "CSV",
            defaults: {},
            nameFormat: "STANDARD",
            receivingOrganization: null,
            type: "CUSTOM",
        },
        jurisdictionalFilter: [
            "hasAtLeastOneOf(waters_submitter, sender_id)",
            "orEquals(patient_state, GH, ordering_facility_state, GH)",
        ],
        qualityFilter: ["allowAll()"],
        routingFilter: [],
        processingModeFilter: [],
        reverseTheQualityFilter: false,
        deidentify: true,
        timing: {
            operation: "MERGE",
            numberPerDay: 1440,
            initialTime: "00:00",
            timeZone: "EASTERN",
            maxReportCount: 10000,
            whenEmpty: {
                action: "NONE",
                onlyOncePerDay: false,
            },
        },
        description: "",
        transport: {
            host: "sftp",
            port: "22",
            filePath: "./upload",
            credentialName: "DEFAULT-SFTP",
            type: "SFTP",
        },
        version: 0,
        createdBy: "local@test.com",
        createdAt: "2022-02-22T17:40:41.219439Z",
        externalName: null,
    };

    const result = JSON.stringify(COMPLEX_JSON, jsonSortReplacer, 2);

    expect(result).toEqual(`{
  "createdAt": "2022-02-22T17:40:41.219439Z",
  "createdBy": "local@test.com",
  "customerStatus": "active",
  "deidentify": true,
  "description": "",
  "externalName": null,
  "jurisdictionalFilter": [
    "hasAtLeastOneOf(waters_submitter, sender_id)",
    "orEquals(patient_state, GH, ordering_facility_state, GH)"
  ],
  "name": "giang",
  "organizationName": "waters",
  "processingModeFilter": [],
  "qualityFilter": [
    "allowAll()"
  ],
  "reverseTheQualityFilter": false,
  "routingFilter": [],
  "timing": {
    "initialTime": "00:00",
    "maxReportCount": 10000,
    "numberPerDay": 1440,
    "operation": "MERGE",
    "timeZone": "EASTERN",
    "whenEmpty": {
      "action": "NONE",
      "onlyOncePerDay": false
    }
  },
  "topic": "covid-19",
  "translation": {
    "defaults": {},
    "format": "CSV",
    "nameFormat": "STANDARD",
    "receivingOrganization": null,
    "schemaName": "waters/waters-covid-19",
    "type": "CUSTOM"
  },
  "transport": {
    "credentialName": "DEFAULT-SFTP",
    "filePath": "./upload",
    "host": "sftp",
    "port": "22",
    "type": "SFTP"
  },
  "version": 0
}`);
});
