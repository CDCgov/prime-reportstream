/*
 * This class is an auto-generated source file for a HAPI
 * HL7 v2.x standard structure class.
 *
 * For more information, visit: http://hl7api.sourceforge.net/
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License.
 *
 * The Original Code is "[file_name]".  Description:
 * "[one_line_description]"
 *
 * The Initial Developer of the Original Code is University Health Network. Copyright (C)
 * 2012.  All Rights Reserved.
 *
 * Contributor(s): ______________________________________.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * GNU General Public License (the  "GPL"), in which case the provisions of the GPL are
 * applicable instead of those above.  If you wish to allow use of your version of this
 * file only under the terms of the GPL and not to allow others to use your version
 * of this file under the MPL, indicate your decision by deleting  the provisions above
 * and replace  them with the notice and other provisions required by the GPL License.
 * If you do not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the GPL.
 *
 */


package fhirengine.translation.hl7.structures.nistelr251.segment;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v251.datatype.*;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.nistelr251.datatype.CWE_ELR;
import fhirengine.translation.hl7.structures.nistelr251.datatype.XPN_ELR;


/**
 * <p>Represents an HL7 PID message segment (Patient Identification).
 * This segment has the following fields:</p>
 * <ul>
 * <li>PID-1: Set ID - PID (SI) <b> </b>
 * <li>PID-2: Patient ID (CX) <b>optional repeating</b>
 * <li>PID-3: Patient Identifier List (CX) <b> repeating</b>
 * <li>PID-4: Alternate Patient ID - PID (CX) <b>optional repeating</b>
 * <li>PID-5: Patient Name (XPN_ELR) <b> repeating</b>
 * <li>PID-6: Mother's Maiden Name (XPN_ELR) <b>optional </b>
 * <li>PID-7: Date/Time of Birth (TS) <b>optional </b>
 * <li>PID-8: Administrative Sex (IS) <b>optional </b>
 * <li>PID-9: Patient Alias (XPN) <b>optional repeating</b>
 * <li>PID-10: Race (CWE_ELR) <b>optional repeating</b>
 * <li>PID-11: Patient Address (XAD) <b>optional repeating</b>
 * <li>PID-12: County Code (IS) <b>optional repeating</b>
 * <li>PID-13: Phone Number - Home (XTN) <b>optional repeating</b>
 * <li>PID-14: Phone Number - Business (XTN) <b>optional repeating</b>
 * <li>PID-15: Primary Language (CWE) <b>optional repeating</b>
 * <li>PID-16: Marital Status (CWE) <b>optional </b>
 * <li>PID-17: Religion (CWE) <b>optional </b>
 * <li>PID-18: Patient Account Number (CX) <b>optional </b>
 * <li>PID-19: SSN Number - Patient (ST) <b>optional repeating</b>
 * <li>PID-20: Driver's License Number - Patient (DLN) <b>optional repeating</b>
 * <li>PID-21: Mother's Identifier (CX) <b>optional repeating</b>
 * <li>PID-22: Ethnic Group (CWE_ELR) <b>optional repeating</b>
 * <li>PID-23: Birth Place (ST) <b>optional </b>
 * <li>PID-24: Multiple Birth Indicator (ID) <b>optional </b>
 * <li>PID-25: Birth Order (NM) <b>optional </b>
 * <li>PID-26: Citizenship (CWE) <b>optional repeating</b>
 * <li>PID-27: Veterans Military Status (CWE) <b>optional </b>
 * <li>PID-28: Nationality (CE) <b>optional repeating</b>
 * <li>PID-29: Patient Death Date and Time (TS) <b>optional </b>
 * <li>PID-30: Patient Death Indicator (ID) <b>optional </b>
 * <li>PID-31: Identity Unknown Indicator (ID) <b>optional </b>
 * <li>PID-32: Identity Reliability Code (IS) <b>optional repeating</b>
 * <li>PID-33: Last Update Date/Time (TS) <b>optional </b>
 * <li>PID-34: Last Update Facility (HD) <b>optional </b>
 * <li>PID-35: Species Code (CWE_ELR) <b>optional </b>
 * <li>PID-36: Breed Code (CWE) <b>optional </b>
 * <li>PID-37: Strain (ST) <b>optional </b>
 * <li>PID-38: Production Class Code (CWE) <b>optional repeating</b>
 * <li>PID-39: Tribal Citizenship (CWE) <b>optional repeating</b>
 * </ul>
 */
@SuppressWarnings("unused")
public class PID extends AbstractSegment {

  /**
   * Creates a new PID segment
   */
  public PID(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(SI.class, true, 1, 0, new Object[]{getMessage()}, "Set ID - PID");
      this.add(CX.class, false, 0, 0, new Object[]{getMessage()}, "Patient ID");
      this.add(CX.class, true, -1, 0, new Object[]{getMessage()}, "Patient Identifier List");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Alternate Patient ID - PID");
      this.add(XPN_ELR.class, true, -1, 0, new Object[]{getMessage()}, "Patient Name");
      this.add(XPN_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Mother's Maiden Name");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Date/Time of Birth");
      this.add(IS.class, false, 1, 0, new Object[]{getMessage(), 1}, "Administrative Sex");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Patient Alias");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Race");
      this.add(XAD.class, false, -1, 0, new Object[]{getMessage()}, "Patient Address");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "County Code");
      this.add(XTN.class, false, -1, 0, new Object[]{getMessage()}, "Phone Number - Home");
      this.add(XTN.class, false, -1, 0, new Object[]{getMessage()}, "Phone Number - Business");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Primary Language");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Marital Status");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Religion");
      this.add(CX.class, false, 1, 0, new Object[]{getMessage()}, "Patient Account Number");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "SSN Number - Patient");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Driver's License Number - Patient");
      this.add(CX.class, false, -1, 0, new Object[]{getMessage()}, "Mother's Identifier");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Ethnic Group");
      this.add(ST.class, false, 1, 0, new Object[]{getMessage()}, "Birth Place");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), 136}, "Multiple Birth Indicator");
      this.add(NM.class, false, 1, 0, new Object[]{getMessage()}, "Birth Order");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Citizenship");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Veterans Military Status");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Nationality");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Patient Death Date and Time");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), 136}, "Patient Death Indicator");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), 136}, "Identity Unknown Indicator");
      this.add(IS.class, false, -1, 0, new Object[]{getMessage(), 445}, "Identity Reliability Code");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Last Update Date/Time");
      this.add(HD.class, false, 1, 0, new Object[]{getMessage()}, "Last Update Facility");
      this.add(CWE_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Species Code");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Breed Code");
      this.add(ST.class, false, 1, 0, new Object[]{getMessage()}, "Strain");
      this.add(CWE.class, false, 2, 0, new Object[]{getMessage()}, "Production Class Code");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Tribal Citizenship");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating PID - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns
   * PID-1: "Set ID - PID" - creates it if necessary
   */
  public SI getSetIDPID() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-1: "Set ID - PID" - creates it if necessary
   */
  public SI getPid1_SetIDPID() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient ID (PID-2).
   */
  public CX[] getPatientID() {
    CX[] retVal = this.getTypedField(2, new CX[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient ID (PID-2).
   */
  public CX[] getPid2_PatientID() {
    CX[] retVal = this.getTypedField(2, new CX[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient ID (PID-2).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientIDReps() {
    return this.getReps(2);
  }


  /**
   * Returns a specific repetition of
   * PID-2: "Patient ID" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getPatientID(int rep) {
    CX retVal = this.getTypedField(2, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-2: "Patient ID" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getPid2_PatientID(int rep) {
    CX retVal = this.getTypedField(2, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient ID (PID-2).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid2_PatientIDReps() {
    return this.getReps(2);
  }


  /**
   * Inserts a repetition of
   * PID-2: "Patient ID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertPatientID(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(2, rep);
  }


  /**
   * Inserts a repetition of
   * PID-2: "Patient ID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertPid2_PatientID(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(2, rep);
  }


  /**
   * Removes a repetition of
   * PID-2: "Patient ID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removePatientID(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(2, rep);
  }


  /**
   * Removes a repetition of
   * PID-2: "Patient ID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removePid2_PatientID(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(2, rep);
  }


  /**
   * Returns all repetitions of Patient Identifier List (PID-3).
   */
  public CX[] getPatientIdentifierList() {
    CX[] retVal = this.getTypedField(3, new CX[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Identifier List (PID-3).
   */
  public CX[] getPid3_PatientIdentifierList() {
    CX[] retVal = this.getTypedField(3, new CX[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Identifier List (PID-3).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientIdentifierListReps() {
    return this.getReps(3);
  }


  /**
   * Returns a specific repetition of
   * PID-3: "Patient Identifier List" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getPatientIdentifierList(int rep) {
    CX retVal = this.getTypedField(3, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-3: "Patient Identifier List" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getPid3_PatientIdentifierList(int rep) {
    CX retVal = this.getTypedField(3, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Identifier List (PID-3).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid3_PatientIdentifierListReps() {
    return this.getReps(3);
  }


  /**
   * Inserts a repetition of
   * PID-3: "Patient Identifier List" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertPatientIdentifierList(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(3, rep);
  }


  /**
   * Inserts a repetition of
   * PID-3: "Patient Identifier List" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertPid3_PatientIdentifierList(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(3, rep);
  }


  /**
   * Removes a repetition of
   * PID-3: "Patient Identifier List" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removePatientIdentifierList(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(3, rep);
  }


  /**
   * Removes a repetition of
   * PID-3: "Patient Identifier List" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removePid3_PatientIdentifierList(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(3, rep);
  }


  /**
   * Returns all repetitions of Alternate Patient ID - PID (PID-4).
   */
  public CX[] getAlternatePatientIDPID() {
    CX[] retVal = this.getTypedField(4, new CX[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Alternate Patient ID - PID (PID-4).
   */
  public CX[] getPid4_AlternatePatientIDPID() {
    CX[] retVal = this.getTypedField(4, new CX[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Alternate Patient ID - PID (PID-4).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAlternatePatientIDPIDReps() {
    return this.getReps(4);
  }


  /**
   * Returns a specific repetition of
   * PID-4: "Alternate Patient ID - PID" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getAlternatePatientIDPID(int rep) {
    CX retVal = this.getTypedField(4, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-4: "Alternate Patient ID - PID" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getPid4_AlternatePatientIDPID(int rep) {
    CX retVal = this.getTypedField(4, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Alternate Patient ID - PID (PID-4).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid4_AlternatePatientIDPIDReps() {
    return this.getReps(4);
  }


  /**
   * Inserts a repetition of
   * PID-4: "Alternate Patient ID - PID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertAlternatePatientIDPID(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(4, rep);
  }


  /**
   * Inserts a repetition of
   * PID-4: "Alternate Patient ID - PID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertPid4_AlternatePatientIDPID(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(4, rep);
  }


  /**
   * Removes a repetition of
   * PID-4: "Alternate Patient ID - PID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removeAlternatePatientIDPID(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(4, rep);
  }


  /**
   * Removes a repetition of
   * PID-4: "Alternate Patient ID - PID" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removePid4_AlternatePatientIDPID(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(4, rep);
  }


  /**
   * Returns all repetitions of Patient Name (PID-5).
   */
  public XPN_ELR[] getPatientName() {
    XPN_ELR[] retVal = this.getTypedField(5, new XPN_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Name (PID-5).
   */
  public XPN_ELR[] getPid5_PatientName() {
    XPN_ELR[] retVal = this.getTypedField(5, new XPN_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Name (PID-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientNameReps() {
    return this.getReps(5);
  }


  /**
   * Returns a specific repetition of
   * PID-5: "Patient Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN_ELR getPatientName(int rep) {
    XPN_ELR retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-5: "Patient Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN_ELR getPid5_PatientName(int rep) {
    XPN_ELR retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Name (PID-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid5_PatientNameReps() {
    return this.getReps(5);
  }


  /**
   * Inserts a repetition of
   * PID-5: "Patient Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR insertPatientName(int rep) throws HL7Exception {
    return (XPN_ELR) super.insertRepetition(5, rep);
  }


  /**
   * Inserts a repetition of
   * PID-5: "Patient Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR insertPid5_PatientName(int rep) throws HL7Exception {
    return (XPN_ELR) super.insertRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * PID-5: "Patient Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR removePatientName(int rep) throws HL7Exception {
    return (XPN_ELR) super.removeRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * PID-5: "Patient Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR removePid5_PatientName(int rep) throws HL7Exception {
    return (XPN_ELR) super.removeRepetition(5, rep);
  }


  /**
   * Returns
   * PID-6: "Mother's Maiden Name" - creates it if necessary
   */
  public XPN_ELR getMotherSMaidenName() {
    XPN_ELR retVal = this.getTypedField(6, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-6: "Mother's Maiden Name" - creates it if necessary
   */
  public XPN_ELR getPid6_MotherSMaidenName() {
    XPN_ELR retVal = this.getTypedField(6, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-7: "Date/Time of Birth" - creates it if necessary
   */
  public TS getDateTimeOfBirth() {
    TS retVal = this.getTypedField(7, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-7: "Date/Time of Birth" - creates it if necessary
   */
  public TS getPid7_DateTimeOfBirth() {
    TS retVal = this.getTypedField(7, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-8: "Administrative Sex" - creates it if necessary
   */
  public IS getAdministrativeSex() {
    IS retVal = this.getTypedField(8, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-8: "Administrative Sex" - creates it if necessary
   */
  public IS getPid8_AdministrativeSex() {
    IS retVal = this.getTypedField(8, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Alias (PID-9).
   */
  public XPN[] getPatientAlias() {
    XPN[] retVal = this.getTypedField(9, new XPN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Alias (PID-9).
   */
  public XPN[] getPid9_PatientAlias() {
    XPN[] retVal = this.getTypedField(9, new XPN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Alias (PID-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientAliasReps() {
    return this.getReps(9);
  }


  /**
   * Returns a specific repetition of
   * PID-9: "Patient Alias" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN getPatientAlias(int rep) {
    XPN retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-9: "Patient Alias" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN getPid9_PatientAlias(int rep) {
    XPN retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Alias (PID-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid9_PatientAliasReps() {
    return this.getReps(9);
  }


  /**
   * Inserts a repetition of
   * PID-9: "Patient Alias" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN insertPatientAlias(int rep) throws HL7Exception {
    return (XPN) super.insertRepetition(9, rep);
  }


  /**
   * Inserts a repetition of
   * PID-9: "Patient Alias" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN insertPid9_PatientAlias(int rep) throws HL7Exception {
    return (XPN) super.insertRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * PID-9: "Patient Alias" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN removePatientAlias(int rep) throws HL7Exception {
    return (XPN) super.removeRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * PID-9: "Patient Alias" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN removePid9_PatientAlias(int rep) throws HL7Exception {
    return (XPN) super.removeRepetition(9, rep);
  }


  /**
   * Returns all repetitions of Race (PID-10).
   */
  public CWE_ELR[] getRace() {
    CWE_ELR[] retVal = this.getTypedField(10, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Race (PID-10).
   */
  public CWE_ELR[] getPid10_Race() {
    CWE_ELR[] retVal = this.getTypedField(10, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Race (PID-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getRaceReps() {
    return this.getReps(10);
  }


  /**
   * Returns a specific repetition of
   * PID-10: "Race" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getRace(int rep) {
    CWE_ELR retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-10: "Race" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getPid10_Race(int rep) {
    CWE_ELR retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Race (PID-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid10_RaceReps() {
    return this.getReps(10);
  }


  /**
   * Inserts a repetition of
   * PID-10: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertRace(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(10, rep);
  }


  /**
   * Inserts a repetition of
   * PID-10: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertPid10_Race(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * PID-10: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeRace(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * PID-10: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removePid10_Race(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(10, rep);
  }


  /**
   * Returns all repetitions of Patient Address (PID-11).
   */
  public XAD[] getPatientAddress() {
    XAD[] retVal = this.getTypedField(11, new XAD[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Address (PID-11).
   */
  public XAD[] getPid11_PatientAddress() {
    XAD[] retVal = this.getTypedField(11, new XAD[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Address (PID-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientAddressReps() {
    return this.getReps(11);
  }


  /**
   * Returns a specific repetition of
   * PID-11: "Patient Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD getPatientAddress(int rep) {
    XAD retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-11: "Patient Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD getPid11_PatientAddress(int rep) {
    XAD retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Address (PID-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid11_PatientAddressReps() {
    return this.getReps(11);
  }


  /**
   * Inserts a repetition of
   * PID-11: "Patient Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD insertPatientAddress(int rep) throws HL7Exception {
    return (XAD) super.insertRepetition(11, rep);
  }


  /**
   * Inserts a repetition of
   * PID-11: "Patient Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD insertPid11_PatientAddress(int rep) throws HL7Exception {
    return (XAD) super.insertRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * PID-11: "Patient Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD removePatientAddress(int rep) throws HL7Exception {
    return (XAD) super.removeRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * PID-11: "Patient Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD removePid11_PatientAddress(int rep) throws HL7Exception {
    return (XAD) super.removeRepetition(11, rep);
  }


  /**
   * Returns all repetitions of County Code (PID-12).
   */
  public IS[] getCountyCode() {
    IS[] retVal = this.getTypedField(12, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of County Code (PID-12).
   */
  public IS[] getPid12_CountyCode() {
    IS[] retVal = this.getTypedField(12, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of County Code (PID-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCountyCodeReps() {
    return this.getReps(12);
  }


  /**
   * Returns a specific repetition of
   * PID-12: "County Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getCountyCode(int rep) {
    IS retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-12: "County Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPid12_CountyCode(int rep) {
    IS retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of County Code (PID-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid12_CountyCodeReps() {
    return this.getReps(12);
  }


  /**
   * Inserts a repetition of
   * PID-12: "County Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertCountyCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(12, rep);
  }


  /**
   * Inserts a repetition of
   * PID-12: "County Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPid12_CountyCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * PID-12: "County Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeCountyCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * PID-12: "County Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePid12_CountyCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(12, rep);
  }


  /**
   * Returns all repetitions of Phone Number - Home (PID-13).
   */
  public XTN[] getPhoneNumberHome() {
    XTN[] retVal = this.getTypedField(13, new XTN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Phone Number - Home (PID-13).
   */
  public XTN[] getPid13_PhoneNumberHome() {
    XTN[] retVal = this.getTypedField(13, new XTN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Phone Number - Home (PID-13).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPhoneNumberHomeReps() {
    return this.getReps(13);
  }


  /**
   * Returns a specific repetition of
   * PID-13: "Phone Number - Home" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getPhoneNumberHome(int rep) {
    XTN retVal = this.getTypedField(13, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-13: "Phone Number - Home" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getPid13_PhoneNumberHome(int rep) {
    XTN retVal = this.getTypedField(13, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Phone Number - Home (PID-13).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid13_PhoneNumberHomeReps() {
    return this.getReps(13);
  }


  /**
   * Inserts a repetition of
   * PID-13: "Phone Number - Home" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertPhoneNumberHome(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(13, rep);
  }


  /**
   * Inserts a repetition of
   * PID-13: "Phone Number - Home" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertPid13_PhoneNumberHome(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(13, rep);
  }


  /**
   * Removes a repetition of
   * PID-13: "Phone Number - Home" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removePhoneNumberHome(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(13, rep);
  }


  /**
   * Removes a repetition of
   * PID-13: "Phone Number - Home" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removePid13_PhoneNumberHome(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(13, rep);
  }


  /**
   * Returns all repetitions of Phone Number - Business (PID-14).
   */
  public XTN[] getPhoneNumberBusiness() {
    XTN[] retVal = this.getTypedField(14, new XTN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Phone Number - Business (PID-14).
   */
  public XTN[] getPid14_PhoneNumberBusiness() {
    XTN[] retVal = this.getTypedField(14, new XTN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Phone Number - Business (PID-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPhoneNumberBusinessReps() {
    return this.getReps(14);
  }


  /**
   * Returns a specific repetition of
   * PID-14: "Phone Number - Business" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getPhoneNumberBusiness(int rep) {
    XTN retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-14: "Phone Number - Business" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getPid14_PhoneNumberBusiness(int rep) {
    XTN retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Phone Number - Business (PID-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid14_PhoneNumberBusinessReps() {
    return this.getReps(14);
  }


  /**
   * Inserts a repetition of
   * PID-14: "Phone Number - Business" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertPhoneNumberBusiness(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(14, rep);
  }


  /**
   * Inserts a repetition of
   * PID-14: "Phone Number - Business" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertPid14_PhoneNumberBusiness(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * PID-14: "Phone Number - Business" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removePhoneNumberBusiness(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * PID-14: "Phone Number - Business" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removePid14_PhoneNumberBusiness(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(14, rep);
  }


  /**
   * Returns all repetitions of Primary Language (PID-15).
   */
  public CWE[] getPrimaryLanguage() {
    CWE[] retVal = this.getTypedField(15, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Primary Language (PID-15).
   */
  public CWE[] getPid15_PrimaryLanguage() {
    CWE[] retVal = this.getTypedField(15, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Primary Language (PID-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPrimaryLanguageReps() {
    return this.getReps(15);
  }


  /**
   * Returns a specific repetition of
   * PID-15: "Primary Language" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPrimaryLanguage(int rep) {
    CWE retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-15: "Primary Language" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPid15_PrimaryLanguage(int rep) {
    CWE retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Primary Language (PID-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid15_PrimaryLanguageReps() {
    return this.getReps(15);
  }


  /**
   * Inserts a repetition of
   * PID-15: "Primary Language" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPrimaryLanguage(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(15, rep);
  }


  /**
   * Inserts a repetition of
   * PID-15: "Primary Language" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPid15_PrimaryLanguage(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * PID-15: "Primary Language" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePrimaryLanguage(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * PID-15: "Primary Language" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePid15_PrimaryLanguage(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(15, rep);
  }


  /**
   * Returns
   * PID-16: "Marital Status" - creates it if necessary
   */
  public CWE getMaritalStatus() {
    CWE retVal = this.getTypedField(16, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-16: "Marital Status" - creates it if necessary
   */
  public CWE getPid16_MaritalStatus() {
    CWE retVal = this.getTypedField(16, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-17: "Religion" - creates it if necessary
   */
  public CWE getReligion() {
    CWE retVal = this.getTypedField(17, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-17: "Religion" - creates it if necessary
   */
  public CWE getPid17_Religion() {
    CWE retVal = this.getTypedField(17, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-18: "Patient Account Number" - creates it if necessary
   */
  public CX getPatientAccountNumber() {
    CX retVal = this.getTypedField(18, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-18: "Patient Account Number" - creates it if necessary
   */
  public CX getPid18_PatientAccountNumber() {
    CX retVal = this.getTypedField(18, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of SSN Number - Patient (PID-19).
   */
  public ST[] getSSNNumberPatient() {
    ST[] retVal = this.getTypedField(19, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of SSN Number - Patient (PID-19).
   */
  public ST[] getPid19_SSNNumberPatient() {
    ST[] retVal = this.getTypedField(19, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of SSN Number - Patient (PID-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSSNNumberPatientReps() {
    return this.getReps(19);
  }


  /**
   * Returns a specific repetition of
   * PID-19: "SSN Number - Patient" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getSSNNumberPatient(int rep) {
    ST retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-19: "SSN Number - Patient" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getPid19_SSNNumberPatient(int rep) {
    ST retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of SSN Number - Patient (PID-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid19_SSNNumberPatientReps() {
    return this.getReps(19);
  }


  /**
   * Inserts a repetition of
   * PID-19: "SSN Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertSSNNumberPatient(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(19, rep);
  }


  /**
   * Inserts a repetition of
   * PID-19: "SSN Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertPid19_SSNNumberPatient(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * PID-19: "SSN Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeSSNNumberPatient(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * PID-19: "SSN Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removePid19_SSNNumberPatient(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(19, rep);
  }


  /**
   * Returns all repetitions of Driver's License Number - Patient (PID-20).
   */
  public DLN[] getDriverSLicenseNumberPatient() {
    DLN[] retVal = this.getTypedField(20, new DLN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Driver's License Number - Patient (PID-20).
   */
  public DLN[] getPid20_DriverSLicenseNumberPatient() {
    DLN[] retVal = this.getTypedField(20, new DLN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Driver's License Number - Patient (PID-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getDriverSLicenseNumberPatientReps() {
    return this.getReps(20);
  }


  /**
   * Returns a specific repetition of
   * PID-20: "Driver's License Number - Patient" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DLN getDriverSLicenseNumberPatient(int rep) {
    DLN retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-20: "Driver's License Number - Patient" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DLN getPid20_DriverSLicenseNumberPatient(int rep) {
    DLN retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Driver's License Number - Patient (PID-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid20_DriverSLicenseNumberPatientReps() {
    return this.getReps(20);
  }


  /**
   * Inserts a repetition of
   * PID-20: "Driver's License Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DLN insertDriverSLicenseNumberPatient(int rep) throws HL7Exception {
    return (DLN) super.insertRepetition(20, rep);
  }


  /**
   * Inserts a repetition of
   * PID-20: "Driver's License Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DLN insertPid20_DriverSLicenseNumberPatient(int rep) throws HL7Exception {
    return (DLN) super.insertRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * PID-20: "Driver's License Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DLN removeDriverSLicenseNumberPatient(int rep) throws HL7Exception {
    return (DLN) super.removeRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * PID-20: "Driver's License Number - Patient" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DLN removePid20_DriverSLicenseNumberPatient(int rep) throws HL7Exception {
    return (DLN) super.removeRepetition(20, rep);
  }


  /**
   * Returns all repetitions of Mother's Identifier (PID-21).
   */
  public CX[] getMotherSIdentifier() {
    CX[] retVal = this.getTypedField(21, new CX[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Mother's Identifier (PID-21).
   */
  public CX[] getPid21_MotherSIdentifier() {
    CX[] retVal = this.getTypedField(21, new CX[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Mother's Identifier (PID-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getMotherSIdentifierReps() {
    return this.getReps(21);
  }


  /**
   * Returns a specific repetition of
   * PID-21: "Mother's Identifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getMotherSIdentifier(int rep) {
    CX retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-21: "Mother's Identifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getPid21_MotherSIdentifier(int rep) {
    CX retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Mother's Identifier (PID-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid21_MotherSIdentifierReps() {
    return this.getReps(21);
  }


  /**
   * Inserts a repetition of
   * PID-21: "Mother's Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertMotherSIdentifier(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(21, rep);
  }


  /**
   * Inserts a repetition of
   * PID-21: "Mother's Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertPid21_MotherSIdentifier(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * PID-21: "Mother's Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removeMotherSIdentifier(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * PID-21: "Mother's Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removePid21_MotherSIdentifier(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(21, rep);
  }


  /**
   * Returns all repetitions of Ethnic Group (PID-22).
   */
  public CWE_ELR[] getEthnicGroup() {
    CWE_ELR[] retVal = this.getTypedField(22, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ethnic Group (PID-22).
   */
  public CWE_ELR[] getPid22_EthnicGroup() {
    CWE_ELR[] retVal = this.getTypedField(22, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ethnic Group (PID-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEthnicGroupReps() {
    return this.getReps(22);
  }


  /**
   * Returns a specific repetition of
   * PID-22: "Ethnic Group" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getEthnicGroup(int rep) {
    CWE_ELR retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-22: "Ethnic Group" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getPid22_EthnicGroup(int rep) {
    CWE_ELR retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ethnic Group (PID-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid22_EthnicGroupReps() {
    return this.getReps(22);
  }


  /**
   * Inserts a repetition of
   * PID-22: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertEthnicGroup(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(22, rep);
  }


  /**
   * Inserts a repetition of
   * PID-22: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertPid22_EthnicGroup(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * PID-22: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeEthnicGroup(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * PID-22: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removePid22_EthnicGroup(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(22, rep);
  }


  /**
   * Returns
   * PID-23: "Birth Place" - creates it if necessary
   */
  public ST getBirthPlace() {
    ST retVal = this.getTypedField(23, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-23: "Birth Place" - creates it if necessary
   */
  public ST getPid23_BirthPlace() {
    ST retVal = this.getTypedField(23, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-24: "Multiple Birth Indicator" - creates it if necessary
   */
  public ID getMultipleBirthIndicator() {
    ID retVal = this.getTypedField(24, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-24: "Multiple Birth Indicator" - creates it if necessary
   */
  public ID getPid24_MultipleBirthIndicator() {
    ID retVal = this.getTypedField(24, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-25: "Birth Order" - creates it if necessary
   */
  public NM getBirthOrder() {
    NM retVal = this.getTypedField(25, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-25: "Birth Order" - creates it if necessary
   */
  public NM getPid25_BirthOrder() {
    NM retVal = this.getTypedField(25, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Citizenship (PID-26).
   */
  public CWE[] getCitizenship() {
    CWE[] retVal = this.getTypedField(26, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Citizenship (PID-26).
   */
  public CWE[] getPid26_Citizenship() {
    CWE[] retVal = this.getTypedField(26, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Citizenship (PID-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCitizenshipReps() {
    return this.getReps(26);
  }


  /**
   * Returns a specific repetition of
   * PID-26: "Citizenship" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getCitizenship(int rep) {
    CWE retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-26: "Citizenship" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPid26_Citizenship(int rep) {
    CWE retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Citizenship (PID-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid26_CitizenshipReps() {
    return this.getReps(26);
  }


  /**
   * Inserts a repetition of
   * PID-26: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertCitizenship(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(26, rep);
  }


  /**
   * Inserts a repetition of
   * PID-26: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPid26_Citizenship(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * PID-26: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeCitizenship(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * PID-26: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePid26_Citizenship(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(26, rep);
  }


  /**
   * Returns
   * PID-27: "Veterans Military Status" - creates it if necessary
   */
  public CWE getVeteransMilitaryStatus() {
    CWE retVal = this.getTypedField(27, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-27: "Veterans Military Status" - creates it if necessary
   */
  public CWE getPid27_VeteransMilitaryStatus() {
    CWE retVal = this.getTypedField(27, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Nationality (PID-28).
   */
  public CE[] getNationality() {
    CE[] retVal = this.getTypedField(28, new CE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Nationality (PID-28).
   */
  public CE[] getPid28_Nationality() {
    CE[] retVal = this.getTypedField(28, new CE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Nationality (PID-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNationalityReps() {
    return this.getReps(28);
  }


  /**
   * Returns a specific repetition of
   * PID-28: "Nationality" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CE getNationality(int rep) {
    CE retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-28: "Nationality" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CE getPid28_Nationality(int rep) {
    CE retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Nationality (PID-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid28_NationalityReps() {
    return this.getReps(28);
  }


  /**
   * Inserts a repetition of
   * PID-28: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CE insertNationality(int rep) throws HL7Exception {
    return (CE) super.insertRepetition(28, rep);
  }


  /**
   * Inserts a repetition of
   * PID-28: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CE insertPid28_Nationality(int rep) throws HL7Exception {
    return (CE) super.insertRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * PID-28: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CE removeNationality(int rep) throws HL7Exception {
    return (CE) super.removeRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * PID-28: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CE removePid28_Nationality(int rep) throws HL7Exception {
    return (CE) super.removeRepetition(28, rep);
  }


  /**
   * Returns
   * PID-29: "Patient Death Date and Time" - creates it if necessary
   */
  public TS getPatientDeathDateAndTime() {
    TS retVal = this.getTypedField(29, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-29: "Patient Death Date and Time" - creates it if necessary
   */
  public TS getPid29_PatientDeathDateAndTime() {
    TS retVal = this.getTypedField(29, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-30: "Patient Death Indicator" - creates it if necessary
   */
  public ID getPatientDeathIndicator() {
    ID retVal = this.getTypedField(30, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-30: "Patient Death Indicator" - creates it if necessary
   */
  public ID getPid30_PatientDeathIndicator() {
    ID retVal = this.getTypedField(30, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-31: "Identity Unknown Indicator" - creates it if necessary
   */
  public ID getIdentityUnknownIndicator() {
    ID retVal = this.getTypedField(31, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-31: "Identity Unknown Indicator" - creates it if necessary
   */
  public ID getPid31_IdentityUnknownIndicator() {
    ID retVal = this.getTypedField(31, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Identity Reliability Code (PID-32).
   */
  public IS[] getIdentityReliabilityCode() {
    IS[] retVal = this.getTypedField(32, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Identity Reliability Code (PID-32).
   */
  public IS[] getPid32_IdentityReliabilityCode() {
    IS[] retVal = this.getTypedField(32, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Identity Reliability Code (PID-32).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getIdentityReliabilityCodeReps() {
    return this.getReps(32);
  }


  /**
   * Returns a specific repetition of
   * PID-32: "Identity Reliability Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getIdentityReliabilityCode(int rep) {
    IS retVal = this.getTypedField(32, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-32: "Identity Reliability Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPid32_IdentityReliabilityCode(int rep) {
    IS retVal = this.getTypedField(32, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Identity Reliability Code (PID-32).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid32_IdentityReliabilityCodeReps() {
    return this.getReps(32);
  }


  /**
   * Inserts a repetition of
   * PID-32: "Identity Reliability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertIdentityReliabilityCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(32, rep);
  }


  /**
   * Inserts a repetition of
   * PID-32: "Identity Reliability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPid32_IdentityReliabilityCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(32, rep);
  }


  /**
   * Removes a repetition of
   * PID-32: "Identity Reliability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeIdentityReliabilityCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(32, rep);
  }


  /**
   * Removes a repetition of
   * PID-32: "Identity Reliability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePid32_IdentityReliabilityCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(32, rep);
  }


  /**
   * Returns
   * PID-33: "Last Update Date/Time" - creates it if necessary
   */
  public TS getLastUpdateDateTime() {
    TS retVal = this.getTypedField(33, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-33: "Last Update Date/Time" - creates it if necessary
   */
  public TS getPid33_LastUpdateDateTime() {
    TS retVal = this.getTypedField(33, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-34: "Last Update Facility" - creates it if necessary
   */
  public HD getLastUpdateFacility() {
    HD retVal = this.getTypedField(34, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-34: "Last Update Facility" - creates it if necessary
   */
  public HD getPid34_LastUpdateFacility() {
    HD retVal = this.getTypedField(34, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-35: "Species Code" - creates it if necessary
   */
  public CWE_ELR getSpeciesCode() {
    CWE_ELR retVal = this.getTypedField(35, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-35: "Species Code" - creates it if necessary
   */
  public CWE_ELR getPid35_SpeciesCode() {
    CWE_ELR retVal = this.getTypedField(35, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-36: "Breed Code" - creates it if necessary
   */
  public CWE getBreedCode() {
    CWE retVal = this.getTypedField(36, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-36: "Breed Code" - creates it if necessary
   */
  public CWE getPid36_BreedCode() {
    CWE retVal = this.getTypedField(36, 0);
    return retVal;
  }


  /**
   * Returns
   * PID-37: "Strain" - creates it if necessary
   */
  public ST getStrain() {
    ST retVal = this.getTypedField(37, 0);
    return retVal;
  }

  /**
   * Returns
   * PID-37: "Strain" - creates it if necessary
   */
  public ST getPid37_Strain() {
    ST retVal = this.getTypedField(37, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Production Class Code (PID-38).
   */
  public CWE[] getProductionClassCode() {
    CWE[] retVal = this.getTypedField(38, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Production Class Code (PID-38).
   */
  public CWE[] getPid38_ProductionClassCode() {
    CWE[] retVal = this.getTypedField(38, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Production Class Code (PID-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getProductionClassCodeReps() {
    return this.getReps(38);
  }


  /**
   * Returns a specific repetition of
   * PID-38: "Production Class Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getProductionClassCode(int rep) {
    CWE retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-38: "Production Class Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPid38_ProductionClassCode(int rep) {
    CWE retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Production Class Code (PID-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid38_ProductionClassCodeReps() {
    return this.getReps(38);
  }


  /**
   * Inserts a repetition of
   * PID-38: "Production Class Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertProductionClassCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(38, rep);
  }


  /**
   * Inserts a repetition of
   * PID-38: "Production Class Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPid38_ProductionClassCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * PID-38: "Production Class Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeProductionClassCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * PID-38: "Production Class Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePid38_ProductionClassCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(38, rep);
  }


  /**
   * Returns all repetitions of Tribal Citizenship (PID-39).
   */
  public CWE[] getTribalCitizenship() {
    CWE[] retVal = this.getTypedField(39, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Tribal Citizenship (PID-39).
   */
  public CWE[] getPid39_TribalCitizenship() {
    CWE[] retVal = this.getTypedField(39, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Tribal Citizenship (PID-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTribalCitizenshipReps() {
    return this.getReps(39);
  }


  /**
   * Returns a specific repetition of
   * PID-39: "Tribal Citizenship" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getTribalCitizenship(int rep) {
    CWE retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PID-39: "Tribal Citizenship" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPid39_TribalCitizenship(int rep) {
    CWE retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Tribal Citizenship (PID-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPid39_TribalCitizenshipReps() {
    return this.getReps(39);
  }


  /**
   * Inserts a repetition of
   * PID-39: "Tribal Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertTribalCitizenship(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(39, rep);
  }


  /**
   * Inserts a repetition of
   * PID-39: "Tribal Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPid39_TribalCitizenship(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * PID-39: "Tribal Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeTribalCitizenship(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * PID-39: "Tribal Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePid39_TribalCitizenship(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(39, rep);
  }


  /**
   * {@inheritDoc}
   */
  protected Type createNewTypeWithoutReflection(int field) {
    switch (field) {
      case 0:
        return new SI(getMessage());
      case 1:
        return new CX(getMessage());
      case 2:
        return new CX(getMessage());
      case 3:
        return new CX(getMessage());
      case 4:
        return new XPN_ELR(getMessage());
      case 5:
        return new XPN_ELR(getMessage());
      case 6:
        return new TS(getMessage());
      case 7:
        return new IS(getMessage(), Integer.valueOf(1));
      case 8:
        return new XPN(getMessage());
      case 9:
        return new CWE_ELR(getMessage());
      case 10:
        return new XAD(getMessage());
      case 11:
        return new IS(getMessage(), Integer.valueOf(0));
      case 12:
        return new XTN(getMessage());
      case 13:
        return new XTN(getMessage());
      case 14:
        return new CWE(getMessage());
      case 15:
        return new CWE(getMessage());
      case 16:
        return new CWE(getMessage());
      case 17:
        return new CX(getMessage());
      case 18:
        return new ST(getMessage());
      case 19:
        return new DLN(getMessage());
      case 20:
        return new CX(getMessage());
      case 21:
        return new CWE_ELR(getMessage());
      case 22:
        return new ST(getMessage());
      case 23:
        return new ID(getMessage(), Integer.valueOf(136));
      case 24:
        return new NM(getMessage());
      case 25:
        return new CWE(getMessage());
      case 26:
        return new CWE(getMessage());
      case 27:
        return new CE(getMessage());
      case 28:
        return new TS(getMessage());
      case 29:
        return new ID(getMessage(), Integer.valueOf(136));
      case 30:
        return new ID(getMessage(), Integer.valueOf(136));
      case 31:
        return new IS(getMessage(), Integer.valueOf(445));
      case 32:
        return new TS(getMessage());
      case 33:
        return new HD(getMessage());
      case 34:
        return new CWE_ELR(getMessage());
      case 35:
        return new CWE(getMessage());
      case 36:
        return new ST(getMessage());
      case 37:
        return new CWE(getMessage());
      case 38:
        return new CWE(getMessage());
      default:
        return null;
    }
  }


}

