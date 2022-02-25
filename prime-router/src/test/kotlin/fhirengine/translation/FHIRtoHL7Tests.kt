package gov.cdc.prime.router.translation

import assertk.assertThat
import assertk.assertions.isEqualTo
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.encoding.FHIR
import gov.cdc.prime.router.encoding.getValue
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test

class FHIRtoHL7Tests : Logging {
    @Test
    fun `parse fhir path`() {
        // Create a Patient to create
        val patient = Patient()
        patient.setActive(true)
        patient.addIdentifier().setSystem("http://foo").setValue("bar")

        val bundle = Bundle()
        bundle.addEntry().setResource(patient).setFullUrl(patient.getId())

        data class TestCase(
            val path: String,
            val resource: IBase,
        )

        val testCases = listOf(
            TestCase(
                "Patient.identifier.value",
                patient,
            ),
            TestCase(
                "Bundle.entry.resource.as(Patient).identifier.value",
                bundle,
            )
        )

        testCases.forEach { case ->
            assertThat(case.resource.getValue<StringType>(case.path).first().toString()).isEqualTo("bar")
        }
    }

    @Test
    fun `translate message segment`() {
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")

        // Create a Patient to create
        val patient = Patient()
        patient.setActive(true)
        patient.addIdentifier().setSystem("http://foo").setValue("bar")

        val bundle = Bundle()
        bundle.addEntry().setResource(patient).setFullUrl(patient.getId())

        val translation = FHIRtoHL7.Mapping(
            hl7Path = "/.PID-3-1",
            fhirPath = "Bundle.entry.resource.as(Patient).identifier.value",
            value = "{resource}",
        )

        message.translate(bundle, translation)

        val terser = Terser(message)

        assertThat(terser.get("/.PID-3-1")).isEqualTo("bar")
    }

    val rawFHIR = """{
        "resourceType": "Bundle",
        "id": "0e5dbe0d-4c78-4c79-827e-fe44c9e41d86",
        "meta": {
            "lastUpdated": "2022-02-23T13:50:49.918-05:00"
        },
        "type": "collection",
        "entry": [
            {
                "fullUrl": "MessageHeader/c03f1b6b-cfc3-3477-89c0-d38316cd1a38",
                "resource": {
                    "resourceType": "MessageHeader",
                    "id": "c03f1b6b-cfc3-3477-89c0-d38316cd1a38",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "eventCoding": {
                        "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                        "code": "R01",
                        "display": "ORU/ACK - Unsolicited transmission of an observation message"
                    },
                    "destination": [
                        {
                            "name": "CDPH CA REDIE",
                            "endpoint": "CDPH_CID"
                        }
                    ],
                    "source": {
                        "name": "CDC PRIME - Atlanta,"
                    },
                    "reason": {
                        "coding": [
                            {
                                "system": "http://terminology.hl7.org/CodeSystem/message-reasons-encounter",
                                "display": "Invalid input: code 'R01' could not be mapped to values in system 'http://terminology.hl7.org/CodeSystem/message-reasons-encounter'."
                            }
                        ]
                    }
                }
            },
            {
                "fullUrl": "Observation/17dea84f-e7dd-49ad-ab99-0f7d628455c1",
                "resource": {
                    "resourceType": "Observation",
                    "id": "17dea84f-e7dd-49ad-ab99-0f7d628455c1",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "system": "urn:id:extID",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c-"
                        }
                    ],
                    "status": "final",
                    "category": [
                        {
                            "coding": [
                                {
                                    "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                                    "code": "laboratory",
                                    "display": "Laboratory"
                                }
                            ]
                        }
                    ],
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "94558-4"
                            }
                        ],
                        "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"
                    },
                    "subject": {
                        "reference": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569"
                    },
                    "effectiveDateTime": "2021-08-02T00:00:00-05:00",
                    "issued": "2021-08-02T00:00:00-05:00",
                    "performer": [
                        {
                            "reference": "Organization/92cee827-86d2-49bf-b399-1f5ba331d4bc"
                        }
                    ],
                    "valueCodeableConcept": {
                        "coding": [
                            {
                                "code": "260415000"
                            }
                        ],
                        "text": "Not detected"
                    },
                    "interpretation": [
                        {
                            "coding": [
                                {
                                    "system": "http://terminology.hl7.org/CodeSystem/v2-0078",
                                    "code": "N",
                                    "display": "Normal"
                                }
                            ]
                        }
                    ],
                    "method": {
                        "coding": [
                            {
                                "code": "10811877011290_DIT"
                            }
                        ]
                    }
                }
            },
            {
                "fullUrl": "Organization/92cee827-86d2-49bf-b399-1f5ba331d4bc",
                "resource": {
                    "resourceType": "Organization",
                    "id": "92cee827-86d2-49bf-b399-1f5ba331d4bc",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "value": "05D2222542"
                        }
                    ],
                    "name": "Winchester House",
                    "address": [
                        {
                            "line": [
                                "6789 Main St"
                            ],
                            "city": "San Jose",
                            "district": "06085",
                            "state": "CA",
                            "postalCode": "95126"
                        }
                    ],
                    "contact": [
                        {
                            "purpose": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/contactentity-type",
                                        "code": "ADMIN",
                                        "display": "Administrative"
                                    }
                                ],
                                "text": "Organization Medical Director"
                            }
                        }
                    ]
                }
            },
            {
                "fullUrl": "Observation/040733e2-8793-4467-b884-b894fd5688f3",
                "resource": {
                    "resourceType": "Observation",
                    "id": "040733e2-8793-4467-b884-b894fd5688f3",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "system": "urn:id:extID",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c-"
                        }
                    ],
                    "status": "final",
                    "category": [
                        {
                            "coding": [
                                {
                                    "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                                    "code": "laboratory",
                                    "display": "Laboratory"
                                }
                            ]
                        }
                    ],
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "95418-0"
                            }
                        ],
                        "text": "Whether patient is employed in a healthcare setting"
                    },
                    "subject": {
                        "reference": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569"
                    },
                    "effectiveDateTime": "2021-08-02T00:00:00-05:00",
                    "issued": "2021-08-02T00:00:00-05:00",
                    "performer": [
                        {
                            "reference": "Organization/a96e081a-768e-482b-a67f-ca6576bc6d7f"
                        }
                    ],
                    "valueCodeableConcept": {
                        "coding": [
                            {
                                "code": "N"
                            }
                        ],
                        "text": "No"
                    }
                }
            },
            {
                "fullUrl": "Organization/a96e081a-768e-482b-a67f-ca6576bc6d7f",
                "resource": {
                    "resourceType": "Organization",
                    "id": "a96e081a-768e-482b-a67f-ca6576bc6d7f",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "value": "05D2222542"
                        }
                    ],
                    "name": "Winchester House",
                    "address": [
                        {
                            "line": [
                                "6789 Main St"
                            ],
                            "city": "San Jose",
                            "district": "06085",
                            "state": "CA",
                            "postalCode": "95126-5285"
                        }
                    ],
                    "contact": [
                        {
                            "purpose": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/contactentity-type",
                                        "code": "ADMIN",
                                        "display": "Administrative"
                                    }
                                ],
                                "text": "Organization Medical Director"
                            }
                        }
                    ]
                }
            },
            {
                "fullUrl": "Observation/935540a8-0914-49f4-bdad-04ba318a29a7",
                "resource": {
                    "resourceType": "Observation",
                    "id": "935540a8-0914-49f4-bdad-04ba318a29a7",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "system": "urn:id:extID",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c-"
                        }
                    ],
                    "status": "final",
                    "category": [
                        {
                            "coding": [
                                {
                                    "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                                    "code": "laboratory",
                                    "display": "Laboratory"
                                }
                            ]
                        }
                    ],
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "95417-2"
                            }
                        ],
                        "text": "First test for condition of interest"
                    },
                    "subject": {
                        "reference": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569"
                    },
                    "effectiveDateTime": "2021-08-02T00:00:00-05:00",
                    "issued": "2021-08-02T00:00:00-05:00",
                    "performer": [
                        {
                            "reference": "Organization/662fad83-632e-4746-90c6-a47aff80a7ff"
                        }
                    ],
                    "valueCodeableConcept": {
                        "coding": [
                            {
                                "code": "N"
                            }
                        ],
                        "text": "No"
                    }
                }
            },
            {
                "fullUrl": "Organization/662fad83-632e-4746-90c6-a47aff80a7ff",
                "resource": {
                    "resourceType": "Organization",
                    "id": "662fad83-632e-4746-90c6-a47aff80a7ff",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "value": "05D2222542"
                        }
                    ],
                    "name": "Winchester House",
                    "address": [
                        {
                            "line": [
                                "6789 Main St"
                            ],
                            "city": "San Jose",
                            "district": "06085",
                            "state": "CA",
                            "postalCode": "95126-5285"
                        }
                    ],
                    "contact": [
                        {
                            "purpose": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/contactentity-type",
                                        "code": "ADMIN",
                                        "display": "Administrative"
                                    }
                                ],
                                "text": "Organization Medical Director"
                            }
                        }
                    ]
                }
            },
            {
                "fullUrl": "Observation/79e6c0df-d003-4eb0-800e-b1bf4f16cea7",
                "resource": {
                    "resourceType": "Observation",
                    "id": "79e6c0df-d003-4eb0-800e-b1bf4f16cea7",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "system": "urn:id:extID",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c-"
                        }
                    ],
                    "status": "final",
                    "category": [
                        {
                            "coding": [
                                {
                                    "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                                    "code": "laboratory",
                                    "display": "Laboratory"
                                }
                            ]
                        }
                    ],
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "95421-4"
                            }
                        ],
                        "text": "Resides in a congregate care setting"
                    },
                    "subject": {
                        "reference": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569"
                    },
                    "effectiveDateTime": "2021-08-02T00:00:00-05:00",
                    "issued": "2021-08-02T00:00:00-05:00",
                    "performer": [
                        {
                            "reference": "Organization/503e3561-2bfc-4453-ade2-55934efdad41"
                        }
                    ],
                    "valueCodeableConcept": {
                        "coding": [
                            {
                                "code": "Y"
                            }
                        ],
                        "text": "Yes"
                    }
                }
            },
            {
                "fullUrl": "Organization/503e3561-2bfc-4453-ade2-55934efdad41",
                "resource": {
                    "resourceType": "Organization",
                    "id": "503e3561-2bfc-4453-ade2-55934efdad41",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "value": "05D2222542"
                        }
                    ],
                    "name": "Winchester House",
                    "address": [
                        {
                            "line": [
                                "6789 Main St"
                            ],
                            "city": "San Jose",
                            "district": "06085",
                            "state": "CA",
                            "postalCode": "95126-5285"
                        }
                    ],
                    "contact": [
                        {
                            "purpose": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/contactentity-type",
                                        "code": "ADMIN",
                                        "display": "Administrative"
                                    }
                                ],
                                "text": "Organization Medical Director"
                            }
                        }
                    ]
                }
            },
            {
                "fullUrl": "Observation/c5ad4ae9-b20a-4b42-9f20-317a1c44e584",
                "resource": {
                    "resourceType": "Observation",
                    "id": "c5ad4ae9-b20a-4b42-9f20-317a1c44e584",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "system": "urn:id:extID",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c-"
                        }
                    ],
                    "status": "final",
                    "category": [
                        {
                            "coding": [
                                {
                                    "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                                    "code": "laboratory",
                                    "display": "Laboratory"
                                }
                            ]
                        }
                    ],
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "95419-8"
                            }
                        ],
                        "text": "Has symptoms related to condition of interest"
                    },
                    "subject": {
                        "reference": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569"
                    },
                    "effectiveDateTime": "2021-08-02T00:00:00-05:00",
                    "issued": "2021-08-02T00:00:00-05:00",
                    "performer": [
                        {
                            "reference": "Organization/8e49e63f-0f72-4e92-b49e-2adad59d6f3e"
                        }
                    ],
                    "valueCodeableConcept": {
                        "coding": [
                            {
                                "code": "N"
                            }
                        ],
                        "text": "No"
                    }
                }
            },
            {
                "fullUrl": "Organization/8e49e63f-0f72-4e92-b49e-2adad59d6f3e",
                "resource": {
                    "resourceType": "Organization",
                    "id": "8e49e63f-0f72-4e92-b49e-2adad59d6f3e",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "value": "05D2222542"
                        }
                    ],
                    "name": "Winchester House",
                    "address": [
                        {
                            "line": [
                                "6789 Main St"
                            ],
                            "city": "San Jose",
                            "district": "06085",
                            "state": "CA",
                            "postalCode": "95126-5285"
                        }
                    ],
                    "contact": [
                        {
                            "purpose": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/contactentity-type",
                                        "code": "ADMIN",
                                        "display": "Administrative"
                                    }
                                ],
                                "text": "Organization Medical Director"
                            }
                        }
                    ]
                }
            },
            {
                "fullUrl": "Specimen/6e427d02-dbd0-434e-8572-c7b0a6cae42c",
                "resource": {
                    "resourceType": "Specimen",
                    "id": "6e427d02-dbd0-434e-8572-c7b0a6cae42c",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                        }
                    ],
                    "type": {
                        "coding": [
                            {
                                "code": "445297001"
                            }
                        ],
                        "text": "Swab of internal nose"
                    },
                    "receivedTime": "2021-08-02T00:00:06-05:00",
                    "collection": {
                        "bodySite": {
                            "coding": [
                                {
                                    "code": "53342003"
                                }
                            ],
                            "text": "Internal nose structure (body structure)"
                        }
                    }
                }
            },
            {
                "fullUrl": "DiagnosticReport/4b507057-1781-402c-84c1-95d551345c00",
                "resource": {
                    "resourceType": "DiagnosticReport",
                    "id": "4b507057-1781-402c-84c1-95d551345c00",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "system": "urn:id:extID",
                            "value": "20210803131511.0147+0000"
                        },
                        {
                            "type": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                                        "code": "FILL",
                                        "display": "Filler Identifier"
                                    }
                                ]
                            },
                            "system": "urn:id:Winchester_House",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                        },
                        {
                            "type": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                                        "code": "PLAC",
                                        "display": "Placer Identifier"
                                    }
                                ]
                            },
                            "system": "urn:id:Winchester_House",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                        }
                    ],
                    "basedOn": [
                        {
                            "reference": "ServiceRequest/549bedf3-6f64-4d17-a7dd-b73bdd6ec902"
                        }
                    ],
                    "status": "final",
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "94558-4"
                            }
                        ],
                        "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"
                    },
                    "subject": {
                        "reference": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569"
                    },
                    "effectivePeriod": {
                        "start": "2021-08-02T00:00:00-05:00",
                        "end": "2021-08-02T00:00:00-05:00"
                    },
                    "issued": "2021-08-02T00:00:00-05:00",
                    "specimen": [
                        {
                            "reference": "Specimen/6e427d02-dbd0-434e-8572-c7b0a6cae42c"
                        }
                    ],
                    "result": [
                        {
                            "reference": "Observation/17dea84f-e7dd-49ad-ab99-0f7d628455c1"
                        },
                        {
                            "reference": "Observation/040733e2-8793-4467-b884-b894fd5688f3"
                        },
                        {
                            "reference": "Observation/935540a8-0914-49f4-bdad-04ba318a29a7"
                        },
                        {
                            "reference": "Observation/79e6c0df-d003-4eb0-800e-b1bf4f16cea7"
                        },
                        {
                            "reference": "Observation/c5ad4ae9-b20a-4b42-9f20-317a1c44e584"
                        }
                    ]
                }
            },
            {
                "fullUrl": "Practitioner/1d938898-4bfe-4e78-a16f-bdc37bbf886f",
                "resource": {
                    "resourceType": "Practitioner",
                    "id": "1d938898-4bfe-4e78-a16f-bdc37bbf886f",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "type": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                                        "code": "NPI",
                                        "display": "National provider identifier"
                                    }
                                ]
                            },
                            "system": "urn:id:CMS",
                            "value": "1679892871"
                        }
                    ],
                    "name": [
                        {
                            "text": "Doctor Doolittle",
                            "family": "Doolittle",
                            "given": [
                                "Doctor"
                            ]
                        }
                    ]
                }
            },
            {
                "fullUrl": "ServiceRequest/549bedf3-6f64-4d17-a7dd-b73bdd6ec902",
                "resource": {
                    "resourceType": "ServiceRequest",
                    "id": "549bedf3-6f64-4d17-a7dd-b73bdd6ec902",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "identifier": [
                        {
                            "type": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                                        "code": "VN",
                                        "display": "Visit number"
                                    }
                                ]
                            },
                            "value": "20210803131511.0147+0000"
                        },
                        {
                            "type": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                                        "code": "PLAC",
                                        "display": "Placer Identifier"
                                    }
                                ]
                            },
                            "system": "urn:id:Winchester_House",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                        },
                        {
                            "type": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                                        "code": "FILL",
                                        "display": "Filler Identifier"
                                    }
                                ]
                            },
                            "system": "urn:id:Winchester_House",
                            "value": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                        }
                    ],
                    "status": "unknown",
                    "intent": "order",
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "94558-4"
                            }
                        ],
                        "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"
                    },
                    "subject": {
                        "reference": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569"
                    },
                    "occurrenceDateTime": "2021-08-02",
                    "requester": {
                        "reference": "Practitioner/1d938898-4bfe-4e78-a16f-bdc37bbf886f"
                    }
                }
            },
            {
                "fullUrl": "Patient/10f81d63-8a05-4ac9-b1d6-f0019a0a8569",
                "resource": {
                    "resourceType": "Patient",
                    "id": "10f81d63-8a05-4ac9-b1d6-f0019a0a8569",
                    "meta": {
                        "extension": [
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                            "code": "R01"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-type",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://terminology.hl7.org/CodeSystem/v2-0076",
                                            "code": "ORU"
                                        }
                                    ]
                                }
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-event-timestamp",
                                "valueDateTime": "2021-08-03T13:15:11.0147Z"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-record-id",
                                "valueString": "1234d1d1-95fe-462c-8ac6-46728dba581c"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version",
                                "valueString": "2.5.1"
                            },
                            {
                                "url": "http://ibm.com/fhir/cdm/StructureDefinition/process-client-id",
                                "valueString": "CDC PRIME - Atlanta,"
                            }
                        ]
                    },
                    "extension": [
                        {
                            "url": "http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd",
                            "valueCodeableConcept": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v3-Race",
                                        "code": "2106-3"
                                    }
                                ],
                                "text": "White"
                            }
                        }
                    ],
                    "identifier": [
                        {
                            "type": {
                                "coding": [
                                    {
                                        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                                        "code": "PI",
                                        "display": "Patient internal identifier"
                                    }
                                ]
                            },
                            "system": "urn:id:Winchester_House",
                            "value": "09d12345-0987-1234-1234-111b1ee0879f"
                        }
                    ],
                    "name": [
                        {
                            "use": "official",
                            "text": "Bugs C Bunny",
                            "family": "Bunny",
                            "given": [
                                "Bugs",
                                "C"
                            ]
                        }
                    ],
                    "telecom": [
                        {
                            "system": "phone",
                            "value": "+1 123 456 7890",
                            "use": "home"
                        }
                    ],
                    "gender": "male",
                    "birthDate": "1900-01-01",
                    "deceasedBoolean": false,
                    "address": [
                        {
                            "line": [
                                "12345 Main St"
                            ],
                            "city": "San Jose",
                            "district": "06085",
                            "state": "CA",
                            "postalCode": "95125",
                            "country": "USA"
                        }
                    ]
                }
            }
        ]
    }"""
    val rawHL7 = """MSH|^~\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^ISO|CDPH CA REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|20210803131511.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726
PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||Bunny^Bugs^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^CA^95125^USA^^^06085||(123)456-7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N
ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester House|6789 Main St^^San Jose^CA^95126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San Jose^CA^95126
OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.68|||202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-0500|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078^^^^2.7|||F|||202108020000-0500|05D2222542^ISO||10811877011290_DIT^^99ELR^^^^2.68^^10811877011290_DIT||20210802||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126^^^^06085
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure (body structure)^SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"""
    @Test
    fun `translate message`() {
        val bundle = FHIR.decode(rawFHIR)
        // val testMessage = HL7.decode(rawHL7).first()
        val translations = FHIRtoHL7.readMappings("./metadata/hl7_mapping/ORU_R01.yml")

        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")

        translations.forEach { translation ->
            message.translate(bundle, translation)
        }

        /*
        val terser = Terser(message)
        val testTerser = Terser(testMessage)

        translations.forEach{ translation -> 
            assertThat(terser.get(translation.hl7Path)).isEqualTo(testTerser.get(translation.hl7Path))
        }
        */

        val encodedResult = message.encode()
        logger.info("\n$encodedResult")

        /*
        val result = CompareHl7Data().compare(rawHL7.byteInputStream(), encodedResult.byteInputStream())
        logger.info("\n${result.errors.joinToString("\n")}")
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings).isEmpty()
        assertThat(result.passed).isTrue()
        */
    }
}