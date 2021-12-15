
### Schema:         upload-covid-19
#### Description:   Schema for CSV Upload Tool

---

**Name**: abnormal_flag

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Abnormal (applies to non-numeric results)
&#62;|Above absolute high-off instrument scale
H|Above high normal
HH|Above upper panic limits
AC|Anti-complementary substances present
<|Below absolute low-off instrument scale
L|Below low normal
LL|Below lower panic limits
B|Better--use when direction not relevant
TOX|Cytotoxic substance present
DET|Detected
IND|Indeterminate
I|Intermediate. Indicates for microbiology susceptibilities only.
MS|Moderately susceptible. Indicates for microbiology susceptibilities only.
NEG|Negative
null|No range defined, or normal ranges don't apply
NR|Non-reactive
N|Normal (applies to non-numeric results)
ND|Not Detected
POS|Positive
QCF|Quality Control Failure
RR|Reactive
R|Resistant. Indicates for microbiology susceptibilities only.
D|Significant change down
U|Significant change up
S|Susceptible. Indicates for microbiology susceptibilities only.
AA|Very abnormal (applies to non-numeric units, analogous to panic limits for numeric units)
VS|Very susceptible. Indicates for microbiology susceptibilities only.
WR|Weakly reactive
W|Worse--use when direction not relevant

**Documentation**:

This field is generated based on the normalcy status of the result. A = abnormal; N = normal

---

**Name**: comment

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: comment_source

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
L|Ancillary (filler) department is source of comment
O|Other system is source of comment
P|Orderer (placer) is source of comment

---

**Name**: comment_type

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
1R|Primary Reason
2R|Secondary Reason
AI|Ancillary Instructions
DR|Duplicate/Interaction Reason
GI|General Instructions
GR|General Reason
PI|Patient Instructions
RE|Remark

---

**Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: device_id

**Type**: TABLE

**PII**: No

**HL7 Fields**

- [OBX-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.17.1)
- [OBX-17-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.17.9)

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Documentation**:

Device_id is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: device_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Documentation**:

Device_id_type is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

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

**Name**: employed_in_high_risk_setting

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient employed in a high risk setting? This AOE question doesn't have an HL7 conversion per the HHS, so it is not included in HL7 messages.


---

**Name**: equipment_instance_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: equipment_manufacture

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Manufacturer

---

**Name**: equipment_model_id

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Equipment UID

---

**Name**: equipment_model_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Equipment UID Type

---

**Name**: deviceName

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Model

**Documentation**:

Required.  Must match a value from LIVD column B, "Model". eg,  "BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B"

---

**Name**: file_created_date

**Type**: DATE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

When was this file created. This is only used for HL7 generation.

---

**Name**: filler_clia

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: filler_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)

**Cardinality**: [0..1]

---

**Name**: filler_order_id

**Type**: ID

**PII**: No

**HL7 Fields**

- [OBR-3-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.1)
- [ORC-3-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.1)
- [SPM-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2)

**Cardinality**: [0..1]

**Documentation**:

Accension number

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

**Name**: flatfile_version_no

**Type**: NUMBER

**PII**: No

**Cardinality**: [0..1]

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
82334004|Indeterminate

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

**Name**: illness_onset_date

**Type**: DATE

**PII**: No

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: testId

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

This is a crucial required field. A unique id for this submission of data.  Do not re-use when correcting previous results.  Rather, submit a new testId, and use the correctedTestId to refer to the testId of the old submission.

---

**Name**: message_profile_id

**Type**: EI

**PII**: No

**Default Value**: PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO

**Cardinality**: [0..1]

**Documentation**:

The message profile identifer

---

**Name**: observation_result_status

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
C|Record coming over is a correction and thus replaces a final result
D|Deletes the OBX record
F|Final results; Can only be changed with a corrected result
I|Specimen in lab; results pending
N|Not asked; used to affirmatively document that the observation identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be sought.
O|Order detail description only (no result)
P|Preliminary results
R|Results entered -- not verified
S|Partial results
U|Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final
W|Post original as wrong, e.g., transmitted for wrong patient
X|Results cannot be obtained for this observation

---

**Name**: order_result_status

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Some, but not all, results available
C|Corrected, final
F|Final results
I|No results available; specimen received, procedure incomplete
M|Corrected, not final
N|Procedure completed, results pending
O|Order received; specimen not yet received
P|Preliminary
R|Results stored; not yet verified
S|No results available; procedure scheduled, but not done
X|No results available; Order canceled
Y|No order on record for this test
Z|No record of this patient

---

**Name**: testOrderedDate

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

eg, 20210108

---

**Name**: testOrdered

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Test Ordered LOINC Code

**Documentation**:

Leave this blank and we'll fill it in automatically.

---

**Name**: ordered_test_encoding_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: LOINC Version ID

---

**Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Test Ordered LOINC Long Name

---

**Name**: ordered_test_system

**Type**: TEXT

**PII**: No

**Default Value**: LOINC

**Cardinality**: [0..1]

---

**Name**: ordered_test_system_abbr

**Type**: TEXT

**PII**: No

**Default Value**: LN

**Cardinality**: [0..1]

---

**Name**: orderingFacilityCity

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: orderingFacilityCounty

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

---

**Name**: orderingFacilityEmail

**Type**: EMAIL

**PII**: No

**Cardinality**: [0..1]

---

**Name**: orderingFacilityName

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: orderingFacilityPhone

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: orderingFacilityState

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Note that many states expect this field to be available, or ReportStream is not able to route data to them.  Please provide if possible in order for us to route to as many states as possible.

---

**Name**: orderingFacilityStreet

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: orderingFacilityStreet2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The secondary address of the facility which the test was ordered from

---

**Name**: orderingFacilityZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: orderingProviderCity

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: ordering_provider_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: ordering_provider_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_provider_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:

The FIPS code for the ordering provider

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

**Name**: ordering_provider_id_authority

**Type**: HD

**PII**: No

**HL7 Fields**

- [OBR-16-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.9)
- [ORC-12-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.9)

**Cardinality**: [0..1]

**Documentation**:

Usually the OID for CMS

---

**Name**: ordering_provider_id_authority_type

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-16-13](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.13)
- [ORC-12-13](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.13)

**Cardinality**: [0..1]

**Documentation**:

Usually NPI

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

**Name**: ordering_provider_middle_initial

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.4)
- [ORC-12-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.4)

**Cardinality**: [0..1]

---

**Name**: ordering_provider_middle_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.4)
- [ORC-12-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.4)

**Cardinality**: [0..1]

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

**Name**: organization_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The SimpleReport concept of organization. It refers to organization for the ordering & performing facility

---

**Name**: patientAge

**Type**: NUMBER

**PII**: No

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patientAgeUnits

**Type**: CODE

**PII**: No

**Default Value**: yr

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
mo|months
yr|years

**Documentation**:

User does not need to specify this.  Default to 'yr' if not specified.

---

**Name**: patientCity

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: patientCounty

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:

The FIPS code for the patient's county

---

**Name**: patient_death_date

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_died

**Type**: CODE

**PII**: Yes

**Default Value**: N

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

---

**Name**: patientDob

**Type**: DATE

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patient_drivers_license

**Type**: ID_DLN

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's drivers license number

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
U|Unknown

**Alt Value Sets**

Code | Display
---- | -------
H|2135-2
N|2186-5
U|UNK
U|ASKU

**Documentation**:

Use the required HHS values. (2135-2, 2186-5, UNK, ASKU)

---

**Name**: patientNameFirst

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

Required

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

**Name**: patientUniqueId

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: patient_id_assigner

**Type**: HD

**PII**: No

**HL7 Fields**

- [PID-3-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)

**Cardinality**: [0..1]

**Documentation**:

The name of the assigner of the patient_id field. Typically we use the name of the ordering facility

---

**Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Default Value**: PI

**Cardinality**: [0..1]

**Documentation**:

User does not need to fill this in.

---

**Name**: patientNameLast

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

Required

---

**Name**: patient_middle_initial

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientNameMiddle

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_name_type_code

**Type**: TEXT

**PII**: No

**Default Value**: L

**Cardinality**: [0..1]

---

**Name**: patientPhone

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

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

**Name**: patientState

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Required. Extremely important field for routing data to states.

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

**Name**: patient_suffix

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The suffix for the patient's name, (i.e. Jr, Sr, etc)

---

**Name**: patient_tribal_citizenship

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
338|Village of Afognak
339|Agdaagux Tribe of King Cove
340|Native Village of Akhiok
341|Akiachak Native Community
342|Akiak Native Community
343|Native Village of Akutan
344|Village of Alakanuk
345|Alatna Village
346|Native Village of Aleknagik
347|Algaaciq Native Village (St. Mary's)
348|Allakaket Village
349|Native Village of Ambler
350|Village of Anaktuvuk Pass
351|Yupiit of Andreafski
352|Angoon Community Association
353|Village of Aniak
354|Anvik Village
355|Arctic Village (See Native Village of Venetie Trib
356|Asa carsarmiut Tribe (formerly Native Village of M
357|Native Village of Atka
358|Village of Atmautluak
359|Atqasuk Village (Atkasook)
360|Native Village of Barrow Inupiat Traditional Gover
361|Beaver Village
362|Native Village of Belkofski
363|Village of Bill Moore's Slough
364|Birch Creek Tribe
365|Native Village of Brevig Mission
366|Native Village of Buckland
367|Native Village of Cantwell
368|Native Village of Chanega (aka Chenega)
369|Chalkyitsik Village
370|Village of Chefornak
371|Chevak Native Village
372|Chickaloon Native Village
373|Native Village of Chignik
374|Native Village of Chignik Lagoon
375|Chignik Lake Village
376|Chilkat Indian Village (Klukwan)
377|Chilkoot Indian Association (Haines)
378|Chinik Eskimo Community (Golovin)
379|Native Village of Chistochina
380|Native Village of Chitina
381|Native Village of Chuathbaluk (Russian Mission, Ku
382|Chuloonawick Native Village
383|Circle Native Community
384|Village of Clark's Point
385|Native Village of Council
386|Craig Community Association
387|Village of Crooked Creek
388|Curyung Tribal Council (formerly Native Village of
389|Native Village of Deering
390|Native Village of Diomede (aka Inalik)
391|Village of Dot Lake
392|Douglas Indian Association
393|Native Village of Eagle
394|Native Village of Eek
395|Egegik Village
396|Eklutna Native Village
397|Native Village of Ekuk
398|Ekwok Village
399|Native Village of Elim
400|Emmonak Village
401|Evansville Village (aka Bettles Field)
402|Native Village of Eyak (Cordova)
403|Native Village of False Pass
404|Native Village of Fort Yukon
405|Native Village of Gakona
406|Galena Village (aka Louden Village)
407|Native Village of Gambell
408|Native Village of Georgetown
409|Native Village of Goodnews Bay
410|Organized Village of Grayling (aka Holikachuk)
411|Gulkana Village
412|Native Village of Hamilton
413|Healy Lake Village
414|Holy Cross Village
415|Hoonah Indian Association
416|Native Village of Hooper Bay
417|Hughes Village
418|Huslia Village
419|Hydaburg Cooperative Association
420|Igiugig Village
421|Village of Iliamna
422|Inupiat Community of the Arctic Slope
423|Iqurmuit Traditional Council (formerly Native Vill
424|Ivanoff Bay Village
425|Kaguyak Village
426|Organized Village of Kake
427|Kaktovik Village (aka Barter Island)
428|Village of Kalskag
429|Village of Kaltag
430|Native Village of Kanatak
431|Native Village of Karluk
432|Organized Village of Kasaan
433|Native Village of Kasigluk
434|Kenaitze Indian Tribe
435|Ketchikan Indian Corporation
436|Native Village of Kiana
437|King Island Native Community
438|King Salmon Tribe
439|Native Village of Kipnuk
440|Native Village of Kivalina
441|Klawock Cooperative Association
442|Native Village of Kluti Kaah (aka Copper Center)
443|Knik Tribe
444|Native Village of Kobuk
445|Kokhanok Village
446|Native Village of Kongiganak
447|Village of Kotlik
448|Native Village of Kotzebue
449|Native Village of Koyuk
450|Koyukuk Native Village
451|Organized Village of Kwethluk
452|Native Village of Kwigillingok
453|Native Village of Kwinhagak (aka Quinhagak)
454|Native Village of Larsen Bay
455|Levelock Village
456|Lesnoi Village (aka Woody Island)
457|Lime Village
458|Village of Lower Kalskag
459|Manley Hot Springs Village
460|Manokotak Village
461|Native Village of Marshall (aka Fortuna Ledge)
462|Native Village of Mary's Igloo
463|McGrath Native Village
464|Native Village of Mekoryuk
465|Mentasta Traditional Council
466|Metlakatla Indian Community, Annette Island Reserv
467|Native Village of Minto
468|Naknek Native Village
469|Native Village of Nanwalek (aka English Bay)
470|Native Village of Napaimute
471|Native Village of Napakiak
472|Native Village of Napaskiak
473|Native Village of Nelson Lagoon
474|Nenana Native Association
475|New Koliganek Village Council (formerly Koliganek
476|New Stuyahok Village
477|Newhalen Village
478|Newtok Village
479|Native Village of Nightmute
480|Nikolai Village
481|Native Village of Nikolski
482|Ninilchik Village
483|Native Village of Noatak
484|Nome Eskimo Community
485|Nondalton Village
486|Noorvik Native Community
487|Northway Village
488|Native Village of Nuiqsut (aka Nooiksut)
489|Nulato Village
490|Nunakauyarmiut Tribe (formerly Native Village of T
491|Native Village of Nunapitchuk
492|Village of Ohogamiut
493|Village of Old Harbor
494|Orutsararmuit Native Village (aka Bethel)
495|Oscarville Traditional Village
496|Native Village of Ouzinkie
497|Native Village of Paimiut
498|Pauloff Harbor Village
499|Pedro Bay Village
500|Native Village of Perryville
501|Petersburg Indian Association
502|Native Village of Pilot Point
503|Pilot Station Traditional Village
504|Native Village of Pitka's Point
505|Platinum Traditional Village
506|Native Village of Point Hope
507|Native Village of Point Lay
508|Native Village of Port Graham
509|Native Village of Port Heiden
510|Native Village of Port Lions
511|Portage Creek Village (aka Ohgsenakale)
512|Pribilof Islands Aleut Communities of St. Paul & S
513|Qagan Tayagungin Tribe of Sand Point Village
514|Qawalangin Tribe of Unalaska
515|Rampart Village
516|Village of Red Devil
517|Native Village of Ruby
518|Saint George Island(See Pribilof Islands Aleut Com
519|Native Village of Saint Michael
520|Saint Paul Island (See Pribilof Islands Aleut Comm
521|Village of Salamatoff
522|Native Village of Savoonga
523|Organized Village of Saxman
524|Native Village of Scammon Bay
525|Native Village of Selawik
526|Seldovia Village Tribe
527|Shageluk Native Village
528|Native Village of Shaktoolik
529|Native Village of Sheldon's Point
530|Native Village of Shishmaref
531|Shoonaq Tribe of Kodiak
532|Native Village of Shungnak
533|Sitka Tribe of Alaska
534|Skagway Village
535|Village of Sleetmute
536|Village of Solomon
537|South Naknek Village
538|Stebbins Community Association
539|Native Village of Stevens
540|Village of Stony River
541|Takotna Village
542|Native Village of Tanacross
543|Native Village of Tanana
544|Native Village of Tatitlek
545|Native Village of Tazlina
546|Telida Village
547|Native Village of Teller
548|Native Village of Tetlin
549|Central Council of the Tlingit and Haida Indian Tb
550|Traditional Village of Togiak
551|Tuluksak Native Community
552|Native Village of Tuntutuliak
553|Native Village of Tununak
554|Twin Hills Village
555|Native Village of Tyonek
556|Ugashik Village
557|Umkumiute Native Village
558|Native Village of Unalakleet
559|Native Village of Unga
560|Village of Venetie (See Native Village of Venetie
561|Native Village of Venetie Tribal Government (Arcti
562|Village of Wainwright
563|Native Village of Wales
564|Native Village of White Mountain
565|Wrangell Cooperative Association
566|Yakutat Tlingit Tribe
1|Absentee-Shawnee Tribe of Indians of Oklahoma
10|Assiniboine and Sioux Tribes of the Fort Peck Indi
100|Havasupai Tribe of the Havasupai Reservation, Ariz
101|Ho-Chunk Nation of Wisconsin (formerly known as th
102|Hoh Indian Tribe of the Hoh Indian Reservation, Wa
103|Hoopa Valley Tribe, California
104|Hopi Tribe of Arizona
105|Hopland Band of Pomo Indians of the Hopland Ranche
106|Houlton Band of Maliseet Indians of Maine
107|Hualapai Indian Tribe of the Hualapai Indian Reser
108|Huron Potawatomi, Inc., Michigan
109|Inaja Band of Diegueno Mission Indians of the Inaj
11|Augustine Band of Cahuilla Mission Indians of the
110|Ione Band of Miwok Indians of California
111|Iowa Tribe of Kansas and Nebraska
112|Iowa Tribe of Oklahoma
113|Jackson Rancheria of Me-Wuk Indians of California
114|Jamestown S'Klallam Tribe of Washington
115|Jamul Indian Village of California
116|Jena Band of Choctaw Indians, Louisiana
117|Jicarilla Apache Tribe of the Jicarilla Apache Ind
118|Kaibab Band of Paiute Indians of the Kaibab Indian
119|Kalispel Indian Community of the Kalispel Reservat
12|Bad River Band of the Lake Superior Tribe of Chipp
120|Karuk Tribe of California
121|Kashia Band of Pomo Indians of the Stewarts Point
122|Kaw Nation, Oklahoma
123|Keweenaw Bay Indian Community of L'Anse and Ontona
124|Kialegee Tribal Town, Oklahoma
125|Kickapoo Tribe of Indians of the Kickapoo Reservat
126|Kickapoo Tribe of Oklahoma
127|Kickapoo Traditional Tribe of Texas
128|Kiowa Indian Tribe of Oklahoma
129|Klamath Indian Tribe of Oregon
13|Bay Mills Indian Community of the Sault Ste. Marie
130|Kootenai Tribe of Idaho
131|La Jolla Band of Luiseno Mission Indians of the La
132|La Posta Band of Diegueno Mission Indians of the L
133|Lac Courte Oreilles Band of Lake Superior Chippewa
134|Lac du Flambeau Band of Lake Superior Chippewa Ind
135|Lac Vieux Desert Band of Lake Superior Chippewa In
136|Las Vegas Tribe of Paiute Indians of the Las Vegas
137|Little River Band of Ottawa Indians of Michigan
138|Little Traverse Bay Bands of Odawa Indians of Mich
139|Lower Lake Rancheria, California
14|Bear River Band of the Rohnerville Rancheria, Cali
140|Los Coyotes Band of Cahuilla Mission Indians of th
141|Lovelock Paiute Tribe of the Lovelock Indian Colon
142|Lower Brule Sioux Tribe of the Lower Brule Reserva
143|Lower Elwha Tribal Community of the Lower Elwha Re
144|Lower Sioux Indian Community of Minnesota Mdewakan
145|Lummi Tribe of the Lummi Reservation, Washington
146|Lytton Rancheria of California
147|Makah Indian Tribe of the Makah Indian Reservation
148|Manchester Band of Pomo Indians of the Manchester-
149|Manzanita Band of Diegueno Mission Indians of the
15|Berry Creek Rancheria of Maidu Indians of Californ
150|Mashantucket Pequot Tribe of Connecticut
151|Match-e-be-nash-she-wish Band of Pottawatomi India
152|Mechoopda Indian Tribe of Chico Rancheria, Califor
153|Menominee Indian Tribe of Wisconsin
154|Mesa Grande Band of Diegueno Mission Indians of th
155|Mescalero Apache Tribe of the Mescalero Reservatio
156|Miami Tribe of Oklahoma
157|Miccosukee Tribe of Indians of Florida
158|Middletown Rancheria of Pomo Indians of California
159|Minnesota Chippewa Tribe, Minnesota (Six component
16|Big Lagoon Rancheria, California
160|Bois Forte Band (Nett Lake); Fond du Lac Band; Gra
161|Mississippi Band of Choctaw Indians, Mississippi
162|Moapa Band of Paiute Indians of the Moapa River In
163|Modoc Tribe of Oklahoma
164|Mohegan Indian Tribe of Connecticut
165|Mooretown Rancheria of Maidu Indians of California
166|Morongo Band of Cahuilla Mission Indians of the Mo
167|Muckleshoot Indian Tribe of the Muckleshoot Reserv
168|Muscogee (Creek) Nation, Oklahoma
169|Narragansett Indian Tribe of Rhode Island
17|Big Pine Band of Owens Valley Paiute Shoshone Indi
170|Navajo Nation, Arizona, New Mexico & Utah
171|Nez Perce Tribe of Idaho
172|Nisqually Indian Tribe of the Nisqually Reservatio
173|Nooksack Indian Tribe of Washington
174|Northern Cheyenne Tribe of the Northern Cheyenne I
175|Northfork Rancheria of Mono Indians of California
176|Northwestern Band of Shoshoni Nation of Utah (Wash
177|Oglala Sioux Tribe of the Pine Ridge Reservation,
178|Omaha Tribe of Nebraska
179|Oneida Nation of New York
18|Big Sandy Rancheria of Mono Indians of California
180|Oneida Tribe of Wisconsin
181|Onondaga Nation of New York
182|Osage Tribe, Oklahoma
183|Ottawa Tribe of Oklahoma
184|Otoe-Missouria Tribe of Indians, Oklahoma
185|Paiute Indian Tribe of Utah
186|Paiute-Shoshone Indians of the Bishop Community of
187|Paiute-Shoshone Tribe of the Fallon Reservation an
188|Paiute-Shoshone Indians of the Lone Pine Community
189|Pala Band of Luiseno Mission Indians of the Pala R
19|Big Valley Band of Pomo Indians of the Big Valley
190|Pascua Yaqui Tribe of Arizona
191|Paskenta Band of Nomlaki Indians of California
192|Passamaquoddy Tribe of Maine
193|Pauma Band of Luiseno Mission Indians of the Pauma
194|Pawnee Nation of Oklahoma
195|Pechanga Band of Luiseno Mission Indians of the Pe
196|Penobscot Tribe of Maine
197|Peoria Tribe of Indians of Oklahoma
198|Picayune Rancheria of Chukchansi Indians of Califo
199|Pinoleville Rancheria of Pomo Indians of Californi
2|Agua Caliente Band of Cahuilla Indians of the Agua
20|Blackfeet Tribe of the Blackfeet Indian Reservatio
200|Pit River Tribe, California (includes Big Bend, Lo
201|Poarch Band of Creek Indians of Alabama
202|Pokagon Band of Potawatomi Indians of Michigan
203|Ponca Tribe of Indians of Oklahoma
204|Ponca Tribe of Nebraska
205|Port Gamble Indian Community of the Port Gamble Re
206|Potter Valley Rancheria of Pomo Indians of Califor
207|Prairie Band of Potawatomi Indians, Kansas
208|Prairie Island Indian Community of Minnesota Mdewa
209|Pueblo of Acoma, New Mexico
21|Blue Lake Rancheria, California
210|Pueblo of Cochiti, New Mexico
211|Pueblo of Jemez, New Mexico
212|Pueblo of Isleta, New Mexico
213|Pueblo of Laguna, New Mexico
214|Pueblo of Nambe, New Mexico
215|Pueblo of Picuris, New Mexico
216|Pueblo of Pojoaque, New Mexico
217|Pueblo of San Felipe, New Mexico
218|Pueblo of San Juan, New Mexico
219|Pueblo of San Ildefonso, New Mexico
22|Bridgeport Paiute Indian Colony of California
220|Pueblo of Sandia, New Mexico
221|Pueblo of Santa Ana, New Mexico
222|Pueblo of Santa Clara, New Mexico
223|Pueblo of Santo Domingo, New Mexico
224|Pueblo of Taos, New Mexico
225|Pueblo of Tesuque, New Mexico
226|Pueblo of Zia, New Mexico
227|Puyallup Tribe of the Puyallup Reservation, Washin
228|Pyramid Lake Paiute Tribe of the Pyramid Lake Rese
229|Quapaw Tribe of Indians, Oklahoma
23|Buena Vista Rancheria of Me-Wuk Indians of Califor
230|Quartz Valley Indian Community of the Quartz Valle
231|Quechan Tribe of the Fort Yuma Indian Reservation,
232|Quileute Tribe of the Quileute Reservation, Washin
233|Quinault Tribe of the Quinault Reservation, Washin
234|Ramona Band or Village of Cahuilla Mission Indians
235|Red Cliff Band of Lake Superior Chippewa Indians o
236|Red Lake Band of Chippewa Indians of the Red Lake
237|Redding Rancheria, California
238|Redwood Valley Rancheria of Pomo Indians of Califo
239|Reno-Sparks Indian Colony, Nevada
24|Burns Paiute Tribe of the Burns Paiute Indian Colo
240|Resighini Rancheria, California (formerly known as
241|Rincon Band of Luiseno Mission Indians of the Rinc
242|Robinson Rancheria of Pomo Indians of California
243|Rosebud Sioux Tribe of the Rosebud Indian Reservat
244|Round Valley Indian Tribes of the Round Valley Res
245|Rumsey Indian Rancheria of Wintun Indians of Calif
246|Sac and Fox Tribe of the Mississippi in Iowa
247|Sac and Fox Nation of Missouri in Kansas and Nebra
248|Sac and Fox Nation, Oklahoma
249|Saginaw Chippewa Indian Tribe of Michigan, Isabell
25|Cabazon Band of Cahuilla Mission Indians of the Ca
250|Salt River Pima-Maricopa Indian Community of the S
251|Samish Indian Tribe, Washington
252|San Carlos Apache Tribe of the San Carlos Reservat
253|San Juan Southern Paiute Tribe of Arizona
254|San Manual Band of Serrano Mission Indians of the
255|San Pasqual Band of Diegueno Mission Indians of Ca
256|Santa Rosa Indian Community of the Santa Rosa Ranc
257|Santa Rosa Band of Cahuilla Mission Indians of the
258|Santa Ynez Band of Chumash Mission Indians of the
259|Santa Ysabel Band of Diegueno Mission Indians of t
26|Cachil DeHe Band of Wintun Indians of the Colusa I
260|Santee Sioux Tribe of the Santee Reservation of Ne
261|Sauk-Suiattle Indian Tribe of Washington
262|Sault Ste. Marie Tribe of Chippewa Indians of Mich
263|Scotts Valley Band of Pomo Indians of California
264|Seminole Nation of Oklahoma
265|Seminole Tribe of Florida, Dania, Big Cypress, Bri
266|Seneca Nation of New York
267|Seneca-Cayuga Tribe of Oklahoma
268|Shakopee Mdewakanton Sioux Community of Minnesota
269|Shawnee Tribe, Oklahoma
27|Caddo Indian Tribe of Oklahoma
270|Sherwood Valley Rancheria of Pomo Indians of Calif
271|Shingle Springs Band of Miwok Indians, Shingle Spr
272|Shoalwater Bay Tribe of the Shoalwater Bay Indian
273|Shoshone Tribe of the Wind River Reservation, Wyom
274|Shoshone-Bannock Tribes of the Fort Hall Reservati
275|Shoshone-Paiute Tribes of the Duck Valley Reservat
276|Sisseton-Wahpeton Sioux Tribe of the Lake Traverse
277|Skokomish Indian Tribe of the Skokomish Reservatio
278|Skull Valley Band of Goshute Indians of Utah
279|Smith River Rancheria, California
28|Cahuilla Band of Mission Indians of the Cahuilla R
280|Snoqualmie Tribe, Washington
281|Soboba Band of Luiseno Indians, California (former
282|Sokaogon Chippewa Community of the Mole Lake Band
283|Southern Ute Indian Tribe of the Southern Ute Rese
284|Spirit Lake Tribe, North Dakota (formerly known as
285|Spokane Tribe of the Spokane Reservation, Washingt
286|Squaxin Island Tribe of the Squaxin Island Reserva
287|St. Croix Chippewa Indians of Wisconsin, St. Croix
288|St. Regis Band of Mohawk Indians of New York
289|Standing Rock Sioux Tribe of North & South Dakota
29|Cahto Indian Tribe of the Laytonville Rancheria, C
290|Stockbridge-Munsee Community of Mohican Indians of
291|Stillaguamish Tribe of Washington
292|Summit Lake Paiute Tribe of Nevada
293|Suquamish Indian Tribe of the Port Madison Reserva
294|Susanville Indian Rancheria, California
295|Swinomish Indians of the Swinomish Reservation, Wa
296|Sycuan Band of Diegueno Mission Indians of Califor
297|Table Bluff Reservation - Wiyot Tribe, California
298|Table Mountain Rancheria of California
299|Te-Moak Tribe of Western Shoshone Indians of Nevad
3|Ak Chin Indian Community of the Maricopa (Ak Chin)
30|California Valley Miwok Tribe, California (formerl
300|Thlopthlocco Tribal Town, Oklahoma
301|Three Affiliated Tribes of the Fort Berthold Reser
302|Tohono O'odham Nation of Arizona
303|Tonawanda Band of Seneca Indians of New York
304|Tonkawa Tribe of Indians of Oklahoma
305|Tonto Apache Tribe of Arizona
306|Torres-Martinez Band of Cahuilla Mission Indians o
307|Tule River Indian Tribe of the Tule River Reservat
308|Tulalip Tribes of the Tulalip Reservation, Washing
309|Tunica-Biloxi Indian Tribe of Louisiana
31|Campo Band of Diegueno Mission Indians of the Camp
310|Tuolumne Band of Me-Wuk Indians of the Tuolumne Ra
311|Turtle Mountain Band of Chippewa Indians of North
312|Tuscarora Nation of New York
313|Twenty-Nine Palms Band of Mission Indians of Calif
314|United Auburn Indian Community of the Auburn Ranch
315|United Keetoowah Band of Cherokee Indians of Oklah
316|Upper Lake Band of Pomo Indians of Upper Lake Ranc
317|Upper Sioux Indian Community of the Upper Sioux Re
318|Upper Skagit Indian Tribe of Washington
319|Ute Indian Tribe of the Uintah & Ouray Reservation
32|Capitan Grande Band of Diegueno Mission Indians of
320|Ute Mountain Tribe of the Ute Mountain Reservation
321|Utu Utu Gwaitu Paiute Tribe of the Benton Paiute R
322|Walker River Paiute Tribe of the Walker River Rese
323|Wampanoag Tribe of Gay Head (Aquinnah) of Massachu
324|Washoe Tribe of Nevada & California (Carson Colony
325|White Mountain Apache Tribe of the Fort Apache Res
326|Wichita and Affiliated Tribes (Wichita, Keechi, Wa
327|Winnebago Tribe of Nebraska
328|Winnemucca Indian Colony of Nevada
329|Wyandotte Tribe of Oklahoma
33|Barona Group of Capitan Grande Band of Mission Ind
330|Yankton Sioux Tribe of South Dakota
331|Yavapai-Apache Nation of the Camp Verde Indian Res
332|Yavapai-Prescott Tribe of the Yavapai Reservation,
333|Yerington Paiute Tribe of the Yerington Colony & C
334|Yomba Shoshone Tribe of the Yomba Reservation, Nev
335|Ysleta Del Sur Pueblo of Texas
336|Yurok Tribe of the Yurok Reservation, California
337|Zuni Tribe of the Zuni Reservation, New Mexico
34|Viejas (Baron Long) Group of Capitan Grande Band o
35|Catawba Indian Nation (aka Catawba Tribe of South
36|Cayuga Nation of New York
37|Cedarville Rancheria, California
38|Chemehuevi Indian Tribe of the Chemehuevi Reservat
39|Cher-Ae Heights Indian Community of the Trinidad R
4|Alabama-Coushatta Tribes of Texas
40|Cherokee Nation, Oklahoma
41|Cheyenne-Arapaho Tribes of Oklahoma
42|Cheyenne River Sioux Tribe of the Cheyenne River
43|Chickasaw Nation, Oklahoma
44|Chicken Ranch Rancheria of Me-Wuk Indians of Calif
45|Chippewa-Cree Indians of the Rocky Boy's Reservati
46|Chitimacha Tribe of Louisiana
47|Choctaw Nation of Oklahoma
48|Citizen Potawatomi Nation, Oklahoma
49|Cloverdale Rancheria of Pomo Indians of California
5|Alabama-Quassarte Tribal Town, Oklahoma
50|Cocopah Tribe of Arizona
51|Coeur D'Alene Tribe of the Coeur D'Alene Reservati
52|Cold Springs Rancheria of Mono Indians of Californ
53|Colorado River Indian Tribes of the Colorado River
54|Comanche Indian Tribe, Oklahoma
55|Confederated Salish & Kootenai Tribes of the Flath
56|Confederated Tribes of the Chehalis Reservation, W
57|Confederated Tribes of the Colville Reservation, W
58|Confederated Tribes of the Coos, Lower Umpqua and
59|Confederated Tribes of the Goshute Reservation, Ne
6|Alturas Indian Rancheria, California
60|Confederated Tribes of the Grand Ronde Community o
61|Confederated Tribes of the Siletz Reservation, Ore
62|Confederated Tribes of the Umatilla Reservation, O
63|Confederated Tribes of the Warm Springs Reservatio
64|Confederated Tribes and Bands of the Yakama Indian
65|Coquille Tribe of Oregon
66|Cortina Indian Rancheria of Wintun Indians of Cali
67|Coushatta Tribe of Louisiana
68|Cow Creek Band of Umpqua Indians of Oregon
69|Coyote Valley Band of Pomo Indians of California
7|Apache Tribe of Oklahoma
70|Crow Tribe of Montana
71|Crow Creek Sioux Tribe of the Crow Creek Reservati
72|Cuyapaipe Community of Diegueno Mission Indians of
73|Death Valley Timbi-Sha Shoshone Band of California
74|Delaware Nation, Oklahoma (formerly Delaware Tribe
75|Delaware Tribe of Indians, Oklahoma
76|Dry Creek Rancheria of Pomo Indians of California
77|Duckwater Shoshone Tribe of the Duckwater Reservat
78|Eastern Band of Cherokee Indians of North Carolina
79|Eastern Shawnee Tribe of Oklahoma
8|Arapahoe Tribe of the Wind River Reservation, Wyom
80|Elem Indian Colony of Pomo Indians of the Sulphur
81|Elk Valley Rancheria, California
82|Ely Shoshone Tribe of Nevada
83|Enterprise Rancheria of Maidu Indians of Californi
84|Flandreau Santee Sioux Tribe of South Dakota
85|Forest County Potawatomi Community of Wisconsin Po
86|Fort Belknap Indian Community of the Fort Belknap
87|Fort Bidwell Indian Community of the Fort Bidwell
88|Fort Independence Indian Community of Paiute India
89|Fort McDermitt Paiute and Shoshone Tribes of the F
9|Aroostook Band of Micmac Indians of Maine
90|Fort McDowell Yavapai Nation, Arizona (formerly th
91|Fort Mojave Indian Tribe of Arizona, California
92|Fort Sill Apache Tribe of Oklahoma
93|Gila River Indian Community of the Gila River Indi
94|Grand Traverse Band of Ottawa & Chippewa Indians o
95|Graton Rancheria, California
96|Greenville Rancheria of Maidu Indians of Californi
97|Grindstone Indian Rancheria of Wintun-Wailaki Indi
98|Guidiville Rancheria of California
99|Hannahville Indian Community of Wisconsin Potawato

**Documentation**:

The tribal citizenship of the patient using the TribalEntityUS (OID 2.16.840.1.113883.5.140) table

---

**Name**: patientZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: placer_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-4-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.4.3)

**Cardinality**: [0..1]

**Documentation**:

The CLIA of the order placer

---

**Name**: placer_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.2)
- [ORC-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.2)
- [ORC-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.4.2)

**Cardinality**: [0..1]

**Documentation**:

The name of the placer of the lab order

---

**Name**: placer_order_group_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: placer_order_id

**Type**: ID

**PII**: No

**HL7 Fields**

- [OBR-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.1)
- [ORC-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.1)

**Cardinality**: [0..1]

**Documentation**:

The ID number of the lab order from the placer

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

**Name**: correctedTestId

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

pointer/link to the unique id of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the message_id of the prior item.

---

**Name**: previousTestDate

**Type**: DATE

**PII**: No

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
82334004|Indeterminate

**Documentation**:

Custom field.  Example - 260415000

---

**Name**: prime_patient_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: prime_patient_id_assigner

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

---

**Name**: processingModeCode

**Type**: CODE

**PII**: No

**Default Value**: P

**Cardinality**: [1..1]

**Value Sets**

Code | Display
---- | -------
D|Debugging
P|Production
T|Training

**Documentation**:

User can leave this field out if its production data, and we'll default to 'P'

---

**Name**: reason_for_study

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: receiving_application

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The receiving application for the message (specified by the receiver)

---

**Name**: receiving_facility

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The receiving facility for the message (specified by the receiver)

---

**Name**: reference_range

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.

---

**Name**: reporting_facility

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The reporting facility for the message, as specified by the receiver. This is typically used if PRIME is the
aggregator


---

**Name**: reporting_facility_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [MSH-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/MSH.4.2)
- [PID-3-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.2)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)
- [SPM-2-1-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.1.3)
- [SPM-2-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2.3)

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's CLIA

---

**Name**: reporting_facility_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [MSH-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/MSH.4.1)
- [PID-3-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.1)
- [PID-3-6-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.1)
- [SPM-2-1-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.1.2)
- [SPM-2-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2.2)

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's name

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

**Name**: result_format

**Type**: TEXT

**PII**: No

**Default Value**: CWE

**Cardinality**: [0..1]

---

**Name**: senderId

**Type**: TEXT

**PII**: No

**Default Value**: ManualUpload

**Cardinality**: [1..1]

**Documentation**:

Required. User should place their sender organization name in this field.

---

**Name**: sending_application

**Type**: HD

**PII**: No

**Default Value**: CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO

**Cardinality**: [0..1]

**Documentation**:

The name and OID for the application sending information to the receivers


---

**Name**: congregateResidentType

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
22232009|Hospital
2081004|Hospital ship
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
285113009|Religious institutional residence
285141008|Work (environment)
32911000|Homeless
261665006|Unknown

**Documentation**:

Custom field

---

**Name**: specimenCollectedDate

**Type**: DATETIME

**PII**: No

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [0..1]

**Documentation**:

eg, 20210113

---

**Name**: specimen_collection_method

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
ANP|Plates, Anaerobic
BAP|Plates, Blood Agar
BCAE|Blood Culture, Aerobic Bottle
BCAN|Blood Culture, Anaerobic Bottle
BCPD|Blood Culture, Pediatric Bottle
BIO|Biopsy
CAP|Capillary Specimen
CATH|Catheterized
CVP|Line, CVP
EPLA|Environmental, Plate
ESWA|Environmental, Swab
FNA|Aspiration, Fine Needle
KOFFP|Plate, Cough
LNA|Line, Arterial
LNV|Line, Venous
MARTL|Martin-Lewis Agar
ML11|Mod. Martin-Lewis Agar
MLP|Plate, Martin-Lewis
NYP|Plate, New York City
PACE|Pace, Gen-Probe
PIN|Pinworm Prep
PNA|Aterial puncture
PRIME|Pump Prime
PUMP|Pump Specimen
QC5|Quality Control For Micro
SCLP|Scalp, Fetal Vein
SCRAPS|Scrapings
SHA|Shaving
SWA|Swab
SWD|Swab, Dacron tipped
TMAN|Transport Media, Anaerobic
TMCH|Transport Media, Chalamydia
TMM4|Transport Media, M4
TMMY|Transport Media, Mycoplasma
TMOT|Transport Media
TMP|Plate, Thayer-Martin
TMPV|Transport Media, PVA
TMSC|Transport Media, Stool Culture
TMUP|Transport Media, Ureaplasma
TMVI|Transport Media, Viral
VENIP|Venipuncture
WOOD|Swab, Wooden Shaft

---

**Name**: specimen_collection_site

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.10) 

---

**Name**: specimen_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14](https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14) 

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

User does not need to specify this.  We'll copy the value from the testId if none is provided here.

---

**Name**: specimen_role

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
B|Blind sample
E|Electronic QC
F|Filer
G|Group
L|Pool
O|Operator proficiency
P|Patient
Q|Control specimen
R|Replicate
V|Verifying collaborator

---

**Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
119297000|Blood specimen (specimen)
71836000|Nasopharyngeal structure (body structure)
45206002|Nasal structure (body structure)
53342003|Internal nose structure (body structure)
29092000|Venous structure (body structure)

**Documentation**:

Refers back to the specimen source site, which is then encoded into the SPM-8 segment

---

**Name**: specimenSource

**Type**: CODE

**PII**: No

**Cardinality**: [1..1]

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
119297000|Blood specimen
122554006|Capillary blood specimen

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

**PII**: No

**Default Value**: LN

**Cardinality**: [0..1]

**Documentation**:

User does not need to specify this.   We'll set it to LN

---

**Name**: deviceIdentifier

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Testkit Name ID

**Documentation**:

Optional; we'll fill in if blank.  If filled in, must match a value from LIVD column M, "Test Kit Name ID"

---

**Name**: test_kit_name_id_cwe_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: LOINC Version ID

**Documentation**:

Follows guidance for OBX-17-7 where the version of the CWE field is passed along

---

**Name**: test_kit_name_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Testkit Name ID Type

---

**Name**: test_method_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

A text field that allows the lab to provide more information aboout the test method

---

**Name**: testPerformed

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Test Performed LOINC Code

**Documentation**:

User does not need to specify this.  It'll get filled in automatically.

---

**Name**: test_performed_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: test_performed_system

**Type**: TEXT

**PII**: No

**Default Value**: LOINC

**Cardinality**: [0..1]

---

**Name**: test_performed_system_abbr

**Type**: TEXT

**PII**: No

**Default Value**: LN

**Cardinality**: [0..1]

---

**Name**: test_performed_system_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: LOINC Version ID

---

**Name**: testResult

**Type**: CODE

**PII**: No

**Cardinality**: [1..1]

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
82334004|Indeterminate

**Documentation**:

Specify a code.  For example, 260373001

---

**Name**: testResultDate

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

eg, 20210111

---

**Name**: testResultStatus

**Type**: CODE

**PII**: No

**Default Value**: F

**HL7 Fields**

- [OBR-25-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.25.1)
- [OBX-11-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.11.1)

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Some, but not all, results available
C|Corrected, final
F|Final results
I|No results available; specimen received, procedure incomplete
M|Corrected, not final
N|Procedure completed, results pending
O|Order received; specimen not yet received
P|Preliminary
R|Results stored; not yet verified
S|No results available; procedure scheduled, but not done
X|No results available; Order canceled
Y|No order on record for this test
Z|No record of this patient

**Documentation**:

User does not need to specify this.  It'll get filled in automatically

---

**Name**: test_result_sub_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_result_units

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The units the test result is measured in.

---

**Name**: testing_lab_accession_number

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The accession number of the specimen collected

---

**Name**: performingFacilityCity

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the testing lab

---

**Name**: performingFacilityClia

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

**Name**: testing_lab_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

**Documentation**:

The country for the testing lab. Currently defaults to USA

---

**Name**: performingFacilityCounty

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

**Documentation**:

The text value for the testing lab county. This is used to do the lookup in the FIPS dataset.

---

**Name**: testing_lab_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:

The county code for the testing lab from the FIPS dataset. This is the standard code used in ELR reporting.


---

**Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Typically this will be the same as the `testing_lab_clia`, but potentially could not be.

---

**Name**: testing_lab_id_assigner

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This is the assigner of the CLIA for the testing lab. If the testing lab has a CLIA, this field will be filled in.

---

**Name**: performingFacilityName

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.2)
- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [OBX-15-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.2)
- [OBX-23-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.1)
- [ORC-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)
- [PID-3-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.1)

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: performingFacilityPhone

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The phone number of the testing lab

---

**Name**: testing_lab_specimen_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The specimen-id from the testing lab

---

**Name**: specimenReceivedDate

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Optional. User does not need to specify this.  We'll fill it in from the specimenCollectedDate

---

**Name**: performingFacilityState

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state for the testing lab

---

**Name**: performingFacilityStreet

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The street address for the testing lab

---

**Name**: performingFacilityStreet2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Street 2 field for the testing lab

---

**Name**: performingFacilityZip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---

**Name**: value_type

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
AD|Address
CE|Coded Entry
CF|Coded Element With Formatted Values
CK|Composite ID With Check Digit
CN|Composite ID And Name
CP|Composite Price
CWE|Coded With Exceptions
CX|Extended Composite ID With Check Digit
DT|Date
ED|Encapsulated Data
FT|Formatted Text (Display)
MO|Money
NM|Numeric
PN|Person Name
RP|Reference Pointer
SN|Structured Numeric
ST|String Data
TM|Time
TN|Telephone Number
TS|Time Stamp (Date & Time)
TX|Text Data (Display)
XAD|Extended Address
XCN|Extended Composite Name and Number For Persons
XON|Extended Composite Name and Number For Organizations
XPN|Extended Person Name
XTN|Extended Telecommunications Number

---
