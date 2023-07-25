### Schema: direct/genabio-covid-19
### Topic: covid-19
### Tracking Element: patient_id (patient_id)
### Base On: [covid-19](./covid-19.md)
### Extends: [direct/direct-covid-19](./direct-direct-covid-19.md)
#### Description: Genabio

---

**Name**: accession_number

**ReportStream Internal Name**: accession_number

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: comment

**ReportStream Internal Name**: comment

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
eg, 20210112
---

**Name**: employed_in_healthcare

**ReportStream Internal Name**: employed_in_healthcare

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:
Override the base hl70136 valueset with a custom one, to handle slightly different syntax
---

**Name**: serialNumber

**ReportStream Internal Name**: equipment_instance_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Barcode or QR code.  Unique within one manufacturer.
---

**Name**: equipment_model_name

**ReportStream Internal Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Model

**Documentation**:
Required.  Must match LIVD column B, "Model". eg,  "BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B"
---

**Name**: firstTest

**ReportStream Internal Name**: first_test

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
N|NO|LOCAL
UNK|UNK|LOCAL

**Documentation**:
Override the base hl70136 valueset with a custom one, to handle slightly different syntax
---

**Name**: healthcareEmployeeType

**ReportStream Internal Name**: healthcare_employee_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
1421009|Specialized surgeon|SNOMED_CT
3430008|Radiation therapist|SNOMED_CT
3842006|Chiropractor|SNOMED_CT
4162009|Dental assistant|SNOMED_CT
5275007|NA - Nursing auxiliary|SNOMED_CT
6816002|Specialized nurse|SNOMED_CT
6868009|Hospital administrator|SNOMED_CT
8724009|Plastic surgeon|SNOMED_CT
11661002|Neuropathologist|SNOMED_CT
11911009|Nephrologist|SNOMED_CT
11935004|Obstetrician|SNOMED_CT
13580004|School dental assistant|SNOMED_CT
14698002|Medical microbiologist|SNOMED_CT
17561000|Cardiologist|SNOMED_CT
18803008|Dermatologist|SNOMED_CT
18850004|Laboratory hematologist|SNOMED_CT
19244007|Gerodontist|SNOMED_CT
20145008|Removable prosthodontist|SNOMED_CT
21365001|Specialized dentist|SNOMED_CT
21450003|Neuropsychiatrist|SNOMED_CT
22515006|Medical assistant|SNOMED_CT
22731001|Orthopedic surgeon|SNOMED_CT
22983004|Thoracic surgeon|SNOMED_CT
23278007|Community health physician|SNOMED_CT
24430003|Physical medicine specialist|SNOMED_CT
24590004|Urologist|SNOMED_CT
25961008|Electroencephalography specialist|SNOMED_CT
26042002|Dental hygienist|SNOMED_CT
26369006|Public health nurse|SNOMED_CT
28229004|Optometrist|SNOMED_CT
28411006|Neonatologist|SNOMED_CT
28544002|Chemical pathologist|SNOMED_CT
36682004|PT - Physiotherapist|SNOMED_CT
37154003|Periodontist|SNOMED_CT
37504001|Orthodontist|SNOMED_CT
39677007|Internal medicine specialist|SNOMED_CT
40127002|Dietitian (general)|SNOMED_CT
40204001|Hematologist|SNOMED_CT
40570005|Interpreter|SNOMED_CT
41672002|Respiratory physician|SNOMED_CT
41904004|Medical X-ray technician|SNOMED_CT
43702002|Occupational health nurse|SNOMED_CT
44652006|Pharmaceutical assistant|SNOMED_CT
45419001|Masseur|SNOMED_CT
45440000|Rheumatologist|SNOMED_CT
45544007|Neurosurgeon|SNOMED_CT
45956004|Sanitarian|SNOMED_CT
46255001|Pharmacist|SNOMED_CT
48740002|Philologist|SNOMED_CT
49203003|Dispensing optometrist|SNOMED_CT
49993003|Maxillofacial surgeon|SNOMED_CT
50149000|Endodontist|SNOMED_CT
54503009|Faith healer|SNOMED_CT
56397003|Neurologist|SNOMED_CT
56466003|Community physician|SNOMED_CT
56542007|Medical record administrator|SNOMED_CT
56545009|Cardiovascular surgeon|SNOMED_CT
57654006|Fixed prosthodontist|SNOMED_CT
59058001|General physician|SNOMED_CT
59169001|Orthopedic technician|SNOMED_CT
59944000|Psychologist|SNOMED_CT
60008001|Community-based dietitian|SNOMED_CT
61207006|Medical pathologist|SNOMED_CT
61246008|Laboratory medicine specialist|SNOMED_CT
61345009|Otorhinolaryngologist|SNOMED_CT
61894003|Endocrinologist|SNOMED_CT
62247001|Family medicine specialist|SNOMED_CT
63098009|Clinical immunologist|SNOMED_CT
66476003|Oral pathologist|SNOMED_CT
66862007|Radiologist|SNOMED_CT
68867008|Public health dentist|SNOMED_CT
68950000|Prosthodontist|SNOMED_CT
69280009|Specialized physician|SNOMED_CT
71838004|Gastroenterologist|SNOMED_CT
73265009|Nursing aid|SNOMED_CT
75271001|MW - Midwife|SNOMED_CT
76166008|Practical aid (pharmacy)|SNOMED_CT
76231001|Osteopath|SNOMED_CT
76899008|Infectious diseases physician|SNOMED_CT
78703002|General surgeon|SNOMED_CT
78729002|Diagnostic radiologist|SNOMED_CT
79898004|Auxiliary midwife|SNOMED_CT
80409005|Translator|SNOMED_CT
80546007|OT - Occupational therapist|SNOMED_CT
80584001|Psychiatrist|SNOMED_CT
80933006|Nuclear medicine physician|SNOMED_CT
81464008|Clinical pathologist|SNOMED_CT
82296001|Pediatrician|SNOMED_CT
83189004|Other professional nurse|SNOMED_CT
83273008|Anatomic pathologist|SNOMED_CT
83685006|Gynecologist|SNOMED_CT
85733003|General pathologist|SNOMED_CT
88189002|Anesthesiologist|SNOMED_CT
88475002|Other dietitians and public health nutritionists|SNOMED_CT
90201008|Pediatric dentist|SNOMED_CT
90655003|Care of the elderly physician|SNOMED_CT
106289002|Dental surgeon|SNOMED_CT
106291005|Dietician AND/OR public health nutritionist|SNOMED_CT
106292003|Nurse|SNOMED_CT
106293008|Nursing personnel|SNOMED_CT
106294002|Midwifery personnel|SNOMED_CT
106296000|Physiotherapist AND/OR occupational therapist|SNOMED_CT
106330007|Philologist, translator AND/OR interpreter|SNOMED_CT
112247003|Medical doctor|SNOMED_CT
158965000|Medical practitioner|SNOMED_CT
158966004|Medical administrator - national|SNOMED_CT
158967008|Consultant physician|SNOMED_CT
158968003|Consultant surgeon|SNOMED_CT
158969006|Consultant gynecology and obstetrics|SNOMED_CT
158970007|Anesthetist|SNOMED_CT
158971006|Hospital registrar|SNOMED_CT
158972004|House officer|SNOMED_CT
158973009|Occupational physician|SNOMED_CT
158974003|Clinical medical officer|SNOMED_CT
158975002|Medical practitioner - teaching|SNOMED_CT
158977005|Dental administrator|SNOMED_CT
158978000|Dental consultant|SNOMED_CT
158979008|Dental general practitioner|SNOMED_CT
158980006|Dental practitioner - teaching|SNOMED_CT
158983008|Nurse administrator - national|SNOMED_CT
158984002|Nursing officer - region|SNOMED_CT
158985001|Nursing officer - district|SNOMED_CT
158986000|Nursing administrator - professional body|SNOMED_CT
158987009|Nursing officer - division|SNOMED_CT
158988004|Nurse education director|SNOMED_CT
158989007|Occupational health nursing officer|SNOMED_CT
158990003|Nursing officer|SNOMED_CT
158992006|Midwifery sister|SNOMED_CT
158993001|Nursing sister (theatre)|SNOMED_CT
158994007|Staff nurse|SNOMED_CT
158995008|Staff midwife|SNOMED_CT
158996009|State enrolled nurse|SNOMED_CT
158997000|District nurse|SNOMED_CT
158998005|Private nurse|SNOMED_CT
158999002|Community midwife|SNOMED_CT
159001001|Clinic nurse|SNOMED_CT
159002008|Practice nurse|SNOMED_CT
159003003|School nurse|SNOMED_CT
159004009|Nurse - teaching|SNOMED_CT
159005005|Student nurse|SNOMED_CT
159006006|Dental nurse|SNOMED_CT
159007002|Community pediatric nurse|SNOMED_CT
159010009|Hospital pharmacist|SNOMED_CT
159011008|Retail pharmacist|SNOMED_CT
159012001|Industrial pharmacist|SNOMED_CT
159013006|Pharmaceutical officer H.A.|SNOMED_CT
159014000|Trainee pharmacist|SNOMED_CT
159016003|Medical radiographer|SNOMED_CT
159017007|Diagnostic radiographer|SNOMED_CT
159018002|Therapeutic radiographer|SNOMED_CT
159019005|Trainee radiographer|SNOMED_CT
159021000|Ophthalmic optician|SNOMED_CT
159022007|Trainee optician|SNOMED_CT
159025009|Remedial gymnast|SNOMED_CT
159026005|Speech and language therapist|SNOMED_CT
159027001|Orthoptist|SNOMED_CT
159028006|Trainee remedial therapist|SNOMED_CT
159033005|Dietician|SNOMED_CT
159034004|Podiatrist|SNOMED_CT
159035003|Dental auxiliary|SNOMED_CT
159036002|ECG technician|SNOMED_CT
159037006|EEG technician|SNOMED_CT
159038001|Artificial limb fitter|SNOMED_CT
159039009|AT - Audiology technician|SNOMED_CT
159040006|Pharmacy technician|SNOMED_CT
159041005|Trainee medical technician|SNOMED_CT
159141008|Geneticist|SNOMED_CT
159972006|Surgical corset fitter|SNOMED_CT
160008000|Dental technician|SNOMED_CT
224529009|Clinical assistant|SNOMED_CT
224530004|Senior registrar|SNOMED_CT
224531000|Registrar|SNOMED_CT
224532007|Senior house officer|SNOMED_CT
224533002|MO - Medical officer|SNOMED_CT
224534008|Health visitor, nurse/midwife|SNOMED_CT
224535009|Registered nurse|SNOMED_CT
224536005|Midwifery tutor|SNOMED_CT
224537001|Accident and Emergency nurse|SNOMED_CT
224538006|Triage nurse|SNOMED_CT
224540001|Community nurse|SNOMED_CT
224541002|Nursing continence advisor|SNOMED_CT
224542009|Coronary care nurse|SNOMED_CT
224543004|Diabetic nurse|SNOMED_CT
224544005|Family planning nurse|SNOMED_CT
224545006|Care of the elderly nurse|SNOMED_CT
224546007|ICN - Infection control nurse|SNOMED_CT
224547003|Intensive therapy nurse|SNOMED_CT
224548008|Learning disabilities nurse|SNOMED_CT
224549000|Neonatal nurse|SNOMED_CT
224550000|Neurology nurse|SNOMED_CT
224551001|Industrial nurse|SNOMED_CT
224552008|Oncology nurse|SNOMED_CT
224553003|Macmillan nurse|SNOMED_CT
224554009|Marie Curie nurse|SNOMED_CT
224555005|Pain control nurse|SNOMED_CT
224556006|Palliative care nurse|SNOMED_CT
224557002|Chemotherapy nurse|SNOMED_CT
224558007|Radiotherapy nurse|SNOMED_CT
224559004|PACU nurse|SNOMED_CT
224560009|Stomatherapist|SNOMED_CT
224561008|Theatre nurse|SNOMED_CT
224562001|Pediatric nurse|SNOMED_CT
224563006|Psychiatric nurse|SNOMED_CT
224564000|Community mental health nurse|SNOMED_CT
224565004|Renal nurse|SNOMED_CT
224566003|Hemodialysis nurse|SNOMED_CT
224567007|Wound care nurse|SNOMED_CT
224569005|Nurse grade|SNOMED_CT
224570006|Clinical nurse specialist|SNOMED_CT
224571005|Nurse practitioner|SNOMED_CT
224572003|Nursing sister|SNOMED_CT
224573008|CN - Charge nurse|SNOMED_CT
224574002|Ward manager|SNOMED_CT
224575001|Nursing team leader|SNOMED_CT
224576000|Nursing assistant|SNOMED_CT
224577009|Healthcare assistant|SNOMED_CT
224578004|Nursery nurse|SNOMED_CT
224579007|Healthcare service manager|SNOMED_CT
224580005|Occupational health service manager|SNOMED_CT
224581009|Community nurse manager|SNOMED_CT
224583007|Behavior therapist|SNOMED_CT
224584001|Behavior therapy assistant|SNOMED_CT
224585000|Drama therapist|SNOMED_CT
224586004|Domiciliary occupational therapist|SNOMED_CT
224587008|Occupational therapy helper|SNOMED_CT
224588003|Psychotherapist|SNOMED_CT
224589006|Community-based physiotherapist|SNOMED_CT
224590002|Play therapist|SNOMED_CT
224591003|Play specialist|SNOMED_CT
224592005|Play leader|SNOMED_CT
224593000|Community-based speech/language therapist|SNOMED_CT
224594006|Speech/language assistant|SNOMED_CT
224595007|Professional counselor|SNOMED_CT
224596008|Marriage guidance counselor|SNOMED_CT
224597004|Trained nurse counselor|SNOMED_CT
224598009|Trained social worker counselor|SNOMED_CT
224599001|Trained personnel counselor|SNOMED_CT
224600003|Psychoanalyst|SNOMED_CT
224601004|Assistant psychologist|SNOMED_CT
224602006|Community-based podiatrist|SNOMED_CT
224603001|Foot care worker|SNOMED_CT
224604007|Audiometrician|SNOMED_CT
224605008|Audiometrist|SNOMED_CT
224606009|Technical healthcare occupation|SNOMED_CT
224607000|Occupational therapy technical instructor|SNOMED_CT
224608005|Administrative healthcare staff|SNOMED_CT
224609002|Complementary health worker|SNOMED_CT
224610007|Supporting services personnel|SNOMED_CT
224614003|Research associate|SNOMED_CT
224615002|Research nurse|SNOMED_CT
224620002|Human aid to communication|SNOMED_CT
224621003|Palantypist|SNOMED_CT
224622005|Note taker|SNOMED_CT
224623000|Cuer|SNOMED_CT
224624006|Lipspeaker|SNOMED_CT
224625007|Interpreter for British sign language|SNOMED_CT
224626008|Interpreter for Signs supporting English|SNOMED_CT
224936003|General practitioner locum|SNOMED_CT
225726006|Lactation consultant|SNOMED_CT
225727002|Midwife counselor|SNOMED_CT
265937000|Nursing occupation|SNOMED_CT
265939002|Medical/dental technicians|SNOMED_CT
283875005|Parkinson disease nurse|SNOMED_CT
302211009|Specialist registrar|SNOMED_CT
303124005|Member of mental health review tribunal|SNOMED_CT
303129000|Hospital manager|SNOMED_CT
303133007|Responsible medical officer|SNOMED_CT
303134001|Independent doctor|SNOMED_CT
304291006|Bereavement counselor|SNOMED_CT
304292004|Surgeon|SNOMED_CT
307988006|Medical technician|SNOMED_CT
308002005|Remedial therapist|SNOMED_CT
309294001|Accident and Emergency doctor|SNOMED_CT
309295000|Clinical oncologist|SNOMED_CT
309296004|Family planning doctor|SNOMED_CT
309322005|Associate general practitioner|SNOMED_CT
309323000|Partner of general practitioner|SNOMED_CT
309324006|Assistant GP|SNOMED_CT
309326008|Deputizing general practitioner|SNOMED_CT
309327004|General practitioner registrar|SNOMED_CT
309328009|Ambulatory pediatrician|SNOMED_CT
309329001|Community pediatrician|SNOMED_CT
309330006|Pediatric cardiologist|SNOMED_CT
309331005|Pediatric endocrinologist|SNOMED_CT
309332003|Pediatric gastroenterologist|SNOMED_CT
309333008|Pediatric nephrologist|SNOMED_CT
309334002|Pediatric neurologist|SNOMED_CT
309335001|Pediatric rheumatologist|SNOMED_CT
309336000|Pediatric oncologist|SNOMED_CT
309337009|Pain management specialist|SNOMED_CT
309338004|Intensive care specialist|SNOMED_CT
309339007|Adult intensive care specialist|SNOMED_CT
309340009|Pediatric intensive care specialist|SNOMED_CT
309341008|Blood transfusion doctor|SNOMED_CT
309342001|Histopathologist|SNOMED_CT
309343006|Physician|SNOMED_CT
309345004|Chest physician|SNOMED_CT
309346003|Thoracic physician|SNOMED_CT
309347007|Clinical hematologist|SNOMED_CT
309348002|Clinical neurophysiologist|SNOMED_CT
309349005|Clinical physiologist|SNOMED_CT
309350005|Diabetologist|SNOMED_CT
309351009|Andrologist|SNOMED_CT
309352002|Neuroendocrinologist|SNOMED_CT
309353007|Reproductive endocrinologist|SNOMED_CT
309354001|Thyroidologist|SNOMED_CT
309355000|Clinical geneticist|SNOMED_CT
309356004|Clinical cytogeneticist|SNOMED_CT
309357008|Clinical molecular geneticist|SNOMED_CT
309358003|Genitourinary medicine physician|SNOMED_CT
309359006|Palliative care physician|SNOMED_CT
309360001|Rehabilitation physician|SNOMED_CT
309361002|Child and adolescent psychiatrist|SNOMED_CT
309362009|Forensic psychiatrist|SNOMED_CT
309363004|Liaison psychiatrist|SNOMED_CT
309364005|Psychogeriatrician|SNOMED_CT
309365006|Psychiatrist for mental handicap|SNOMED_CT
309366007|Rehabilitation psychiatrist|SNOMED_CT
309367003|Obstetrician and gynecologist|SNOMED_CT
309368008|Breast surgeon|SNOMED_CT
309369000|Cardiothoracic surgeon|SNOMED_CT
309371000|Cardiac surgeon|SNOMED_CT
309372007|Ear, nose and throat surgeon|SNOMED_CT
309373002|Endocrine surgeon|SNOMED_CT
309374008|Thyroid surgeon|SNOMED_CT
309375009|Pituitary surgeon|SNOMED_CT
309376005|Gastrointestinal surgeon|SNOMED_CT
309377001|General gastrointestinal surgeon|SNOMED_CT
309378006|Upper gastrointestinal surgeon|SNOMED_CT
309379003|Colorectal surgeon|SNOMED_CT
309380000|Hand surgeon|SNOMED_CT
309381001|Hepatobiliary surgeon|SNOMED_CT
309382008|Ophthalmic surgeon|SNOMED_CT
309383003|Pediatric surgeon|SNOMED_CT
309384009|Pancreatic surgeon|SNOMED_CT
309385005|Transplant surgeon|SNOMED_CT
309386006|Trauma surgeon|SNOMED_CT
309388007|Vascular surgeon|SNOMED_CT
309389004|Medical practitioner grade|SNOMED_CT
309390008|Hospital consultant|SNOMED_CT
309391007|Visiting specialist registrar|SNOMED_CT
309392000|Research registrar|SNOMED_CT
309393005|General practitioner grade|SNOMED_CT
309394004|General practitioner principal|SNOMED_CT
309395003|Hospital specialist|SNOMED_CT
309396002|Associate specialist|SNOMED_CT
309397006|Research fellow|SNOMED_CT
309398001|Allied health professional|SNOMED_CT
309399009|Hospital dietitian|SNOMED_CT
309400002|Domiciliary physiotherapist|SNOMED_CT
309401003|General practitioner-based physiotherapist|SNOMED_CT
309402005|Hospital-based physiotherapist|SNOMED_CT
309403000|Private physiotherapist|SNOMED_CT
309404006|Physiotherapy assistant|SNOMED_CT
309409001|Hospital-based speech and language therapist|SNOMED_CT
309410006|Arts therapist|SNOMED_CT
309411005|Dance therapist|SNOMED_CT
309412003|Music therapist|SNOMED_CT
309413008|Renal dietitian|SNOMED_CT
309414002|Liver dietitian|SNOMED_CT
309415001|Oncology dietitian|SNOMED_CT
309416000|Pediatric dietitian|SNOMED_CT
309417009|Diabetes dietitian|SNOMED_CT
309418004|Audiologist|SNOMED_CT
309419007|Hearing therapist|SNOMED_CT
309420001|Audiological scientist|SNOMED_CT
309421002|Hearing aid dispenser|SNOMED_CT
309422009|Community-based occupational therapist|SNOMED_CT
309423004|Hospital occupational therapist|SNOMED_CT
309427003|Social services occupational therapist|SNOMED_CT
309428008|Orthotist|SNOMED_CT
309429000|Surgical fitter|SNOMED_CT
309434001|Hospital-based podiatrist|SNOMED_CT
309435000|Podiatry assistant|SNOMED_CT
309436004|Lymphedema nurse|SNOMED_CT
309437008|Community learning disabilities nurse|SNOMED_CT
309439006|Clinical nurse teacher|SNOMED_CT
309440008|Community practice nurse teacher|SNOMED_CT
309441007|Nurse tutor|SNOMED_CT
309442000|Nurse teacher practitioner|SNOMED_CT
309443005|Nurse lecturer practitioner|SNOMED_CT
309444004|Outreach nurse|SNOMED_CT
309445003|Anesthetic nurse|SNOMED_CT
309446002|Nurse manager|SNOMED_CT
309450009|Nurse administrator|SNOMED_CT
309452001|Midwifery grade|SNOMED_CT
309453006|Midwife|SNOMED_CT
309454000|Student midwife|SNOMED_CT
309455004|Parentcraft sister|SNOMED_CT
309459005|Healthcare professional grade|SNOMED_CT
309460000|Restorative dentist|SNOMED_CT
310170009|Pediatric audiologist|SNOMED_CT
310171008|Immunopathologist|SNOMED_CT
310172001|Audiological physician|SNOMED_CT
310173006|Clinical pharmacologist|SNOMED_CT
310174000|Private doctor|SNOMED_CT
310175004|Agency nurse|SNOMED_CT
310176003|Behavioral therapist nurse|SNOMED_CT
310177007|Cardiac rehabilitation nurse|SNOMED_CT
310178002|Genitourinary nurse|SNOMED_CT
310179005|Rheumatology nurse specialist|SNOMED_CT
310180008|Continence nurse|SNOMED_CT
310181007|Contact tracing nurse|SNOMED_CT
310182000|General nurse|SNOMED_CT
310183005|Nurse for the mentally handicapped|SNOMED_CT
310184004|Liaison nurse|SNOMED_CT
310185003|Diabetic liaison nurse|SNOMED_CT
310186002|Nurse psychotherapist|SNOMED_CT
310187006|Company nurse|SNOMED_CT
310188001|Hospital midwife|SNOMED_CT
310189009|Genetic counselor|SNOMED_CT
310190000|Mental health counselor|SNOMED_CT
310191001|Clinical psychologist|SNOMED_CT
310192008|Educational psychologist|SNOMED_CT
310193003|Coroner|SNOMED_CT
310194009|Appliance officer|SNOMED_CT
310512001|Medical oncologist|SNOMED_CT
311441001|School medical officer|SNOMED_CT
312485001|Integrated midwife|SNOMED_CT
372102007|RN First Assist|SNOMED_CT
387619007|Optician|SNOMED_CT
394572006|Medical secretary|SNOMED_CT
394618009|Hospital nurse|SNOMED_CT
397824005|Consultant anesthetist|SNOMED_CT
397897005|Paramedic|SNOMED_CT
397903001|Staff grade obstetrician|SNOMED_CT
397908005|Staff grade practitioner|SNOMED_CT
398130009|Medical student|SNOMED_CT
398238009|Acting obstetric registrar|SNOMED_CT
404940000|Physiotherapist technical instructor|SNOMED_CT
405277009|Resident physician|SNOMED_CT
405278004|Certified registered nurse anesthetist|SNOMED_CT
405279007|Attending physician|SNOMED_CT
405623001|Assigned practitioner|SNOMED_CT
405684005|Professional initiating surgical case|SNOMED_CT
405685006|Professional providing staff relief during surgical procedure|SNOMED_CT
408798009|Consultant pediatrician|SNOMED_CT
408799001|Consultant neonatologist|SNOMED_CT
409974004|Health educator|SNOMED_CT
409975003|Certified health education specialist|SNOMED_CT
413854007|Circulating nurse|SNOMED_CT
415075003|Perioperative nurse|SNOMED_CT
415506007|Scrub nurse|SNOMED_CT
416160000|Fellow of American Academy of Osteopathy|SNOMED_CT
420409002|Oculoplastic surgeon|SNOMED_CT
420678001|Retinal surgeon|SNOMED_CT
421841007|Admitting physician|SNOMED_CT
422140007|Medical ophthalmologist|SNOMED_CT
422234006|Ophthalmologist|SNOMED_CT
432100008|Health coach|SNOMED_CT
442867008|Respiratory therapist|SNOMED_CT
443090005|Podiatric surgeon|SNOMED_CT
444912007|Hypnotherapist|SNOMED_CT
445313000|Asthma nurse specialist|SNOMED_CT
445451001|Nurse case manager|SNOMED_CT
446050000|PCP - Primary care physician|SNOMED_CT
446701002|Addiction medicine specialist|SNOMED_CT
449161006|PA - physician assistant|SNOMED_CT
471302004|Government midwife|SNOMED_CT
3981000175106|Nurse complex case manager|SNOMED_CT
231189271000087109|Naturopath|SNOMED_CT
236749831000087105|Prosthetist|SNOMED_CT
258508741000087105|Hip and knee surgeon|SNOMED_CT
260767431000087107|Hepatologist|SNOMED_CT
285631911000087106|Shoulder surgeon|SNOMED_CT
291705421000087106|Interventional radiologist|SNOMED_CT
341320851000087105|Pediatric radiologist|SNOMED_CT
368890881000087105|Emergency medicine specialist|SNOMED_CT
398480381000087106|Family medicine specialist - palliative care|SNOMED_CT
416186861000087101|Surgical oncologist|SNOMED_CT
450044741000087104|Acupuncturist|SNOMED_CT
465511991000087105|Pediatric orthopedic surgeon|SNOMED_CT
494782281000087101|Pediatric hematologist|SNOMED_CT
619197631000087102|Neuroradiologist|SNOMED_CT
623630151000087105|Family medicine specialist - anesthetist|SNOMED_CT
666997781000087107|Doula|SNOMED_CT
673825031000087109|Traditional herbal medicine specialist|SNOMED_CT
682131381000087105|Occupational medicine specialist|SNOMED_CT
724111801000087104|Pediatric emergency medicine specialist|SNOMED_CT
747936471000087102|Family medicine specialist - care of the elderly|SNOMED_CT
766788081000087100|Travel medicine specialist|SNOMED_CT
767205061000087108|Spine surgeon|SNOMED_CT
813758161000087106|Maternal or fetal medicine specialist|SNOMED_CT
822410621000087104|Massage therapist|SNOMED_CT
847240411000087102|Hospitalist|SNOMED_CT
853827051000087104|Sports medicine specialist|SNOMED_CT
926871431000087103|Pediatric respirologist|SNOMED_CT
954544641000087107|Homeopath|SNOMED_CT
956387501000087102|Family medicine specialist - emergency medicine|SNOMED_CT
969118571000087109|Pediatric hematologist or oncologist|SNOMED_CT
984095901000087105|Foot and ankle surgeon|SNOMED_CT
990928611000087105|Invasive cardiologist|SNOMED_CT
999480451000087102|Case manager|SNOMED_CT
999480461000087104|Kinesthesiologist|SNOMED_CT

**Documentation**:
Custom.  eg, 6816002
---

**Name**: hospitalized

**ReportStream Internal Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:
Override the base hl70136 valueset with a custom one, to handle slightly different syntax
---

**Name**: hospitalizedCode

**ReportStream Internal Name**: hospitalized_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
260373001|Detected|SNOMED_CT
260415000|Not detected|SNOMED_CT
720735008|Presumptive positive|SNOMED_CT
10828004|Positive|SNOMED_CT
42425007|Equivocal|SNOMED_CT
260385009|Negative|SNOMED_CT
895231008|Not detected in pooled specimen|SNOMED_CT
462371000124108|Detected in pooled specimen|SNOMED_CT
419984006|Inconclusive|SNOMED_CT
125154007|Specimen unsatisfactory for evaluation|SNOMED_CT
455371000124106|Invalid result|SNOMED_CT
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)|SNOMED_CT
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)|SNOMED_CT
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)|SNOMED_CT
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)|SNOMED_CT
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)|SNOMED_CT
373121007|Test not done|SNOMED_CT
82334004|Indeterminate|SNOMED_CT

**Documentation**:
Custom.  eg, 840539006, same valueset as testResult
---

**Name**: icu

**ReportStream Internal Name**: icu

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:
Override the base hl70136 valueset with a custom one, to handle slightly different syntax
---

**Name**: illness_onset_date

**ReportStream Internal Name**: illness_onset_date

**Type**: DATE

**PII**: No

**Cardinality**: [0..1]

---

**Name**: message_id

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
ReportStream copies value from the specimenId if none is provided by the sender.
---

**Name**: order_test_date

**ReportStream Internal Name**: order_test_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
eg, 20210108
---

**Name**: testOrdered

**ReportStream Internal Name**: ordered_test_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Ordered LOINC Code

**Documentation**:
eg, 94531-1
---

**Name**: testName

**ReportStream Internal Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Ordered LOINC Long Name

**Documentation**:
Should be the name that matches to Test Ordered LOINC Long Name, in LIVD table
---

**Name**: ordering_facility_city

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The city of the facility which the test was ordered from
---

**Name**: orderingFacilityCounty

**ReportStream Internal Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: orderingFacilityEmail

**ReportStream Internal Name**: ordering_facility_email

**Type**: EMAIL

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_name

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The name of the facility which the test was ordered from
---

**Name**: ordering_facility_phone_number

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The phone number of the facility which the test was ordered from
---

**Name**: ordering_facility_state

**ReportStream Internal Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
Note that many states expect this field to be available, or ReportStream is not able to route data to them.  Please provide if possible in order for us to route to as many states as possible.
---

**Name**: ordering_facility_street

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The address of the facility which the test was ordered from
---

**Name**: ordering_facility_street2

**ReportStream Internal Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The secondary address of the facility which the test was ordered from
---

**Name**: ordering_facility_zip_code

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The zip code of the facility which the test was ordered from
---

**Name**: ordering_provider_city

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The city of the provider
---

**Name**: ordering_provider_first_name

**ReportStream Internal Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.3)
- [ORC-12-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.3)

**Cardinality**: [0..1]

**Documentation**:
The first name of the provider who ordered the test
---

**Name**: ordering_provider_id

**ReportStream Internal Name**: ordering_provider_id

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [0..1]

**Documentation**:
eg, "1265050918"
---

**Name**: ordering_provider_last_name

**ReportStream Internal Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.2)
- [ORC-12-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.2)

**Cardinality**: [0..1]

**Documentation**:
The last name of provider who ordered the test
---

**Name**: ordering_provider_middle_name

**ReportStream Internal Name**: ordering_provider_middle_name

**Type**: TEXT_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_provider_phone_number

**ReportStream Internal Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [0..1]

**Documentation**:
The phone number of the provider
---

**Name**: orderingProviderPhoneArea

**ReportStream Internal Name**: ordering_provider_phone_number_area_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom.  Not currently used. ReportStream assumes area code is in orderingProviderPhone
---

**Name**: ordering_provider_state

**ReportStream Internal Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state of the provider
---

**Name**: ordering_provider_street

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The street address of the provider
---

**Name**: ordering_provider_street2

**ReportStream Internal Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The street second address of the provider
---

**Name**: ordering_provider_zip_code

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The zip code of the provider
---

**Name**: patientAge

**ReportStream Internal Name**: patient_age

**Type**: NUMBER

**PII**: Yes

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patientAgeUnits

**ReportStream Internal Name**: patient_age_units

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
mo|months|LOCAL
yr|years|LOCAL

**Documentation**:
Always filled when `patient_age` is filled
---

**Name**: patient_city

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's city
---

**Name**: patient_county

**ReportStream Internal Name**: patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_dob

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.

---

**Name**: patient_email

**ReportStream Internal Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_ethnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
H|Hispanic or Latino|LOCAL
H|Hispanic|LOCAL
H|Latino|LOCAL
H|Mex. Amer./Hispanic|LOCAL
H|2135-2|LOCAL
H|H|LOCAL
N|Non Hispanic or Latino|LOCAL
N|Non-Hispanic or Latino|LOCAL
N|Non Hispanic|LOCAL
N|Non-Hispanic|LOCAL
N|Not Hispanic or Latino|LOCAL
N|Not Hispanic|LOCAL
N|2186-5|LOCAL
N|N|LOCAL
U|Unknown|LOCAL
U|U|LOCAL
U|UNK|LOCAL
U|Black|LOCAL
U|White|LOCAL
U|African American|LOCAL
U|NULL|LOCAL
U|Patient Declines|LOCAL

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
H|2135-2|LOCAL
N|2186-5|LOCAL
U|UNK|LOCAL
U|ASKU|LOCAL

**Documentation**:
Internally, ReportStream uses hl70189 (H,N,U), but should use HHS values. (2135-2, 2186-5, UNK, ASKU). A mapping is done here, but better is to switch all of RS to HHS standard.
---

**Name**: patientEthnicityText

**ReportStream Internal Name**: patient_ethnicity_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom. ReportStream uses the patientEthnicity code, not this text value.
---

**Name**: patient_first_name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's first name
---

**Name**: patient_gender

**ReportStream Internal Name**: patient_gender

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
F|Female|LOCAL
F|Woman|LOCAL
F|F|LOCAL
M|Male|LOCAL
M|Man|LOCAL
M|M|LOCAL
U|U|LOCAL
U|UNK|LOCAL
U|UNKNOWN|LOCAL
O|O|LOCAL
O|Other|LOCAL
O|OTH|LOCAL
A|A|LOCAL
A|Ambiguous|LOCAL

**Documentation**:
The patient's gender. There is a valueset defined based on the values in PID-8-1, but downstream consumers are free to define their own accepted values. Please refer to the consumer-specific schema if you have questions.

---

**Name**: patient_id

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.

---

**Name**: patientUniqueIdHash

**ReportStream Internal Name**: patient_id_hash

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_last_name

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
Not required, but generally data will not flow to states if last/first name provided.
---

**Name**: patient_middle_name

**ReportStream Internal Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_phone_number

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's phone number with area code
---

**Name**: patientPhoneArea

**ReportStream Internal Name**: patient_phone_number_area_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom. Not currently used. ReportStream assumes area code is in patientPhone
---

**Name**: patient_preferred_language

**ReportStream Internal Name**: patient_preferred_language

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
abk|Abkhazian|ISO
abk|Abkhaz|ISO
ace|Achinese|ISO
ach|Acoli|ISO
ada|Adangme|ISO
ady|Adyghe; Adygei|ISO
aar|Afar|ISO
afh|Afrihili|ISO
afr|Afrikaans|ISO
afa|Afro-Asiatic (Other)|ISO
ain|Ainu|ISO
aka|Akan|ISO
akk|Akkadian|ISO
alb|Albanian|ISO
ale|Aleut|ISO
alg|Algonquian languages|ISO
tut|Altaic (Other)|ISO
amh|Amharic|ISO
anp|Angika|ISO
apa|Apache languages|ISO
ara|Arabic|ISO
arg|Aragonese|ISO
arp|Arapaho|ISO
arw|Arawak|ISO
arm|Armenian|ISO
rup|Aromanian; Arumanian; Macedo-Romanian|ISO
art|Artificial (Other)|ISO
asm|Assamese|ISO
aii|Assyrian|ISO
ast|Asturian; Bable; Leonese; Asturleonese|ISO
ath|Athapascan languages|ISO
aus|Australian languages|ISO
map|Austronesian (Other)|ISO
ava|Avaric|ISO
ave|Avestan|ISO
awa|Awadhi|ISO
aym|Aymara|ISO
aze|Azerbaijani|ISO
ban|Balinese|ISO
bat|Baltic (Other)|ISO
bal|Baluchi|ISO
bam|Bambara|ISO
bai|Bamileke languages|ISO
bad|Banda languages|ISO
bnt|Bantu (Other)|ISO
bas|Basa|ISO
bak|Bashkir|ISO
baq|Basque|ISO
btk|Batak languages|ISO
bej|Beja; Bedawiyet|ISO
bel|Belarusian|ISO
bem|Bemba|ISO
ben|Bengali|ISO
ber|Berber (Other)|ISO
bho|Bhojpuri|ISO
bih|Bihari|ISO
bik|Bikol|ISO
bin|Bini; Edo|ISO
bis|Bislama|ISO
byn|Blin; Bilin|ISO
zbl|Blissymbols; Blissymbolics; Bliss|ISO
nob|Norwegian Bokmål|ISO
bos|Bosnian|ISO
bra|Braj|ISO
bre|Breton|ISO
bug|Buginese|ISO
bul|Bulgarian|ISO
bua|Buriat|ISO
bur|Burmese|ISO
cad|Caddo|ISO
yue|Cantonese|ISO
yue|Cantonese; Chinese|ISO
cat|Catalan; Valencian|ISO
cat|Catalan|ISO
cau|Caucasian (Other)|ISO
ceb|Cebuano|ISO
cel|Celtic (Other)|ISO
cai|Central American Indian (Other)|ISO
khm|Central Khmer|ISO
chg|Chagatai|ISO
cmc|Chamic languages|ISO
cha|Chamorro|ISO
che|Chechen|ISO
chr|Cherokee|ISO
chy|Cheyenne|ISO
chb|Chibcha|ISO
nya|Chichewa; Chewa; Nyanja|ISO
chi|Chinese|ISO
chn|Chinook jargon|ISO
chp|Chipewyan; Dene Suline|ISO
cho|Choctaw|ISO
chk|Chuukese|ISO
chv|Chuvash|ISO
nwc|Classical Newari; Old Newari; Classical Nepal Bhasa|ISO
syc|Classical Syriac|ISO
cop|Coptic|ISO
cor|Cornish|ISO
cos|Corsican|ISO
cre|Cree|ISO
mus|Creek|ISO
crp|Creoles and pidgins (Other)|ISO
cpe|Creoles and pidgins English based (Other)|ISO
cpf|Creoles and pidgins French-based (Other)|ISO
cpp|Creoles and pidgins Portuguese-based (Other)|ISO
crh|Crimean Tatar; Crimean Turkish|ISO
hrv|Croatian|ISO
cus|Cushitic (Other)|ISO
cze|Czech|ISO
dak|Dakota|ISO
dan|Danish|ISO
dar|Dargwa|ISO
prs|Dari|ISO
del|Delaware|ISO
din|Dinka|ISO
div|Divehi; Dhivehi; Maldivian|ISO
div|Dhivehi|ISO
doi|Dogri|ISO
dgr|Dogrib|ISO
dra|Dravidian (Other)|ISO
dua|Duala|ISO
dut|Dutch; Flemish|ISO
dut|Dutch|ISO
dyu|Dyula|ISO
dzo|Dzongkha|ISO
frs|Eastern Frisian|ISO
efi|Efik|ISO
eka|Ekajuk|ISO
elx|Elamite|ISO
eng|English|ISO
myv|Erzya|ISO
epo|Esperanto|ISO
est|Estonian|ISO
ewe|Ewe|ISO
ewo|Ewondo|ISO
fan|Fang|ISO
fat|Fanti|ISO
fao|Faroese|ISO
fij|Fijian|ISO
fil|Filipino; Pilipino|ISO
fil|Filipino|ISO
fin|Finnish|ISO
fiu|Finno-Ugrian (Other)|ISO
fon|Fon|ISO
fre|French|ISO
fry|Frisian|ISO
fur|Friulian|ISO
ful|Fulah|ISO
gaa|Ga|ISO
gla|Gaelic; Scottish Gaelic|ISO
gla|Gaelic|ISO
car|Galibi Carib|ISO
glg|Galician|ISO
lug|Ganda|ISO
gay|Gayo|ISO
gba|Gbaya|ISO
gez|Geez|ISO
geo|Georgian|ISO
ger|German|ISO
gem|Germanic (Other)|ISO
gil|Gilbertese|ISO
gon|Gondi|ISO
gor|Gorontalo|ISO
got|Gothic|ISO
grb|Grebo|ISO
gre|Greek|ISO
gre|Greek Modern (1453-)|ISO
grn|Guarani|ISO
guj|Gujarati|ISO
gwi|Gwich'in|ISO
hai|Haida|ISO
hat|Haitian; Haitian Creole|ISO
hat|Haitian|ISO
hau|Hausa|ISO
haw|Hawaiian|ISO
heb|Hebrew|ISO
her|Herero|ISO
hil|Hiligaynon|ISO
him|Himachali|ISO
hin|Hindi|ISO
hmo|Hiri Motu|ISO
hit|Hittite|ISO
hmn|Hmong|ISO
hun|Hungarian|ISO
hup|Hupa|ISO
iba|Iban|ISO
ice|Icelandic|ISO
ido|Ido|ISO
ibo|Igbo|ISO
ijo|Ijo languages|ISO
ilo|Iloko|ISO
smn|Inari Sami|ISO
inc|Indic (Other)|ISO
ine|Indo-European (Other)|ISO
ind|Indonesian|ISO
inh|Ingush|ISO
ikt|Inuinnaqtun|ISO
ina|Interlingua (International Auxiliary Language Association)|ISO
ile|Interlingue; Occidental|ISO
iku|Inuktitut|ISO
ipk|Inupiaq|ISO
ira|Iranian (Other)|ISO
gle|Irish|ISO
iro|Iroquoian languages|ISO
ita|Italian|ISO
jpn|Japanese|ISO
jav|Javanese|ISO
cjy|Jinyu Chinese|ISO
jrb|Judeo-Arabic|ISO
jpr|Judeo-Persian|ISO
kbd|Kabardian|ISO
kab|Kabyle|ISO
kac|Kachin; Jingpho|ISO
kal|Kalaallisut; Greenlandic|ISO
xal|Kalmyk; Oirat|ISO
kam|Kamba|ISO
kan|Kannada|ISO
kau|Kanuri|ISO
krc|Karachay-Balkar|ISO
kaa|Kara-Kalpak|ISO
krl|Karelian|ISO
kar|Karen languages|ISO
kas|Kashmiri|ISO
csb|Kashubian|ISO
kaw|Kawi|ISO
kaz|Kazakh|ISO
kha|Khasi|ISO
khi|Khoisan (Other)|ISO
khm|Khmer|ISO
kho|Khotanese|ISO
kik|Kikuyu; Gikuyu|ISO
kmb|Kimbundu|ISO
kin|Kinyarwanda|ISO
kir|Kirghiz; Kyrgyz|ISO
kir|Kyrgyz|ISO
kir|Kirghiz|ISO
tlh|Klingon; tlhIngan-Hol|ISO
kom|Komi|ISO
kon|Kongo|ISO
kok|Konkani|ISO
kor|Korean|ISO
kos|Kosraean|ISO
kpe|Kpelle|ISO
kro|Kru languages|ISO
kua|Kuanyama; Kwanyama|ISO
kum|Kumyk|ISO
kur|Kurdish|ISO
kru|Kurukh|ISO
kut|Kutenai|ISO
lad|Ladino|ISO
lah|Lahnda|ISO
lam|Lamba|ISO
day|Land Dayak languages|ISO
lao|Lao|ISO
lat|Latin|ISO
lav|Latvian|ISO
lez|Lezghian|ISO
lim|Limburgan; Limburger; Limburgish|ISO
lin|Lingala|ISO
lit|Lithuanian|ISO
jbo|Lojban|ISO
nds|Low German; Low Saxon; German|ISO
dsb|Lower Sorbian|ISO
loz|Lozi|ISO
lub|Luba-Katanga|ISO
lua|Luba-Lulua|ISO
lui|Luiseno|ISO
smj|Lule Sami|ISO
lun|Lunda|ISO
luo|Luo (Kenya and Tanzania)|ISO
lus|Lushai|ISO
ltz|Luxembourgish; Letzeburgesch|ISO
ltz|Luxembourgish|ISO
mac|Macedonian|ISO
mad|Madurese|ISO
mag|Magahi|ISO
mai|Maithili|ISO
mak|Makasar|ISO
mlg|Malagasy|ISO
may|Malay|ISO
mal|Malayalam|ISO
mlt|Maltese|ISO
cmn|Mandarin|ISO
cmn|Mandarin Chinese|ISO
mnc|Manchu|ISO
mdr|Mandar|ISO
man|Mandingo|ISO
mni|Manipuri|ISO
mno|Manobo languages|ISO
glv|Manx|ISO
mao|Maori|ISO
arn|Mapudungun; Mapuche|ISO
mar|Marathi|ISO
chm|Mari|ISO
mah|Marshallese|ISO
mwr|Marwari|ISO
mas|Masai|ISO
myn|Mayan languages|ISO
myn|Mayan|ISO
men|Mende|ISO
mic|Mi'kmaq; Micmac|ISO
min|Minangkabau|ISO
mwl|Mirandese|ISO
moh|Mohawk|ISO
mdf|Moksha|ISO
mol|Moldavian; Moldovan|ISO
mol|Moldovan|ISO
mol|Moldavian|ISO
lol|Mongo|ISO
mon|Mongolian|ISO
mkh|Mon-Khmer (Other)|ISO
cnr|Montenegrin|ISO
mos|Mossi|ISO
mul|Multiple languages|ISO
mun|Munda languages|ISO
nah|Nahuatl languages|ISO
nau|Nauru|ISO
nav|Navajo; Navaho|ISO
nde|Ndebele|ISO
nde|Ndebele North; North Ndebele|ISO
nbl|Ndebele South; South Ndebele|ISO
ndo|Ndonga|ISO
nap|Neapolitan|ISO
new|Nepal Bhasa; Newari|ISO
nep|Nepali|ISO
nia|Nias|ISO
nic|Niger-Kordofanian (Other)|ISO
ssa|Nilo-Saharan (Other)|ISO
niu|Niuean|ISO
nqo|N'Ko|ISO
zxx|No linguistic content; Not applicable|ISO
nog|Nogai|ISO
nai|North American Indian|ISO
frr|Northern Frisian|ISO
sme|Northern Sami|ISO
nso|Northern Sotho|ISO
nor|Norwegian|ISO
nno|Norwegian Nynorsk; Nynorsk Norwegian|ISO
nno|Nynorsk|ISO
nub|Nubian languages|ISO
nym|Nyamwezi|ISO
nyn|Nyankole|ISO
nyo|Nyoro|ISO
nzi|Nzima|ISO
oci|Occitan (post 1500); Provençal|ISO
oci|Occitan|ISO
oji|Ojibwa|ISO
ori|Oriya|ISO
orm|Oromo|ISO
osa|Osage|ISO
oss|Ossetian; Ossetic|ISO
oto|Otomian languages|ISO
pal|Pahlavi|ISO
pau|Palauan|ISO
pli|Pali|ISO
pam|Pampanga; Kapampangan|ISO
pag|Pangasinan|ISO
pan|Panjabi|ISO
pap|Papiamento|ISO
paa|Papuan (Other)|ISO
pus|Pashto|ISO
pus|Pushto|ISO
pst|Central Pashto|ISO
pbt|Southern Pashto|ISO
pus|Pashto|ISO
nso|Pedi; Sepedi; Northern Sotho|ISO
per|Persian|ISO
phi|Philippine (Other)|ISO
phn|Phoenician|ISO
pon|Pohnpeian|ISO
pol|Polish|ISO
por|Portuguese|ISO
pra|Prakrit languages|ISO
pan|Punjabi|ISO
pus|Pushto; Pashto|ISO
que|Quechua|ISO
raj|Rajasthani|ISO
rap|Rapanui|ISO
rar|Rarotongan; Cook Islands Maori|ISO
qaa-qtz|Reserved for local use|ISO
roa|Romance (Other)|ISO
rum|Romanian|ISO
roh|Romansh|ISO
rom|Romany|ISO
run|Rundi|ISO
rus|Russian|ISO
sal|Salishan languages|ISO
sam|Samaritan Aramaic|ISO
smi|Sami languages (Other)|ISO
smo|Samoan|ISO
sad|Sandawe|ISO
sag|Sango|ISO
san|Sanskrit|ISO
sat|Santali|ISO
srd|Sardinian|ISO
sas|Sasak|ISO
sco|Scots|ISO
sel|Selkup|ISO
sem|Semitic (Other)|ISO
srp|Serbian|ISO
srr|Serer|ISO
shn|Shan|ISO
sna|Shona|ISO
iii|Sichuan Yi; Nuosu|ISO
scn|Sicilian|ISO
sid|Sidamo|ISO
sgn|Sign Languages|ISO
bla|Siksika|ISO
snd|Sindhi|ISO
sin|Sinhala; Sinhalese|ISO
sin|Sinhalese|ISO
sit|Sino-Tibetan (Other)|ISO
sio|Siouan languages|ISO
sms|Skolt Sami|ISO
den|Slave (Athapascan)|ISO
sla|Slavic (Other)|ISO
slo|Slovak|ISO
slv|Slovenian|ISO
slv|Slovene|ISO
sog|Sogdian|ISO
som|Somali|ISO
son|Songhai languages|ISO
snk|Soninke|ISO
wen|Sorbian languages|ISO
sot|Sotho Southern|ISO
sai|South American Indian (Other)|ISO
alt|Southern Altai|ISO
sma|Southern Sami|ISO
spa|Spanish; Castilian|ISO
spa|Spanish|ISO
srn|Sranan Tongo|ISO
suk|Sukuma|ISO
sux|Sumerian|ISO
sun|Sundanese|ISO
sus|Susu|ISO
swa|Swahili|ISO
ssw|Swati|ISO
swe|Swedish|ISO
gsw|Swiss German|ISO
gsw|Alemannic|ISO
gsw|Alsatian|ISO
syr|Syriac|ISO
tgl|Tagalog|ISO
tah|Tahitian|ISO
tai|Tai (Other)|ISO
tgk|Tajik|ISO
tmh|Tamashek|ISO
tam|Tamil|ISO
tat|Tatar|ISO
tel|Telugu|ISO
ter|Tereno|ISO
tet|Tetum|ISO
tha|Thai|ISO
tib|Tibetan|ISO
tig|Tigre|ISO
tir|Tigrinya|ISO
tem|Timne|ISO
tiv|Tiv|ISO
tli|Tlingit|ISO
tpi|Tok Pisin|ISO
tkl|Tokelau|ISO
tog|Tonga (Nyasa)|ISO
ton|Tonga (Tonga Islands)|ISO
tsi|Tsimshian|ISO
tso|Tsonga|ISO
tsn|Tswana|ISO
tum|Tumbuka|ISO
tup|Tupi languages|ISO
tur|Turkish|ISO
tuk|Turkmen|ISO
tvl|Tuvalu|ISO
tyv|Tuvinian|ISO
twi|Twi|ISO
udm|Udmurt|ISO
uga|Ugaritic|ISO
uig|Uighur|ISO
uig|Uyghur|ISO
ukr|Ukrainian|ISO
umb|Umbundu|ISO
mis|Uncoded languages|ISO
und|Undetermined|ISO
hsb|Upper Sorbian|ISO
urd|Urdu|ISO
uzb|Uzbek|ISO
vai|Vai|ISO
ven|Venda|ISO
vie|Vietnamese|ISO
vol|Volapük|ISO
vot|Votic|ISO
wak|Wakashan languages|ISO
wal|Walamo|ISO
wln|Walloon|ISO
war|Waray|ISO
was|Washo|ISO
wel|Welsh|ISO
fry|Western Frisian|ISO
wol|Wolof|ISO
xho|Xhosa|ISO
sah|Yakut|ISO
yao|Yao|ISO
yap|Yapese|ISO
yid|Yiddish|ISO
yor|Yoruba|ISO
ypk|Yupik languages|ISO
znd|Zande languages|ISO
zap|Zapotec|ISO
zza|Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki|ISO
zen|Zenaga|ISO
zha|Zhuang; Chuang|ISO
zul|Zulu|ISO
zun|Zuni|ISO

---

**Name**: patient_race

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
2106-3|White|LOCAL
2106-3|W|LOCAL
2106-3|Caucasian|LOCAL
2106-3|C|LOCAL
2106-3|2106-3|LOCAL
1002-5|American Indian or Alaska Native|LOCAL
1002-5|American Indian|LOCAL
1002-5|Native American|LOCAL
1002-5|1002-5|LOCAL
2054-5|Black or African American|LOCAL
2054-5|African American|LOCAL
2054-5|African American Alaska Native|LOCAL
2054-5|African American Black|LOCAL
2054-5|Black|LOCAL
2054-5|B|LOCAL
2054-5|2054-5|LOCAL
2076-8|Native Hawaiian or Other Pacific Islander|LOCAL
2076-8|Hawaiian|LOCAL
2076-8|NH|LOCAL
2076-8|2076-8|LOCAL
2131-1|Other|LOCAL
2131-1|OTH|LOCAL
2131-1|O|LOCAL
2131-1|Other Race|LOCAL
2131-1|Other Race White|LOCAL
2131-1|Other Race,White|LOCAL
2131-1|Other Race Black|LOCAL
2131-1|Other Race,Black|LOCAL
2131-1|2131-1|LOCAL
2028-9|Asian|LOCAL
2028-9|Asian Indian|LOCAL
2028-9|2028-9|LOCAL
UNK|Unknown|LOCAL
UNK|UNK|LOCAL
UNK|U|LOCAL
UNK|Patient Declines|LOCAL
UNK|null|LOCAL
ASKU|Asked, but unknown|LOCAL
ASKU|ASKU|LOCAL

**Documentation**:
The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.

---

**Name**: patientRaceText

**ReportStream Internal Name**: patient_race_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom.    ReportStream uses patientRace code, not this text value.
---

**Name**: patient_state

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
Extremely important field for routing data to states.
---

**Name**: patient_street

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's street address
---

**Name**: patient_street2

**ReportStream Internal Name**: patient_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's second address line
---

**Name**: patient_zip_code

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The patient's zip code
---

**Name**: pregnant

**ReportStream Internal Name**: pregnant

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:
Is the patient pregnant?
---

**Name**: pregnantText

**ReportStream Internal Name**: pregnant_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom.  ReportStream uses the 'pregnant' code, not this text value.
---

**Name**: correctedTestId

**ReportStream Internal Name**: previous_message_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
pointer/link to the unique id of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the message_id of the prior item.
---

**Name**: previousTestDate

**ReportStream Internal Name**: previous_test_date

**Type**: DATE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom field
---

**Name**: previousTestResult

**ReportStream Internal Name**: previous_test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
260373001|Detected|SNOMED_CT
260415000|Not detected|SNOMED_CT
720735008|Presumptive positive|SNOMED_CT
10828004|Positive|SNOMED_CT
42425007|Equivocal|SNOMED_CT
260385009|Negative|SNOMED_CT
895231008|Not detected in pooled specimen|SNOMED_CT
462371000124108|Detected in pooled specimen|SNOMED_CT
419984006|Inconclusive|SNOMED_CT
125154007|Specimen unsatisfactory for evaluation|SNOMED_CT
455371000124106|Invalid result|SNOMED_CT
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)|SNOMED_CT
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)|SNOMED_CT
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)|SNOMED_CT
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)|SNOMED_CT
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)|SNOMED_CT
373121007|Test not done|SNOMED_CT
82334004|Indeterminate|SNOMED_CT

**Documentation**:
Custom field.  Example - 260415000
---

**Name**: previousTestType

**ReportStream Internal Name**: previous_test_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom field. Note, value matched LIVD column "F", "Test Performed LOINC Code"
---

**Name**: processingModeCode

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: T

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
D|Debugging|HL7
P|Production|HL7
T|Training|HL7

**Documentation**:
P, D, or T for Production, Debugging, or Training
---

**Name**: reporting_facility_clia

**ReportStream Internal Name**: reporting_facility_clia

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

**ReportStream Internal Name**: reporting_facility_name

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

**Name**: residence_type

**ReportStream Internal Name**: residence_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
22232009|Hospital|SNOMED_CT
2081004|Hospital ship|SNOMED_CT
32074000|Long Term Care Hospital|SNOMED_CT
224929004|Secure Hospital|SNOMED_CT
42665001|Nursing Home|SNOMED_CT
30629002|Retirement Home|SNOMED_CT
74056004|Orphanage|SNOMED_CT
722173008|Prison-based care site|SNOMED_CT
20078004|Substance Abuse Treatment Center|SNOMED_CT
257573002|Boarding House|SNOMED_CT
224683003|Military Accommodation|SNOMED_CT
284546000|Hospice|SNOMED_CT
257628001|Hostel|SNOMED_CT
310207003|Sheltered Housing|SNOMED_CT
57656006|Penal Institution|SNOMED_CT
285113009|Religious institutional residence|SNOMED_CT
285141008|Work (environment)|SNOMED_CT
32911000|Homeless|SNOMED_CT
261665006|Unknown|SNOMED_CT

---

**Name**: resident_congregate_setting

**ReportStream Internal Name**: resident_congregate_setting

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:
Override the base hl70136 valueset with a custom one, to handle slightly different syntax
---

**Name**: senderId

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Default Value**: Genabio

**Cardinality**: [1..1]

**Documentation**:
ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.
---

**Name**: congregateResidentType

**ReportStream Internal Name**: site_of_care

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
22232009|Hospital|SNOMED_CT
2081004|Hospital ship|SNOMED_CT
32074000|Long Term Care Hospital|SNOMED_CT
224929004|Secure Hospital|SNOMED_CT
42665001|Nursing Home|SNOMED_CT
30629002|Retirement Home|SNOMED_CT
74056004|Orphanage|SNOMED_CT
722173008|Prison-based care site|SNOMED_CT
20078004|Substance Abuse Treatment Center|SNOMED_CT
257573002|Boarding House|SNOMED_CT
224683003|Military Accommodation|SNOMED_CT
284546000|Hospice|SNOMED_CT
257628001|Hostel|SNOMED_CT
310207003|Sheltered Housing|SNOMED_CT
57656006|Penal Institution|SNOMED_CT
285113009|Religious institutional residence|SNOMED_CT
285141008|Work (environment)|SNOMED_CT
32911000|Homeless|SNOMED_CT
261665006|Unknown|SNOMED_CT

**Documentation**:
Custom field
---

**Name**: specimen_collection_date

**ReportStream Internal Name**: specimen_collection_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: specimenCollectedDate

**ReportStream Internal Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [1..1]

**Documentation**:
eg, 20210113
---

**Name**: specimenId

**ReportStream Internal Name**: specimen_id

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

**Name**: specimen_type

**ReportStream Internal Name**: specimen_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
445297001|Swab of internal nose|SNOMED_CT
445297001|Nasal Swab|SNOMED_CT
445297001|Nasal|SNOMED_CT
445297001|Varied|SNOMED_CT
445297001|445297001|SNOMED_CT
258500001|Nasopharyngeal swab|SNOMED_CT
258500001|258500001|SNOMED_CT
871810001|Mid-turbinate nasal swab|SNOMED_CT
871810001|871810001|SNOMED_CT
697989009|Anterior nares swab|SNOMED_CT
697989009|Anterior nasal swab|SNOMED_CT
697989009|697989009|SNOMED_CT
258411007|Nasopharyngeal aspirate|SNOMED_CT
258411007|258411007|SNOMED_CT
258467004|Nasopharyngeal washings|SNOMED_CT
258467004|Nasopharyngeal wash|SNOMED_CT
258467004|258467004|SNOMED_CT
429931000124105|Nasal aspirate|SNOMED_CT
429931000124105|Nasal aspirate specimen|SNOMED_CT
429931000124105|429931000124105|SNOMED_CT
258529004|Throat swab|SNOMED_CT
258529004|Oropharyngeal Swab|SNOMED_CT
258529004|258529004|SNOMED_CT
418932006|Oral Swab|SNOMED_CT
418932006|418932006|SNOMED_CT
119334006|Sputum specimen|SNOMED_CT
119334006|Sputum|SNOMED_CT
119334006|119334006|SNOMED_CT
258560004|Saliva specimen|SNOMED_CT
258560004|Saliva|SNOMED_CT
258560004|258560004|SNOMED_CT
119364003|Serum specimen|SNOMED_CT
119364003|Serum|SNOMED_CT
119364003|119364003|SNOMED_CT
119361006|Plasma specimen|SNOMED_CT
119361006|Plasma|SNOMED_CT
119361006|119361006|SNOMED_CT
258580003|Whole blood sample|SNOMED_CT
258580003|Whole blood|SNOMED_CT
258580003|258580003|SNOMED_CT
122555007|Venous blood specimen|SNOMED_CT
122555007|Venous whole blood|SNOMED_CT
122555007|122555007|SNOMED_CT
119297000|Blood specimen|SNOMED_CT
119297000|119297000|SNOMED_CT
122554006|Capillary blood specimen|SNOMED_CT
122554006|fingerstick whole blood|SNOMED_CT
122554006|122554006|SNOMED_CT
440500007|Dried blood spot specimen|SNOMED_CT
440500007|Dried blood spot|SNOMED_CT
440500007|fingerstick blood dried blood spot|SNOMED_CT
440500007|440500007|SNOMED_CT
433801000124107|Nasopharyngeal and oropharyngeal swab|SNOMED_CT
433801000124107|Nasal and throat swab combination|SNOMED_CT
433801000124107|Nasal and throat swab|SNOMED_CT
433801000124107|433801000124107|SNOMED_CT
309171007|Lower respiratory fluid sample|SNOMED_CT
309171007|lower respiratory tract aspirates|SNOMED_CT
309171007|309171007|SNOMED_CT
258607008|Bronchoalveolar lavage fluid sample|SNOMED_CT
258607008|Bronchoalveolar lavage fluid|SNOMED_CT
258607008|Bronchoalveolar lavage|SNOMED_CT
258607008|258607008|SNOMED_CT
433871000124101|Nasal washings|SNOMED_CT
433871000124101|433871000124101|SNOMED_CT
441620008|Sputum specimen obtained by sputum induction|SNOMED_CT
441620008|441620008|SNOMED_CT
441903006|Coughed sputum specimen|SNOMED_CT
441903006|441903006|SNOMED_CT
119336008|Specimen from trachea obtained by aspiration|SNOMED_CT
119336008|119336008|SNOMED_CT
258610001|Oral fluid specimen|SNOMED_CT
258610001|258610001|SNOMED_CT
119335007|Specimen obtained by bronchial aspiration|SNOMED_CT
119335007|119335007|SNOMED_CT
445447003|Exhaled air specimen|SNOMED_CT
445447003|445447003|SNOMED_CT

**Documentation**:
The specimen source, such as Blood or Serum
---

**Name**: symptomatic_for_disease

**ReportStream Internal Name**: symptomatic_for_disease

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:
Override the base hl70136 valueset with a custom one, to handle slightly different syntax
---

**Name**: symptomsList

**ReportStream Internal Name**: symptoms_list

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom.  Just a simple text string for now. Format is symptomCode1^date1;symptomCode2^date2; ...
---

**Name**: testCodingSystem

**ReportStream Internal Name**: test_coding_system

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Custom.  Eg, "LN"
---

**Name**: deviceIdentifier

**ReportStream Internal Name**: test_kit_name_id

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Testkit Name ID

**Documentation**:
Must match LIVD column M, "Test Kit Name ID"
---

**Name**: test_performed_code

**ReportStream Internal Name**: test_performed_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Code

**Documentation**:
eg, 94558-4
---

**Name**: test_result

**ReportStream Internal Name**: test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
260385009|Negative|SNOMED_CT
260385009|Neg|SNOMED_CT
260385009|Negative *** High ***|SNOMED_CT
260385009|Negative *** Low ***|SNOMED_CT
260385009|260385009|SNOMED_CT
260415000|Not detected|SNOMED_CT
260415000|NDET|SNOMED_CT
260415000|260415000|SNOMED_CT
260373001|Detected|SNOMED_CT
260373001|DET|SNOMED_CT
260373001|260373001|SNOMED_CT
10828004|Positive|SNOMED_CT
10828004|Pos|SNOMED_CT
10828004|Positive (Abnormal)|SNOMED_CT
10828004|Positive (Alpha Abnormal)|SNOMED_CT
10828004|Positive *** High ***|SNOMED_CT
10828004|Positive  *** High ***|SNOMED_CT
10828004|Positive  *** Low ***|SNOMED_CT
10828004|Positive |SNOMED_CT
10828004|10828004|SNOMED_CT
720735008|Presumptive positive|SNOMED_CT
720735008|720735008|SNOMED_CT
419984006|Inconclusive|SNOMED_CT
419984006|Inconclusive Result|SNOMED_CT
419984006|419984006|SNOMED_CT
42425007|Equivocal|SNOMED_CT
42425007|42425007|SNOMED_CT
895231008|Not detected in pooled specimen|SNOMED_CT
895231008|895231008|SNOMED_CT
462371000124108|Detected in pooled specimen|SNOMED_CT
462371000124108|462371000124108|SNOMED_CT
455371000124106|Invalid result|SNOMED_CT
455371000124106|Invalid|SNOMED_CT
455371000124106|455371000124106|SNOMED_CT
125154007|Specimen unsatisfactory for evaluation|SNOMED_CT
125154007|125154007|SNOMED_CT
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)|SNOMED_CT
840539006|840539006|SNOMED_CT
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)|SNOMED_CT
840544004|840544004|SNOMED_CT
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)|SNOMED_CT
840546002|840546002|SNOMED_CT
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)|SNOMED_CT
840533007|840533007|SNOMED_CT
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840536004|840536004|SNOMED_CT
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840535000|840535000|SNOMED_CT
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)|SNOMED_CT
840534001|840534001|SNOMED_CT
373121007|Test not done|SNOMED_CT
373121007|373121007|SNOMED_CT
82334004|Indeterminate|SNOMED_CT
82334004|82334004|SNOMED_CT

**Documentation**:
eg, 260373001
---

**Name**: testResultCodingSystem

**ReportStream Internal Name**: test_result_coding_system

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
eg, SCT.   Custom
---

**Name**: test_result_date

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
eg, 20210111
---

**Name**: test_result_status

**ReportStream Internal Name**: test_result_status

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: F

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
C|Record coming over is a correction and thus replaces a final result|HL7
D|Deletes the OBX record|HL7
F|Final results; Can only be changed with a corrected result|HL7
I|Specimen in lab; results pending|HL7
N|Not asked; used to affirmatively document that the observation identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be sought.|HL7
O|Order detail description only (no result)|HL7
P|Preliminary results|HL7
R|Results entered -- not verified|HL7
S|Partial results|HL7
U|Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final|HL7
W|Post original as wrong, e.g., transmitted for wrong patient|HL7
X|Results cannot be obtained for this observation|HL7

**Documentation**:
The test result status, which is different from the test result itself. Per the valueset, this indicates if
the test result is in some intermediate status, is a correction, or is the final result.

---

**Name**: testResultText

**ReportStream Internal Name**: test_result_text

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
eg, "DETECTED".  Custom.  ReportStream uses testResult code, not this text value.
---

**Name**: testing_lab_city

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The city of the testing lab
---

**Name**: testing_lab_clia

**ReportStream Internal Name**: testing_lab_clia

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

**Name**: performingFacilityCounty

**ReportStream Internal Name**: testing_lab_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

**Documentation**:
The text value for the testing lab county. This is used to do the lookup in the FIPS dataset.
---

**Name**: testing_lab_name

**ReportStream Internal Name**: testing_lab_name

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

**Name**: testing_lab_phone_number

**ReportStream Internal Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The phone number of the testing lab
---

**Name**: testing_lab_specimen_received_date

**ReportStream Internal Name**: testing_lab_specimen_received_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_state

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state for the testing lab
---

**Name**: testing_lab_street

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The street address for the testing lab
---

**Name**: testing_lab_street2

**ReportStream Internal Name**: testing_lab_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Street 2 field for the testing lab
---

**Name**: testing_lab_zip_code

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The postal code for the testing lab
---

**Name**: abnormal_flag

**ReportStream Internal Name**: abnormal_flag

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
A|Abnormal (applies to non-numeric results)|HL7
&#62;|Above absolute high-off instrument scale|HL7
H|Above high normal|HL7
HH|Above upper panic limits|HL7
AC|Anti-complementary substances present|HL7
<|Below absolute low-off instrument scale|HL7
L|Below low normal|HL7
LL|Below lower panic limits|HL7
B|Better--use when direction not relevant|HL7
TOX|Cytotoxic substance present|HL7
DET|Detected|HL7
IND|Indeterminate|HL7
I|Intermediate. Indicates for microbiology susceptibilities only.|HL7
MS|Moderately susceptible. Indicates for microbiology susceptibilities only.|HL7
NEG|Negative|HL7
null|No range defined, or normal ranges don't apply|HL7
NR|Non-reactive|HL7
N|Normal (applies to non-numeric results)|HL7
ND|Not Detected|HL7
POS|Positive|HL7
QCF|Quality Control Failure|HL7
RR|Reactive|HL7
R|Resistant. Indicates for microbiology susceptibilities only.|HL7
D|Significant change down|HL7
U|Significant change up|HL7
S|Susceptible. Indicates for microbiology susceptibilities only.|HL7
AA|Very abnormal (applies to non-numeric units, analogous to panic limits for numeric units)|HL7
VS|Very susceptible. Indicates for microbiology susceptibilities only.|HL7
WR|Weakly reactive|HL7
W|Worse--use when direction not relevant|HL7

**Documentation**:
This field is generated based on the normalcy status of the result. A = abnormal; N = normal
---

**Name**: filler_order_id

**ReportStream Internal Name**: filler_order_id

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

**Name**: patient_id_type

**ReportStream Internal Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Default Value**: PI

**Cardinality**: [0..1]

---

**Name**: placer_order_id

**ReportStream Internal Name**: placer_order_id

**Type**: ID

**PII**: No

**HL7 Fields**

- [OBR-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.1)
- [ORC-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.1)

**Cardinality**: [0..1]

**Documentation**:
The ID number of the lab order from the placer
---

**Name**: test_authorized_for_home

**ReportStream Internal Name**: test_authorized_for_home

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: is_home

**Documentation**:
Is the test authorized for home use by the FDA (Y, N, UNK)
---

**Name**: test_authorized_for_otc

**ReportStream Internal Name**: test_authorized_for_otc

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: is_otc

**Documentation**:
Is the test authorized for over-the-counter purchase by the FDA (Y, N, UNK)
---

**Name**: test_authorized_for_unproctored

**ReportStream Internal Name**: test_authorized_for_unproctored

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: is_unproctored

**Documentation**:
Is the test authorized for unproctored administration by the FDA (Y, N, UNK)
---

**Name**: test_type

**ReportStream Internal Name**: test_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Type

---
