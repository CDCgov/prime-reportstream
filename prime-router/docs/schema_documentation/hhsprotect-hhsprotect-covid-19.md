
### Schema:         hhsprotect/hhsprotect-covid-19
#### Description:   Schema for Submission to HHSProtect

---

**Name**: healthcareEmployee

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: serialNumber

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Barcode or QR code.  Unique within one manufacturer.

---

**Name**: deviceName

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Model

**Documentation**:

Required.  Must match LIVD column B, "Model". eg,  "BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B"

---

**Name**: firstTest

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: healthcareEmployeeType

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
1421009|Specialized surgeon
3430008|Radiation therapist
3842006|Chiropractor
4162009|Dental assistant
5275007|NA - Nursing auxiliary
6816002|Specialized nurse
6868009|Hospital administrator
8724009|Plastic surgeon
11661002|Neuropathologist
11911009|Nephrologist
11935004|Obstetrician
13580004|School dental assistant
14698002|Medical microbiologist
17561000|Cardiologist
18803008|Dermatologist
18850004|Laboratory hematologist
19244007|Gerodontist
20145008|Removable prosthodontist
21365001|Specialized dentist
21450003|Neuropsychiatrist
22515006|Medical assistant
22731001|Orthopedic surgeon
22983004|Thoracic surgeon
23278007|Community health physician
24430003|Physical medicine specialist
24590004|Urologist
25961008|Electroencephalography specialist
26042002|Dental hygienist
26369006|Public health nurse
28229004|Optometrist
28411006|Neonatologist
28544002|Chemical pathologist
36682004|PT - Physiotherapist
37154003|Periodontist
37504001|Orthodontist
39677007|Internal medicine specialist
40127002|Dietitian (general)
40204001|Hematologist
40570005|Interpreter
41672002|Respiratory physician
41904004|Medical X-ray technician
43702002|Occupational health nurse
44652006|Pharmaceutical assistant
45419001|Masseur
45440000|Rheumatologist
45544007|Neurosurgeon
45956004|Sanitarian
46255001|Pharmacist
48740002|Philologist
49203003|Dispensing optometrist
49993003|Maxillofacial surgeon
50149000|Endodontist
54503009|Faith healer
56397003|Neurologist
56466003|Community physician
56542007|Medical record administrator
56545009|Cardiovascular surgeon
57654006|Fixed prosthodontist
59058001|General physician
59169001|Orthopedic technician
59944000|Psychologist
60008001|Community-based dietitian
61207006|Medical pathologist
61246008|Laboratory medicine specialist
61345009|Otorhinolaryngologist
61894003|Endocrinologist
62247001|Family medicine specialist
63098009|Clinical immunologist
66476003|Oral pathologist
66862007|Radiologist
68867008|Public health dentist
68950000|Prosthodontist
69280009|Specialized physician
71838004|Gastroenterologist
73265009|Nursing aid
75271001|MW - Midwife
76166008|Practical aid (pharmacy)
76231001|Osteopath
76899008|Infectious diseases physician
78703002|General surgeon
78729002|Diagnostic radiologist
79898004|Auxiliary midwife
80409005|Translator
80546007|OT - Occupational therapist
80584001|Psychiatrist
80933006|Nuclear medicine physician
81464008|Clinical pathologist
82296001|Pediatrician
83189004|Other professional nurse
83273008|Anatomic pathologist
83685006|Gynecologist
85733003|General pathologist
88189002|Anesthesiologist
88475002|Other dietitians and public health nutritionists
90201008|Pediatric dentist
90655003|Care of the elderly physician
106289002|Dental surgeon
106291005|Dietician AND/OR public health nutritionist
106292003|Nurse
106293008|Nursing personnel
106294002|Midwifery personnel
106296000|Physiotherapist AND/OR occupational therapist
106330007|Philologist, translator AND/OR interpreter
112247003|Medical doctor
158965000|Medical practitioner
158966004|Medical administrator - national
158967008|Consultant physician
158968003|Consultant surgeon
158969006|Consultant gynecology and obstetrics
158970007|Anesthetist
158971006|Hospital registrar
158972004|House officer
158973009|Occupational physician
158974003|Clinical medical officer
158975002|Medical practitioner - teaching
158977005|Dental administrator
158978000|Dental consultant
158979008|Dental general practitioner
158980006|Dental practitioner - teaching
158983008|Nurse administrator - national
158984002|Nursing officer - region
158985001|Nursing officer - district
158986000|Nursing administrator - professional body
158987009|Nursing officer - division
158988004|Nurse education director
158989007|Occupational health nursing officer
158990003|Nursing officer
158992006|Midwifery sister
158993001|Nursing sister (theatre)
158994007|Staff nurse
158995008|Staff midwife
158996009|State enrolled nurse
158997000|District nurse
158998005|Private nurse
158999002|Community midwife
159001001|Clinic nurse
159002008|Practice nurse
159003003|School nurse
159004009|Nurse - teaching
159005005|Student nurse
159006006|Dental nurse
159007002|Community pediatric nurse
159010009|Hospital pharmacist
159011008|Retail pharmacist
159012001|Industrial pharmacist
159013006|Pharmaceutical officer H.A.
159014000|Trainee pharmacist
159016003|Medical radiographer
159017007|Diagnostic radiographer
159018002|Therapeutic radiographer
159019005|Trainee radiographer
159021000|Ophthalmic optician
159022007|Trainee optician
159025009|Remedial gymnast
159026005|Speech and language therapist
159027001|Orthoptist
159028006|Trainee remedial therapist
159033005|Dietician
159034004|Podiatrist
159035003|Dental auxiliary
159036002|ECG technician
159037006|EEG technician
159038001|Artificial limb fitter
159039009|AT - Audiology technician
159040006|Pharmacy technician
159041005|Trainee medical technician
159141008|Geneticist
159972006|Surgical corset fitter
160008000|Dental technician
224529009|Clinical assistant
224530004|Senior registrar
224531000|Registrar
224532007|Senior house officer
224533002|MO - Medical officer
224534008|Health visitor, nurse/midwife
224535009|Registered nurse
224536005|Midwifery tutor
224537001|Accident and Emergency nurse
224538006|Triage nurse
224540001|Community nurse
224541002|Nursing continence advisor
224542009|Coronary care nurse
224543004|Diabetic nurse
224544005|Family planning nurse
224545006|Care of the elderly nurse
224546007|ICN - Infection control nurse
224547003|Intensive therapy nurse
224548008|Learning disabilities nurse
224549000|Neonatal nurse
224550000|Neurology nurse
224551001|Industrial nurse
224552008|Oncology nurse
224553003|Macmillan nurse
224554009|Marie Curie nurse
224555005|Pain control nurse
224556006|Palliative care nurse
224557002|Chemotherapy nurse
224558007|Radiotherapy nurse
224559004|PACU nurse
224560009|Stomatherapist
224561008|Theatre nurse
224562001|Pediatric nurse
224563006|Psychiatric nurse
224564000|Community mental health nurse
224565004|Renal nurse
224566003|Hemodialysis nurse
224567007|Wound care nurse
224569005|Nurse grade
224570006|Clinical nurse specialist
224571005|Nurse practitioner
224572003|Nursing sister
224573008|CN - Charge nurse
224574002|Ward manager
224575001|Nursing team leader
224576000|Nursing assistant
224577009|Healthcare assistant
224578004|Nursery nurse
224579007|Healthcare service manager
224580005|Occupational health service manager
224581009|Community nurse manager
224583007|Behavior therapist
224584001|Behavior therapy assistant
224585000|Drama therapist
224586004|Domiciliary occupational therapist
224587008|Occupational therapy helper
224588003|Psychotherapist
224589006|Community-based physiotherapist
224590002|Play therapist
224591003|Play specialist
224592005|Play leader
224593000|Community-based speech/language therapist
224594006|Speech/language assistant
224595007|Professional counselor
224596008|Marriage guidance counselor
224597004|Trained nurse counselor
224598009|Trained social worker counselor
224599001|Trained personnel counselor
224600003|Psychoanalyst
224601004|Assistant psychologist
224602006|Community-based podiatrist
224603001|Foot care worker
224604007|Audiometrician
224605008|Audiometrist
224606009|Technical healthcare occupation
224607000|Occupational therapy technical instructor
224608005|Administrative healthcare staff
224609002|Complementary health worker
224610007|Supporting services personnel
224614003|Research associate
224615002|Research nurse
224620002|Human aid to communication
224621003|Palantypist
224622005|Note taker
224623000|Cuer
224624006|Lipspeaker
224625007|Interpreter for British sign language
224626008|Interpreter for Signs supporting English
224936003|General practitioner locum
225726006|Lactation consultant
225727002|Midwife counselor
265937000|Nursing occupation
265939002|Medical/dental technicians
283875005|Parkinson disease nurse
302211009|Specialist registrar
303124005|Member of mental health review tribunal
303129000|Hospital manager
303133007|Responsible medical officer
303134001|Independent doctor
304291006|Bereavement counselor
304292004|Surgeon
307988006|Medical technician
308002005|Remedial therapist
309294001|Accident and Emergency doctor
309295000|Clinical oncologist
309296004|Family planning doctor
309322005|Associate general practitioner
309323000|Partner of general practitioner
309324006|Assistant GP
309326008|Deputizing general practitioner
309327004|General practitioner registrar
309328009|Ambulatory pediatrician
309329001|Community pediatrician
309330006|Pediatric cardiologist
309331005|Pediatric endocrinologist
309332003|Pediatric gastroenterologist
309333008|Pediatric nephrologist
309334002|Pediatric neurologist
309335001|Pediatric rheumatologist
309336000|Pediatric oncologist
309337009|Pain management specialist
309338004|Intensive care specialist
309339007|Adult intensive care specialist
309340009|Pediatric intensive care specialist
309341008|Blood transfusion doctor
309342001|Histopathologist
309343006|Physician
309345004|Chest physician
309346003|Thoracic physician
309347007|Clinical hematologist
309348002|Clinical neurophysiologist
309349005|Clinical physiologist
309350005|Diabetologist
309351009|Andrologist
309352002|Neuroendocrinologist
309353007|Reproductive endocrinologist
309354001|Thyroidologist
309355000|Clinical geneticist
309356004|Clinical cytogeneticist
309357008|Clinical molecular geneticist
309358003|Genitourinary medicine physician
309359006|Palliative care physician
309360001|Rehabilitation physician
309361002|Child and adolescent psychiatrist
309362009|Forensic psychiatrist
309363004|Liaison psychiatrist
309364005|Psychogeriatrician
309365006|Psychiatrist for mental handicap
309366007|Rehabilitation psychiatrist
309367003|Obstetrician and gynecologist
309368008|Breast surgeon
309369000|Cardiothoracic surgeon
309371000|Cardiac surgeon
309372007|Ear, nose and throat surgeon
309373002|Endocrine surgeon
309374008|Thyroid surgeon
309375009|Pituitary surgeon
309376005|Gastrointestinal surgeon
309377001|General gastrointestinal surgeon
309378006|Upper gastrointestinal surgeon
309379003|Colorectal surgeon
309380000|Hand surgeon
309381001|Hepatobiliary surgeon
309382008|Ophthalmic surgeon
309383003|Pediatric surgeon
309384009|Pancreatic surgeon
309385005|Transplant surgeon
309386006|Trauma surgeon
309388007|Vascular surgeon
309389004|Medical practitioner grade
309390008|Hospital consultant
309391007|Visiting specialist registrar
309392000|Research registrar
309393005|General practitioner grade
309394004|General practitioner principal
309395003|Hospital specialist
309396002|Associate specialist
309397006|Research fellow
309398001|Allied health professional
309399009|Hospital dietitian
309400002|Domiciliary physiotherapist
309401003|General practitioner-based physiotherapist
309402005|Hospital-based physiotherapist
309403000|Private physiotherapist
309404006|Physiotherapy assistant
309409001|Hospital-based speech and language therapist
309410006|Arts therapist
309411005|Dance therapist
309412003|Music therapist
309413008|Renal dietitian
309414002|Liver dietitian
309415001|Oncology dietitian
309416000|Pediatric dietitian
309417009|Diabetes dietitian
309418004|Audiologist
309419007|Hearing therapist
309420001|Audiological scientist
309421002|Hearing aid dispenser
309422009|Community-based occupational therapist
309423004|Hospital occupational therapist
309427003|Social services occupational therapist
309428008|Orthotist
309429000|Surgical fitter
309434001|Hospital-based podiatrist
309435000|Podiatry assistant
309436004|Lymphedema nurse
309437008|Community learning disabilities nurse
309439006|Clinical nurse teacher
309440008|Community practice nurse teacher
309441007|Nurse tutor
309442000|Nurse teacher practitioner
309443005|Nurse lecturer practitioner
309444004|Outreach nurse
309445003|Anesthetic nurse
309446002|Nurse manager
309450009|Nurse administrator
309452001|Midwifery grade
309453006|Midwife
309454000|Student midwife
309455004|Parentcraft sister
309459005|Healthcare professional grade
309460000|Restorative dentist
310170009|Pediatric audiologist
310171008|Immunopathologist
310172001|Audiological physician
310173006|Clinical pharmacologist
310174000|Private doctor
310175004|Agency nurse
310176003|Behavioral therapist nurse
310177007|Cardiac rehabilitation nurse
310178002|Genitourinary nurse
310179005|Rheumatology nurse specialist
310180008|Continence nurse
310181007|Contact tracing nurse
310182000|General nurse
310183005|Nurse for the mentally handicapped
310184004|Liaison nurse
310185003|Diabetic liaison nurse
310186002|Nurse psychotherapist
310187006|Company nurse
310188001|Hospital midwife
310189009|Genetic counselor
310190000|Mental health counselor
310191001|Clinical psychologist
310192008|Educational psychologist
310193003|Coroner
310194009|Appliance officer
310512001|Medical oncologist
311441001|School medical officer
312485001|Integrated midwife
372102007|RN First Assist
387619007|Optician
394572006|Medical secretary
394618009|Hospital nurse
397824005|Consultant anesthetist
397897005|Paramedic
397903001|Staff grade obstetrician
397908005|Staff grade practitioner
398130009|Medical student
398238009|Acting obstetric registrar
404940000|Physiotherapist technical instructor
405277009|Resident physician
405278004|Certified registered nurse anesthetist
405279007|Attending physician
405623001|Assigned practitioner
405684005|Professional initiating surgical case
405685006|Professional providing staff relief during surgical procedure
408798009|Consultant pediatrician
408799001|Consultant neonatologist
409974004|Health educator
409975003|Certified health education specialist
413854007|Circulating nurse
415075003|Perioperative nurse
415506007|Scrub nurse
416160000|Fellow of American Academy of Osteopathy
420409002|Oculoplastic surgeon
420678001|Retinal surgeon
421841007|Admitting physician
422140007|Medical ophthalmologist
422234006|Ophthalmologist
432100008|Health coach
442867008|Respiratory therapist
443090005|Podiatric surgeon
444912007|Hypnotherapist
445313000|Asthma nurse specialist
445451001|Nurse case manager
446050000|PCP - Primary care physician
446701002|Addiction medicine specialist
449161006|PA - physician assistant
471302004|Government midwife
3981000175106|Nurse complex case manager
231189271000087109|Naturopath
236749831000087105|Prosthetist
258508741000087105|Hip and knee surgeon
260767431000087107|Hepatologist
285631911000087106|Shoulder surgeon
291705421000087106|Interventional radiologist
341320851000087105|Pediatric radiologist
368890881000087105|Emergency medicine specialist
398480381000087106|Family medicine specialist - palliative care
416186861000087101|Surgical oncologist
450044741000087104|Acupuncturist
465511991000087105|Pediatric orthopedic surgeon
494782281000087101|Pediatric hematologist
619197631000087102|Neuroradiologist
623630151000087105|Family medicine specialist - anesthetist
666997781000087107|Doula
673825031000087109|Traditional herbal medicine specialist
682131381000087105|Occupational medicine specialist
724111801000087104|Pediatric emergency medicine specialist
747936471000087102|Family medicine specialist - care of the elderly
766788081000087100|Travel medicine specialist
767205061000087108|Spine surgeon
813758161000087106|Maternal or fetal medicine specialist
822410621000087104|Massage therapist
847240411000087102|Hospitalist
853827051000087104|Sports medicine specialist
926871431000087103|Pediatric respirologist
954544641000087107|Homeopath
956387501000087102|Family medicine specialist - emergency medicine
969118571000087109|Pediatric hematologist or oncologist
984095901000087105|Foot and ankle surgeon
990928611000087105|Invasive cardiologist
999480451000087102|Case manager
999480461000087104|Kinesthesiologist

**Documentation**:

Custom.  eg, 6816002

---

**Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: hospitalizedCode

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
260373001|Detected
260415000|Not detected
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
455371000124106|Invalid result
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
373121007|Test not done

**Documentation**:

Custom.  eg, 840539006, same valueset as testResult

---

**Name**: symptomsIcu

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: testId

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

ReportStream copies value from the specimenId if none is provided by the sender.

---

**Name**: testOrderedDate

**Type**: DATE

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [0..1]

**Documentation**:

eg, 20210108

---

**Name**: testOrdered

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Ordered LOINC Code

**Documentation**:

eg, 94531-1

---

**Name**: testName

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Ordered LOINC Long Name

**Documentation**:

Should be the name that matches to Test Ordered LOINC Long Name, in LIVD table

---

**Name**: orderingFacilityState

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Note that many states expect this field to be available, or ReportStream is not able rto route data to them.  Please provide if possible in order for us to route to as many states as possible.

---

**Name**: orderingProviderCity

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: orderingProviderFname

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.3)
- [ORC-12-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.3)

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: orderingProviderNpi

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [0..1]

**Documentation**:

eg, "1265050918"

---

**Name**: orderingProviderLname

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.2)
- [ORC-12-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.2)

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: orderingProviderPhone

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: ordering_provider_phone_number_area_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom.  Not currently used. ReportStream assumes area code is in orderingProviderPhone

---

**Name**: orderingProviderState

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: orderingProviderAddress

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: orderingProviderAddress2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: orderingProviderZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: patAge

**Type**: NUMBER

**PII**: No

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patAgeUnits

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
mo|months
yr|years

**Documentation**:

Always filled when `patient_age` is filled

---

**Name**: patientCity

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patientCounty

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patientEmail

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientEthnicity

**Type**: CODE

**PII**: No

**Format**: $alt

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown

**Alt Value Sets**

Code | Display
---- | -------
H|2135-2
N|2186-5
U|UNK

**Documentation**:

Internally, ReportStream uses hl70189 (H,N,U), but should use HHS values. (2135-2, 2186-5, UNK, ASKU). A mapping is done here, but better is to switch all of RS to HHS standard.

---

**Name**: patient_ethnicity_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom. ReportStream uses the patientEthnicity code, not this text value.

---

**Name**: patientNameFirst

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patientSex

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
M|Male
F|Female
O|Other
A|Ambiguous
U|Unknown
N|Not applicable

**Documentation**:

The patient's gender. There is a valueset defined based on the values in PID-8-1, but downstream consumers are free to define their own accepted values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: patientUniqueId

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patientNameLast

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

Not required, but generally data will not flow to states if last/first name provided.

---

**Name**: patientNameMiddle

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientPhone

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patient_phone_number_area_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom. Not currently used. ReportStream assumes area code is in patientPhone

---

**Name**: patientRace

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
1002-5|American Indian or Alaska Native
2028-9|Asian
2054-5|Black or African American
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2131-1|Other
UNK|Unknown
ASKU|Asked, but unknown

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: patient_race_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom.    ReportStream uses patientRace code, not this text value.

---

**Name**: patientState

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Extremely important field for routing data to states.

---

**Name**: patientHomeAddress

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patientHomeAddress2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: patZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: pregnant

**Type**: CODE

**PII**: No

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown

**Documentation**:

Is the patient pregnant?

---

**Name**: pregnant_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom.  ReportStream uses the 'pregnant' code, not this text value.

---

**Name**: previousTestDate

**Type**: DATE

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [0..1]

**Documentation**:

Custom field

---

**Name**: previousTestResult

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
260373001|Detected
260415000|Not detected
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
455371000124106|Invalid result
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
373121007|Test not done

**Documentation**:

Custom field.  Example - 260415000

---

**Name**: previousTestType

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom field. Note, value matched LIVD column "F", "Test Performed LOINC Code"

---

**Name**: processingModeCode

**Type**: CODE

**PII**: No

**Default Value**: T

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
D|Debugging
P|Production
T|Training

**Documentation**:

P, D, or T for Production, Debugging, or Training

---

**Name**: reportingFacility

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Expecting an ISO heirarchic designator here. eg, "1265050918" (same value as in ordering_provider_npi).  Note that reporting_facility_text is also available as a field, if a name is needed.

---

**Name**: congregateResident

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: submitterId

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.

---

**Name**: congregateResidentType

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
22232009|Hospital
32074000|Long Term Care Hospital
224929004|Secure Hospital
42665001|Nursing Home
30629002|Retirement Home
74056004|Orphanage
722173008|Prison-based care site
20078004|Substance Abuse Treatment Center
257573002|Boarding House
224683003|Military Accommodation
284546000|Hospice
257628001|Hostel
310207003|Sheltered Housing
57656006|Penal Institution
32911000|Homeless

**Documentation**:

Custom field

---

**Name**: specimenDate

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [0..1]

**Documentation**:

eg, 20210113

---

**Name**: specimenId

**Type**: EI

**PII**: No

**HL7 Fields**

- [SPM-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2)

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

**Documentation**:

A unique id, such as a UUID. Note - Need to override the mapper in covid-19.schema file.

---

**Name**: specimenSource

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
445297001|Swab of internal nose
258500001|Nasopharyngeal swab
871810001|Mid-turbinate nasal swab
697989009|Anterior nares swab
258411007|Nasopharyngeal aspirate
429931000124105|Nasal aspirate
258529004|Throat swab
119334006|Sputum specimen
119342007|Saliva specimen
258607008|Bronchoalveolar lavage fluid sample
119364003|Serum specimen
119361006|Plasma specimen
440500007|Dried blood spot specimen
258580003|Whole blood sample
122555007|Venous blood specimen

**Documentation**:

The specimen source, such as Blood or Serum

---

**Name**: symptomatic

**Type**: CODE

**PII**: No

**Format**: $display

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|YES
N|NO
UNK|UNK

**Documentation**:

Override the base hl70136 valueset with a custom one, to handle slightly different syntax

---

**Name**: symptomsList

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom.  Just a simple text string for now. Format is symptomCode1^date1;symptomCode2^date2; ...

---

**Name**: test_authorized_for_home

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-Supplemental-2021-06-07

**Table Column**: is_home

**Documentation**:

Is the test authorized for home use by the FDA (Y, N, UNK)

---

**Name**: test_authorized_for_otc

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-Supplemental-2021-06-07

**Table Column**: is_otc

**Documentation**:

Is the test authorized for over-the-counter purchase by the FDA (Y, N, UNK)

---

**Name**: test_authorized_for_unproctored

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-Supplemental-2021-06-07

**Table Column**: is_unproctored

**Documentation**:

Is the test authorized for unproctored administration by the FDA (Y, N, UNK)

---

**Name**: testCodingSystem

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Custom.  Eg, "LN"

---

**Name**: deviceIdentifier

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Testkit Name ID

**Documentation**:

Must match LIVD column M, "Test Kit Name ID"

---

**Name**: testPerformed

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Test Performed LOINC Code

**Documentation**:

eg, 94558-4

---

**Name**: testResult

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
260373001|Detected
260415000|Not detected
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
455371000124106|Invalid result
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
373121007|Test not done

**Documentation**:

eg, 260373001

---

**Name**: testResultCodingSystem

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

eg, SCT.   Custom

---

**Name**: testResultDate

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [0..1]

**Documentation**:

eg, 20210111

---

**Name**: testReportDate

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [0..1]

**Documentation**:

eg, 20210112

---

**Name**: test_result_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

eg, "DETECTED".  Custom.  ReportStream uses testResult code, not this text value.

---

**Name**: performingFacility

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [OBR-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.3)
- [OBX-15-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.1)
- [OBX-23-10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.10)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.3)

**Cardinality**: [1..1]

**Documentation**:

Expecting a CLIA number here.  eg, "10D2218834"

---

**Name**: facilityZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---
