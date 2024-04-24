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


package fhirengine.translation.hl7.structures.radxmars251.segment;

// import gov.cdc.nist.group.*;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v251.datatype.*;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.radxmars251.datatype.*;


/**
 * <p>Represents an HL7 OBX message segment (Observation/Result).
 * This segment has the following fields:</p>
 * <ul>
 * <li>OBX-1: Set ID - OBX (SI) <b>optional </b>
 * <li>OBX-2: Value Type (ID) <b>optional </b>
 * <li>OBX-3: Observation Identifier (CWE) <b> </b>
 * <li>OBX-4: Observation Sub-ID (ST) <b>optional </b>
 * <li>OBX-5: Observation Value (Varies) <b> </b>
 * <li>OBX-6: Units (CWE) <b>optional </b>
 * <li>OBX-7: References Range (ST) <b>optional </b>
 * <li>OBX-8: Interpretation Codes (CWE) <b>optional repeating</b>
 * <li>OBX-9: Probability (NM) <b>optional </b>
 * <li>OBX-10: Nature of Abnormal Test (ID) <b>optional repeating</b>
 * <li>OBX-11: Observation Result Status (ID) <b> </b>
 * <li>OBX-12: Effective Date of Reference Range (TS) <b>optional </b>
 * <li>OBX-13: User Defined Access Checks (ST) <b>optional </b>
 * <li>OBX-14: Date/Time of the Observation (TS) <b>optional </b>
 * <li>OBX-15: Producer's ID (CWE) <b>optional </b>
 * <li>OBX-16: Responsible Observer (XCN) <b>optional repeating</b>
 * <li>OBX-17: Observation Method (CWE) <b>optional repeating</b>
 * <li>OBX-18: Equipment Instance Identifier (EI) <b>optional repeating</b>
 * <li>OBX-19: Date/Time of the Analysis (TS) <b>optional </b>
 * <li>OBX-20: Observation Site (CWE) <b>optional repeating</b>
 * <li>OBX-21: Observation Instance Identifier (EI) <b>optional </b>
 * <li>OBX-22: Mood Code (CNE) <b>optional </b>
 * <li>OBX-23: Performing Organization Name (XON) <b>optional </b>
 * <li>OBX-24: Performing Organization Address (XAD) <b>optional </b>
 * <li>OBX-25: Performing Organization Medical Director (XCN) <b>optional </b>
 * <li>OBX-26: Patient Results Release Category (ID) <b>optional </b>
 * <li>OBX-27: Root Cause (CWE) <b>optional </b>
 * <li>OBX-28: Local Process Control (ID) <b>optional </b>
 * <li>OBX-29: Observation Type (ID) <b>optional </b>
 * </ul>
 */
@SuppressWarnings("unused")
public class OBX_NIH extends AbstractSegment {

  /**
   * Creates a new OBX segment
   */
  public OBX_NIH(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(SI.class, false, 1, 4, new Object[]{getMessage()}, "Set ID - OBX");
      this.add(ID.class, false, 1, 3, new Object[]{getMessage(), Integer.valueOf(125)}, "Value Type");
      this.add(CWE_NIH.class, true, 1, 0, new Object[]{getMessage()}, "Observation Identifier");
      this.add(ST.class, false, 1, 20, new Object[]{getMessage()}, "Observation Sub-ID");
      this.add(Varies.class, true, 1, 99999, new Object[]{getMessage()}, "Observation Value");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Units");
      this.add(ST.class, false, 1, 60, new Object[]{getMessage()}, "References Range");
      this.add(CWE_NIH.class, false, 0, 0, new Object[]{getMessage()}, "Interpretation Codes");
      this.add(NM.class, false, 1, 5, new Object[]{getMessage()}, "Probability");
      this.add(ID.class, false, 0, 2, new Object[]{getMessage(), Integer.valueOf(80)}, "Nature of Abnormal Test");
      this.add(ID.class, true, 1, 1, new Object[]{getMessage(), Integer.valueOf(85)}, "Observation Result Status");
      this.add(TS_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Effective Date of Reference Range");
      this.add(ST.class, false, 1, 20, new Object[]{getMessage()}, "User Defined Access Checks");
      this.add(TS_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Date/Time of the Observation");
      this.add(CWE_OBX_15.class, false, 1, 0, new Object[]{getMessage()}, "Producer's ID");
      this.add(XCN.class, false, 0, 0, new Object[]{getMessage()}, "Responsible Observer");
      this.add(CWE_OBX_17.class, false, 0, 0, new Object[]{getMessage()}, "Observation Method");
      this.add(EI_NIH.class, false, 0, 0, new Object[]{getMessage()}, "Equipment Instance Identifier");
      this.add(TS_NIH2.class, false, 1, 0, new Object[]{getMessage()}, "Date/Time of the Analysis");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Observation Site");
      this.add(NULLDT.class, false, 1, 0, new Object[]{getMessage()}, "Observation Instance Identifier");
      this.add(NULLDT.class, false, 1, 0, new Object[]{getMessage()}, "Mood Code");
      this.add(XON_OBX_23.class, false, 1, 0, new Object[]{getMessage()}, "Performing Organization Name");
      this.add(XAD_OBX_24.class, false, 1, 0, new Object[]{getMessage()}, "Performing Organization Address");
      this.add(XCN_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Performing Organization Medical Director");
      this.add(ID.class, false, 1, 10, new Object[]{getMessage()}, "Patient Results Release Category");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Root Cause");
      this.add(ID.class, false, 0, 5, new Object[]{getMessage()}, "Local Process Control");
      this.add(ID.class, false, 1, 4, new Object[]{getMessage()}, "Observation Type");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating OBX - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns
   * OBX-1: "Set ID - OBX" - creates it if necessary
   */
  public SI getSetIDOBX() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-1: "Set ID - OBX" - creates it if necessary
   */
  public SI getObx1_SetIDOBX() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-2: "Value Type" - creates it if necessary
   */
  public ID getValueType() {
    ID retVal = this.getTypedField(2, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-2: "Value Type" - creates it if necessary
   */
  public ID getObx2_ValueType() {
    ID retVal = this.getTypedField(2, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-3: "Observation Identifier" - creates it if necessary
   */
  public CWE_NIH getObservationIdentifier() {
    CWE_NIH retVal = this.getTypedField(3, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-3: "Observation Identifier" - creates it if necessary
   */
  public CWE_NIH getObx3_ObservationIdentifier() {
    CWE_NIH retVal = this.getTypedField(3, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-4: "Observation Sub-ID" - creates it if necessary
   */
  public ST getObservationSubID() {
    ST retVal = this.getTypedField(4, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-4: "Observation Sub-ID" - creates it if necessary
   */
  public ST getObx4_ObservationSubID() {
    ST retVal = this.getTypedField(4, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-5: "Observation Value" - creates it if necessary
   */
  public Varies getObservationValue() {
    Varies retVal = this.getTypedField(5, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-5: "Observation Value" - creates it if necessary
   */
  public Varies getObx4_ObservationValue() {
    Varies retVal = this.getTypedField(5, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-6: "Units" - creates it if necessary
   */
  public CWE_NIH getUnits() {
    CWE_NIH retVal = this.getTypedField(6, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-6: "Units" - creates it if necessary
   */
  public CWE_NIH getObx6_Units() {
    CWE_NIH retVal = this.getTypedField(6, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-7: "References Range" - creates it if necessary
   */
  public ST getReferencesRange() {
    ST retVal = this.getTypedField(7, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-7: "References Range" - creates it if necessary
   */
  public ST getObx7_ReferencesRange() {
    ST retVal = this.getTypedField(7, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Interpretation Codes (OBX-8).
   */
  public CWE_NIH[] getInterpretationCodes() {
    CWE_NIH[] retVal = this.getTypedField(8, new CWE_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Interpretation Codes (OBX-8).
   */
  public CWE_NIH[] getObx8_InterpretationCodes() {
    CWE_NIH[] retVal = this.getTypedField(8, new CWE_NIH[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Interpretation Codes (OBX-8).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getInterpretationCodesReps() {
    return this.getReps(8);
  }


  /**
   * Returns a specific repetition of
   * OBX-8: "Interpretation Codes" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_NIH getInterpretationCodes(int rep) {
    CWE_NIH retVal = this.getTypedField(8, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBX-8: "Interpretation Codes" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_NIH getObx8_InterpretationCodes(int rep) {
    CWE_NIH retVal = this.getTypedField(8, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Interpretation Codes (OBX-8).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObx8_InterpretationCodesReps() {
    return this.getReps(8);
  }


  /**
   * Inserts a repetition of
   * OBX-8: "Interpretation Codes" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH insertInterpretationCodes(int rep) throws HL7Exception {
    return (CWE_NIH) super.insertRepetition(8, rep);
  }


  /**
   * Inserts a repetition of
   * OBX-8: "Interpretation Codes" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH insertObx8_InterpretationCodes(int rep) throws HL7Exception {
    return (CWE_NIH) super.insertRepetition(8, rep);
  }


  /**
   * Removes a repetition of
   * OBX-8: "Interpretation Codes" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH removeInterpretationCodes(int rep) throws HL7Exception {
    return (CWE_NIH) super.removeRepetition(8, rep);
  }


  /**
   * Removes a repetition of
   * OBX-8: "Interpretation Codes" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH removeObx8_InterpretationCodes(int rep) throws HL7Exception {
    return (CWE_NIH) super.removeRepetition(8, rep);
  }


  /**
   * Returns
   * OBX-9: "Probability" - creates it if necessary
   */
  public NM getProbability() {
    NM retVal = this.getTypedField(9, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-9: "Probability" - creates it if necessary
   */
  public NM getObx9_Probability() {
    NM retVal = this.getTypedField(9, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Nature of Abnormal Test (OBX-10).
   */
  public ID[] getNatureOfAbnormalTest() {
    ID[] retVal = this.getTypedField(10, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Nature of Abnormal Test (OBX-10).
   */
  public ID[] getObx10_NatureOfAbnormalTest() {
    ID[] retVal = this.getTypedField(10, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Nature of Abnormal Test (OBX-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNatureOfAbnormalTestReps() {
    return this.getReps(10);
  }


  /**
   * Returns a specific repetition of
   * OBX-10: "Nature of Abnormal Test" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getNatureOfAbnormalTest(int rep) {
    ID retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBX-10: "Nature of Abnormal Test" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getObx10_NatureOfAbnormalTest(int rep) {
    ID retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Nature of Abnormal Test (OBX-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObx10_NatureOfAbnormalTestReps() {
    return this.getReps(10);
  }


  /**
   * Inserts a repetition of
   * OBX-10: "Nature of Abnormal Test" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertNatureOfAbnormalTest(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(10, rep);
  }


  /**
   * Inserts a repetition of
   * OBX-10: "Nature of Abnormal Test" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertObx10_NatureOfAbnormalTest(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * OBX-10: "Nature of Abnormal Test" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeNatureOfAbnormalTest(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * OBX-10: "Nature of Abnormal Test" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeObx10_NatureOfAbnormalTest(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(10, rep);
  }


  /**
   * Returns
   * OBX-11: "Observation Result Status" - creates it if necessary
   */
  public ID getObservationResultStatus() {
    ID retVal = this.getTypedField(11, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-11: "Observation Result Status" - creates it if necessary
   */
  public ID getObx11_ObservationResultStatus() {
    ID retVal = this.getTypedField(11, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-12: "Effective Date of Reference Range" - creates it if necessary
   */
  public TS_NIH getEffectiveDateOfReferenceRange() {
    TS_NIH retVal = this.getTypedField(12, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-12: "Effective Date of Reference Range" - creates it if necessary
   */
  public TS_NIH getObx12_EffectiveDateOfReferenceRange() {
    TS_NIH retVal = this.getTypedField(12, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-13: "User Defined Access Checks" - creates it if necessary
   */
  public ST getUserDefinedAccessChecks() {
    ST retVal = this.getTypedField(13, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-13: "User Defined Access Checks" - creates it if necessary
   */
  public ST getObx13_UserDefinedAccessChecks() {
    ST retVal = this.getTypedField(13, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-14: "Date/Time of the Observation" - creates it if necessary
   */
  public TS_NIH getDateTimeOfTheObservation() {
    TS_NIH retVal = this.getTypedField(14, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-14: "Date/Time of the Observation" - creates it if necessary
   */
  public TS_NIH getObx14_DateTimeOfTheObservation() {
    TS_NIH retVal = this.getTypedField(14, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-15: "Producer's ID" - creates it if necessary
   */
  public CWE_OBX_15 getProducerSID() {
    CWE_OBX_15 retVal = this.getTypedField(15, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-15: "Producer's ID" - creates it if necessary
   */
  public CWE_OBX_15 getObx15_ProducerSID() {
    CWE_OBX_15 retVal = this.getTypedField(15, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Responsible Observer (OBX-16).
   */
  public XCN[] getResponsibleObserver() {
    XCN[] retVal = this.getTypedField(16, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Responsible Observer (OBX-16).
   */
  public XCN[] getObx16_ResponsibleObserver() {
    XCN[] retVal = this.getTypedField(16, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Responsible Observer (OBX-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getResponsibleObserverReps() {
    return this.getReps(16);
  }


  /**
   * Returns a specific repetition of
   * OBX-16: "Responsible Observer" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getResponsibleObserver(int rep) {
    XCN retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBX-16: "Responsible Observer" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getObx16_ResponsibleObserver(int rep) {
    XCN retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Responsible Observer (OBX-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObx16_ResponsibleObserverReps() {
    return this.getReps(16);
  }


  /**
   * Inserts a repetition of
   * OBX-16: "Responsible Observer" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertResponsibleObserver(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(16, rep);
  }


  /**
   * Inserts a repetition of
   * OBX-16: "Responsible Observer" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertObx16_ResponsibleObserver(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * OBX-16: "Responsible Observer" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeResponsibleObserver(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * OBX-16: "Responsible Observer" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeObx16_ResponsibleObserver(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(16, rep);
  }


  /**
   * Returns all repetitions of Observation Method (OBX-17).
   */
  public CWE_OBX_17[] getObservationMethod() {
    CWE_OBX_17[] retVal = this.getTypedField(17, new CWE_OBX_17[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Observation Method (OBX-17).
   */
  public CWE_OBX_17[] getObx17_ObservationMethod() {
    CWE_OBX_17[] retVal = this.getTypedField(17, new CWE_OBX_17[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Observation Method (OBX-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObservationMethodReps() {
    return this.getReps(17);
  }


  /**
   * Returns a specific repetition of
   * OBX-17: "Observation Method" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_OBX_17 getObservationMethod(int rep) {
    CWE_OBX_17 retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBX-17: "Observation Method" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_OBX_17 getObx17_ObservationMethod(int rep) {
    CWE_OBX_17 retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Observation Method (OBX-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObx17_ObservationMethodReps() {
    return this.getReps(17);
  }


  /**
   * Inserts a repetition of
   * OBX-17: "Observation Method" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_OBX_17 insertObservationMethod(int rep) throws HL7Exception {
    return (CWE_OBX_17) super.insertRepetition(17, rep);
  }


  /**
   * Inserts a repetition of
   * OBX-17: "Observation Method" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_OBX_17 insertObx17_ObservationMethod(int rep) throws HL7Exception {
    return (CWE_OBX_17) super.insertRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * OBX-17: "Observation Method" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_OBX_17 removeObservationMethod(int rep) throws HL7Exception {
    return (CWE_OBX_17) super.removeRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * OBX-17: "Observation Method" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_OBX_17 removeObx17_ObservationMethod(int rep) throws HL7Exception {
    return (CWE_OBX_17) super.removeRepetition(17, rep);
  }


  /**
   * Returns all repetitions of Equipment Instance Identifier (OBX-18).
   */
  public EI_NIH[] getEquipmentInstanceIdentifier() {
    EI_NIH[] retVal = this.getTypedField(18, new EI_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Equipment Instance Identifier (OBX-18).
   */
  public EI_NIH[] getObx18_EquipmentInstanceIdentifier() {
    EI_NIH[] retVal = this.getTypedField(18, new EI_NIH[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Equipment Instance Identifier (OBX-18).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEquipmentInstanceIdentifierReps() {
    return this.getReps(18);
  }


  /**
   * Returns a specific repetition of
   * OBX-18: "Equipment Instance Identifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public EI_NIH getEquipmentInstanceIdentifier(int rep) {
    EI_NIH retVal = this.getTypedField(18, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBX-18: "Equipment Instance Identifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public EI_NIH getObx18_EquipmentInstanceIdentifier(int rep) {
    EI_NIH retVal = this.getTypedField(18, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Equipment Instance Identifier (OBX-18).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObx18_EquipmentInstanceIdentifierReps() {
    return this.getReps(18);
  }


  /**
   * Inserts a repetition of
   * OBX-18: "Equipment Instance Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EI_NIH insertEquipmentInstanceIdentifier(int rep) throws HL7Exception {
    return (EI_NIH) super.insertRepetition(18, rep);
  }


  /**
   * Inserts a repetition of
   * OBX-18: "Equipment Instance Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EI_NIH insertObx18_EquipmentInstanceIdentifier(int rep) throws HL7Exception {
    return (EI_NIH) super.insertRepetition(18, rep);
  }


  /**
   * Removes a repetition of
   * OBX-18: "Equipment Instance Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EI_NIH removeEquipmentInstanceIdentifier(int rep) throws HL7Exception {
    return (EI_NIH) super.removeRepetition(18, rep);
  }


  /**
   * Removes a repetition of
   * OBX-18: "Equipment Instance Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EI_NIH removeObx18_EquipmentInstanceIdentifier(int rep) throws HL7Exception {
    return (EI_NIH) super.removeRepetition(18, rep);
  }


  /**
   * Returns
   * OBX-19: "Date/Time of the Analysis" - creates it if necessary
   */
  public TS_NIH2 getDateTimeOfTheAnalysis() {
    TS_NIH2 retVal = this.getTypedField(19, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-19: "Date/Time of the Analysis" - creates it if necessary
   */
  public TS_NIH2 getObx19_DateTimeOfTheAnalysis() {
    TS_NIH2 retVal = this.getTypedField(19, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Observation Site (OBX-20).
   */
  public NULLDT[] getObservationSite() {
    NULLDT[] retVal = this.getTypedField(20, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Observation Site (OBX-20).
   */
  public NULLDT[] getObx20_ObservationSite() {
    NULLDT[] retVal = this.getTypedField(20, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Observation Site (OBX-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObservationSiteReps() {
    return this.getReps(20);
  }


  /**
   * Returns a specific repetition of
   * OBX-20: "Observation Site" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getObservationSite(int rep) {
    NULLDT retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBX-20: "Observation Site" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getObx20_ObservationSite(int rep) {
    NULLDT retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Observation Site (OBX-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObx20_ObservationSiteReps() {
    return this.getReps(20);
  }


  /**
   * Inserts a repetition of
   * OBX-20: "Observation Site" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertObservationSite(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(20, rep);
  }


  /**
   * Inserts a repetition of
   * OBX-20: "Observation Site" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertObx20_ObservationSite(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * OBX-20: "Observation Site" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeObservationSite(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * OBX-20: "Observation Site" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeObx20_ObservationSite(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(20, rep);
  }


  /**
   * Returns
   * OBX-21: "Observation Instance Identifier" - creates it if necessary
   */
  public NULLDT getObservationInstanceIdentifier() {
    NULLDT retVal = this.getTypedField(21, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-21: "Observation Instance Identifier" - creates it if necessary
   */
  public NULLDT getObx21_ObservationInstanceIdentifier() {
    NULLDT retVal = this.getTypedField(21, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-22: "Mood Code" - creates it if necessary
   */
  public NULLDT getMoodCode() {
    NULLDT retVal = this.getTypedField(22, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-22: "Mood Code" - creates it if necessary
   */
  public NULLDT getObx22_MoodCode() {
    NULLDT retVal = this.getTypedField(22, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-23: "Performing Organization Name" - creates it if necessary
   */
  public XON_OBX_23 getPerformingOrganizationName() {
    XON_OBX_23 retVal = this.getTypedField(23, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-23: "Performing Organization Name" - creates it if necessary
   */
  public XON_OBX_23 getObx23_PerformingOrganizationName() {
    XON_OBX_23 retVal = this.getTypedField(23, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-24: "Performing Organization Address" - creates it if necessary
   */
  public XAD_OBX_24 getPerformingOrganizationAddress() {
    XAD_OBX_24 retVal = this.getTypedField(24, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-24: "Performing Organization Address" - creates it if necessary
   */
  public XAD_OBX_24 getObx24_PerformingOrganizationAddress() {
    XAD_OBX_24 retVal = this.getTypedField(24, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-25: "Performing Organization Medical Director" - creates it if necessary
   */
  public XCN_NIH getPerformingOrganizationMedicalDirector() {
    XCN_NIH retVal = this.getTypedField(25, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-25: "Performing Organization Medical Director" - creates it if necessary
   */
  public XCN_NIH getObx25_PerformingOrganizationMedicalDirector() {
    XCN_NIH retVal = this.getTypedField(25, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-26: "Patient Results Release Category" - creates it if necessary
   */
  public ID getPatientResultsReleaseCategory() {
    ID retVal = this.getTypedField(26, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-26: "Patient Results Release Category" - creates it if necessary
   */
  public ID getObx26_PatientResultsReleaseCategory() {
    ID retVal = this.getTypedField(26, 0);
    return retVal;
  }


  /**
   * Returns
   * OBX-27: "Root Cause" - creates it if necessary
   */
  public CWE_NIH getRootCause() {
    CWE_NIH retVal = this.getTypedField(27, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-27: "Root Cause" - creates it if necessary
   */
  public CWE_NIH getObx27_RootCause() {
    CWE_NIH retVal = this.getTypedField(27, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Local Process Control (OBX-28).
   */
  public CWE_NIH[] getLocalProcessControl() {
    CWE_NIH[] retVal = this.getTypedField(28, new CWE_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Local Process Control (OBX-28).
   */
  public CWE_NIH[] getObx28_LocalProcessControl() {
    CWE_NIH[] retVal = this.getTypedField(28, new CWE_NIH[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Local Process Control (OBX-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getLocalProcessControlReps() {
    return this.getReps(28);
  }


  /**
   * Returns a specific repetition of
   * OBX-28: "Local Process Control" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_NIH getLocalProcessControl(int rep) {
    CWE_NIH retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBX-28: "Local Process Control" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_NIH getObx28_LocalProcessControl(int rep) {
    CWE_NIH retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Local Process Control (OBX-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObx28_LocalProcessControlReps() {
    return this.getReps(28);
  }


  /**
   * Inserts a repetition of
   * OBX-28: "Local Process Control" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH insertLocalProcessControl(int rep) throws HL7Exception {
    return (CWE_NIH) super.insertRepetition(28, rep);
  }


  /**
   * Inserts a repetition of
   * OBX-28: "Local Process Control" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH insertObx28_LocalProcessControl(int rep) throws HL7Exception {
    return (CWE_NIH) super.insertRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * OBX-28: "Local Process Control" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH removeLocalProcessControl(int rep) throws HL7Exception {
    return (CWE_NIH) super.removeRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * OBX-28: "Local Process Control" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_NIH removeObx28_LocalProcessControl(int rep) throws HL7Exception {
    return (CWE_NIH) super.removeRepetition(28, rep);
  }


  /**
   * Returns
   * OBX-29: "Observation Type" - creates it if necessary
   */
  public ID getObservationType() {
    ID retVal = this.getTypedField(29, 0);
    return retVal;
  }

  /**
   * Returns
   * OBX-29: "Observation Type" - creates it if necessary
   */
  public ID getObx29_ObservationType() {
    ID retVal = this.getTypedField(29, 0);
    return retVal;
  }


  /**
   * {@inheritDoc}
   */
  protected Type createNewTypeWithoutReflection(int field) {
    switch (field) {
      case 0:
        return new SI(getMessage());
      case 1:
        return new ID(getMessage(), Integer.valueOf(125));
      case 2:
        return new CWE_NIH(getMessage());
      case 3:
        return new ST(getMessage());
      case 4:
        return new Varies(getMessage());
      case 5:
        return new CWE_NIH(getMessage());
      case 6:
        return new ST(getMessage());
      case 7:
        return new CWE_NIH(getMessage());
      case 8:
        return new NM(getMessage());
      case 9:
        return new ID(getMessage(), Integer.valueOf(80));
      case 10:
        return new ID(getMessage(), Integer.valueOf(85));
      case 11:
        return new TS_NIH(getMessage());
      case 12:
        return new ST(getMessage());
      case 13:
        return new TS_NIH(getMessage());
      case 14:
        return new CWE_OBX_15(getMessage());
      case 15:
        return new XCN(getMessage());
      case 16:
        return new CWE_OBX_17(getMessage());
      case 17:
        return new EI_NIH(getMessage());
      case 18:
        return new TS_NIH(getMessage());
      case 19:
        return new NULLDT(getMessage());
      case 20:
        return new NULLDT(getMessage());
      case 21:
        return new NULLDT(getMessage());
      case 22:
        return new XON_OBX_23(getMessage());
      case 23:
        return new XAD_OBX_24(getMessage());
      case 24:
        return new XCN_NIH(getMessage());
      case 25:
        return new ID(getMessage());
      case 26:
        return new CWE_NIH(getMessage());
      case 27:
        return new ID(getMessage());
      case 28:
        return new ID(getMessage());
      default:
        return null;
    }
  }


}

