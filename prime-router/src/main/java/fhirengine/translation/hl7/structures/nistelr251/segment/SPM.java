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
import ca.uhn.hl7v2.model.v251.datatype.CQ;
import ca.uhn.hl7v2.model.v251.datatype.CWE;
import ca.uhn.hl7v2.model.v251.datatype.EIP;
import ca.uhn.hl7v2.model.v251.datatype.ID;
import ca.uhn.hl7v2.model.v251.datatype.NM;
import ca.uhn.hl7v2.model.v251.datatype.SI;
import ca.uhn.hl7v2.model.v251.datatype.ST;
import ca.uhn.hl7v2.model.v251.datatype.TS;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.nistelr251.datatype.*;


/**
 * <p>Represents an HL7 SPM message segment (Specimen).
 * This segment has the following fields:</p>
 * <ul>
 * <li>SPM-1: Set ID - SPM (SI) <b> </b>
 * <li>SPM-2: Specimen ID (EIP_ELR) <b> </b>
 * <li>SPM-3: Specimen Parent IDs (EIP) <b>optional repeating</b>
 * <li>SPM-4: Specimen Type (CWE_ELR) <b> </b>
 * <li>SPM-5: Specimen Type Modifier (CWE_ELR) <b>optional repeating</b>
 * <li>SPM-6: Specimen Additives (CWE_ELR) <b>optional repeating</b>
 * <li>SPM-7: Specimen Collection Method (CWE_ELR) <b>optional </b>
 * <li>SPM-8: Specimen Source Site (CWE_ELR) <b>optional </b>
 * <li>SPM-9: Specimen Source Site Modifier (CWE_ELR) <b>optional repeating</b>
 * <li>SPM-10: Specimen Collection Site (CWE) <b>optional </b>
 * <li>SPM-11: Specimen Role (CWE_ELR) <b>optional repeating</b>
 * <li>SPM-12: Specimen Collection Amount (CQ_ELR) <b>optional </b>
 * <li>SPM-13: Grouped Specimen Count (NM) <b>optional </b>
 * <li>SPM-14: Specimen Description (ST) <b>optional repeating</b>
 * <li>SPM-15: Specimen Handling Code (CWE) <b>optional repeating</b>
 * <li>SPM-16: Specimen Risk Code (CWE) <b>optional repeating</b>
 * <li>SPM-17: Specimen Collection Date/Time (DR_ELR) <b> </b>
 * <li>SPM-18: Specimen Received Date/Time (TS_ELR) <b> </b>
 * <li>SPM-19: Specimen Expiration Date/Time (TS) <b>optional </b>
 * <li>SPM-20: Specimen Availability (ID) <b>optional </b>
 * <li>SPM-21: Specimen Reject Reason (CWE_ELR) <b>optional repeating</b>
 * <li>SPM-22: Specimen Quality (CWE) <b>optional </b>
 * <li>SPM-23: Specimen Appropriateness (CWE) <b>optional </b>
 * <li>SPM-24: Specimen Condition (CWE) <b>optional repeating</b>
 * <li>SPM-25: Specimen Current Quantity (CQ) <b>optional </b>
 * <li>SPM-26: Number of Specimen Containers (NM) <b>optional </b>
 * <li>SPM-27: Container Type (CWE) <b>optional </b>
 * <li>SPM-28: Container Condition (CWE) <b>optional </b>
 * <li>SPM-29: Specimen Child Role (CWE) <b>optional </b>
 * </ul>
 */
@SuppressWarnings("unused")
public class SPM extends AbstractSegment {

  /**
   * Creates a new SPM segment
   */
  public SPM(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(SI.class, true, 1, 0, new Object[]{getMessage()}, "Set ID - SPM");
      this.add(EIP_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Specimen ID");
      this.add(EIP.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Parent IDs");
      this.add(CWE_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Specimen Type");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Type Modifier");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Additives");
      this.add(CWE_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Collection Method");
      this.add(CWE_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Source Site");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Source Site Modifier");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Collection Site");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Role");
      this.add(CQ_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Collection Amount");
      this.add(NM.class, false, 1, 0, new Object[]{getMessage()}, "Grouped Specimen Count");
      this.add(ST.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Description");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Handling Code");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Risk Code");
      this.add(DR_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Specimen Collection Date/Time");
      this.add(TS_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Specimen Received Date/Time");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Expiration Date/Time");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), 136}, "Specimen Availability");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Reject Reason");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Quality");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Appropriateness");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Specimen Condition");
      this.add(CQ.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Current Quantity");
      this.add(NM.class, false, 1, 0, new Object[]{getMessage()}, "Number of Specimen Containers");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Container Type");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Container Condition");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Specimen Child Role");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating SPM - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns
   * SPM-1: "Set ID - SPM" - creates it if necessary
   */
  public SI getSetIDSPM() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-1: "Set ID - SPM" - creates it if necessary
   */
  public SI getSpm1_SetIDSPM() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-2: "Specimen ID" - creates it if necessary
   */
  public EIP_ELR getSpecimenID() {
    EIP_ELR retVal = this.getTypedField(2, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-2: "Specimen ID" - creates it if necessary
   */
  public EIP_ELR getSpm2_SpecimenID() {
    EIP_ELR retVal = this.getTypedField(2, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Parent IDs (SPM-3).
   */
  public EIP[] getSpecimenParentIDs() {
    EIP[] retVal = this.getTypedField(3, new EIP[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Parent IDs (SPM-3).
   */
  public EIP[] getSpm3_SpecimenParentIDs() {
    EIP[] retVal = this.getTypedField(3, new EIP[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Parent IDs (SPM-3).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenParentIDsReps() {
    return this.getReps(3);
  }


  /**
   * Returns a specific repetition of
   * SPM-3: "Specimen Parent IDs" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public EIP getSpecimenParentIDs(int rep) {
    EIP retVal = this.getTypedField(3, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-3: "Specimen Parent IDs" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public EIP getSpm3_SpecimenParentIDs(int rep) {
    EIP retVal = this.getTypedField(3, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Parent IDs (SPM-3).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm3_SpecimenParentIDsReps() {
    return this.getReps(3);
  }


  /**
   * Inserts a repetition of
   * SPM-3: "Specimen Parent IDs" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EIP insertSpecimenParentIDs(int rep) throws HL7Exception {
    return (EIP) super.insertRepetition(3, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-3: "Specimen Parent IDs" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EIP insertSpm3_SpecimenParentIDs(int rep) throws HL7Exception {
    return (EIP) super.insertRepetition(3, rep);
  }


  /**
   * Removes a repetition of
   * SPM-3: "Specimen Parent IDs" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EIP removeSpecimenParentIDs(int rep) throws HL7Exception {
    return (EIP) super.removeRepetition(3, rep);
  }


  /**
   * Removes a repetition of
   * SPM-3: "Specimen Parent IDs" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public EIP removeSpm3_SpecimenParentIDs(int rep) throws HL7Exception {
    return (EIP) super.removeRepetition(3, rep);
  }


  /**
   * Returns
   * SPM-4: "Specimen Type" - creates it if necessary
   */
  public CWE_ELR getSpecimenType() {
    CWE_ELR retVal = this.getTypedField(4, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-4: "Specimen Type" - creates it if necessary
   */
  public CWE_ELR getSpm4_SpecimenType() {
    CWE_ELR retVal = this.getTypedField(4, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Type Modifier (SPM-5).
   */
  public CWE_ELR[] getSpecimenTypeModifier() {
    CWE_ELR[] retVal = this.getTypedField(5, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Type Modifier (SPM-5).
   */
  public CWE_ELR[] getSpm5_SpecimenTypeModifier() {
    CWE_ELR[] retVal = this.getTypedField(5, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Type Modifier (SPM-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenTypeModifierReps() {
    return this.getReps(5);
  }


  /**
   * Returns a specific repetition of
   * SPM-5: "Specimen Type Modifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpecimenTypeModifier(int rep) {
    CWE_ELR retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-5: "Specimen Type Modifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpm5_SpecimenTypeModifier(int rep) {
    CWE_ELR retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Type Modifier (SPM-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm5_SpecimenTypeModifierReps() {
    return this.getReps(5);
  }


  /**
   * Inserts a repetition of
   * SPM-5: "Specimen Type Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpecimenTypeModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(5, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-5: "Specimen Type Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpm5_SpecimenTypeModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * SPM-5: "Specimen Type Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpecimenTypeModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * SPM-5: "Specimen Type Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpm5_SpecimenTypeModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(5, rep);
  }


  /**
   * Returns all repetitions of Specimen Additives (SPM-6).
   */
  public CWE_ELR[] getSpecimenAdditives() {
    CWE_ELR[] retVal = this.getTypedField(6, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Additives (SPM-6).
   */
  public CWE_ELR[] getSpm6_SpecimenAdditives() {
    CWE_ELR[] retVal = this.getTypedField(6, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Additives (SPM-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenAdditivesReps() {
    return this.getReps(6);
  }


  /**
   * Returns a specific repetition of
   * SPM-6: "Specimen Additives" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpecimenAdditives(int rep) {
    CWE_ELR retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-6: "Specimen Additives" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpm6_SpecimenAdditives(int rep) {
    CWE_ELR retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Additives (SPM-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm6_SpecimenAdditivesReps() {
    return this.getReps(6);
  }


  /**
   * Inserts a repetition of
   * SPM-6: "Specimen Additives" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpecimenAdditives(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(6, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-6: "Specimen Additives" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpm6_SpecimenAdditives(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * SPM-6: "Specimen Additives" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpecimenAdditives(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * SPM-6: "Specimen Additives" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpm6_SpecimenAdditives(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(6, rep);
  }


  /**
   * Returns
   * SPM-7: "Specimen Collection Method" - creates it if necessary
   */
  public CWE_ELR getSpecimenCollectionMethod() {
    CWE_ELR retVal = this.getTypedField(7, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-7: "Specimen Collection Method" - creates it if necessary
   */
  public CWE_ELR getSpm7_SpecimenCollectionMethod() {
    CWE_ELR retVal = this.getTypedField(7, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-8: "Specimen Source Site" - creates it if necessary
   */
  public CWE_ELR getSpecimenSourceSite() {
    CWE_ELR retVal = this.getTypedField(8, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-8: "Specimen Source Site" - creates it if necessary
   */
  public CWE_ELR getSpm8_SpecimenSourceSite() {
    CWE_ELR retVal = this.getTypedField(8, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Source Site Modifier (SPM-9).
   */
  public CWE_ELR[] getSpecimenSourceSiteModifier() {
    CWE_ELR[] retVal = this.getTypedField(9, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Source Site Modifier (SPM-9).
   */
  public CWE_ELR[] getSpm9_SpecimenSourceSiteModifier() {
    CWE_ELR[] retVal = this.getTypedField(9, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Source Site Modifier (SPM-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenSourceSiteModifierReps() {
    return this.getReps(9);
  }


  /**
   * Returns a specific repetition of
   * SPM-9: "Specimen Source Site Modifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpecimenSourceSiteModifier(int rep) {
    CWE_ELR retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-9: "Specimen Source Site Modifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpm9_SpecimenSourceSiteModifier(int rep) {
    CWE_ELR retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Source Site Modifier (SPM-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm9_SpecimenSourceSiteModifierReps() {
    return this.getReps(9);
  }


  /**
   * Inserts a repetition of
   * SPM-9: "Specimen Source Site Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpecimenSourceSiteModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(9, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-9: "Specimen Source Site Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpm9_SpecimenSourceSiteModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * SPM-9: "Specimen Source Site Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpecimenSourceSiteModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * SPM-9: "Specimen Source Site Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpm9_SpecimenSourceSiteModifier(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(9, rep);
  }


  /**
   * Returns
   * SPM-10: "Specimen Collection Site" - creates it if necessary
   */
  public CWE getSpecimenCollectionSite() {
    CWE retVal = this.getTypedField(10, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-10: "Specimen Collection Site" - creates it if necessary
   */
  public CWE getSpm10_SpecimenCollectionSite() {
    CWE retVal = this.getTypedField(10, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Role (SPM-11).
   */
  public CWE_ELR[] getSpecimenRole() {
    CWE_ELR[] retVal = this.getTypedField(11, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Role (SPM-11).
   */
  public CWE_ELR[] getSpm11_SpecimenRole() {
    CWE_ELR[] retVal = this.getTypedField(11, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Role (SPM-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenRoleReps() {
    return this.getReps(11);
  }


  /**
   * Returns a specific repetition of
   * SPM-11: "Specimen Role" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpecimenRole(int rep) {
    CWE_ELR retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-11: "Specimen Role" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpm11_SpecimenRole(int rep) {
    CWE_ELR retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Role (SPM-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm11_SpecimenRoleReps() {
    return this.getReps(11);
  }


  /**
   * Inserts a repetition of
   * SPM-11: "Specimen Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpecimenRole(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(11, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-11: "Specimen Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpm11_SpecimenRole(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * SPM-11: "Specimen Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpecimenRole(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * SPM-11: "Specimen Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpm11_SpecimenRole(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(11, rep);
  }


  /**
   * Returns
   * SPM-12: "Specimen Collection Amount" - creates it if necessary
   */
  public CQ_ELR getSpecimenCollectionAmount() {
    CQ_ELR retVal = this.getTypedField(12, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-12: "Specimen Collection Amount" - creates it if necessary
   */
  public CQ_ELR getSpm12_SpecimenCollectionAmount() {
    CQ_ELR retVal = this.getTypedField(12, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-13: "Grouped Specimen Count" - creates it if necessary
   */
  public NM getGroupedSpecimenCount() {
    NM retVal = this.getTypedField(13, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-13: "Grouped Specimen Count" - creates it if necessary
   */
  public NM getSpm13_GroupedSpecimenCount() {
    NM retVal = this.getTypedField(13, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Description (SPM-14).
   */
  public ST[] getSpecimenDescription() {
    ST[] retVal = this.getTypedField(14, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Description (SPM-14).
   */
  public ST[] getSpm14_SpecimenDescription() {
    ST[] retVal = this.getTypedField(14, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Description (SPM-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenDescriptionReps() {
    return this.getReps(14);
  }


  /**
   * Returns a specific repetition of
   * SPM-14: "Specimen Description" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getSpecimenDescription(int rep) {
    ST retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-14: "Specimen Description" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getSpm14_SpecimenDescription(int rep) {
    ST retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Description (SPM-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm14_SpecimenDescriptionReps() {
    return this.getReps(14);
  }


  /**
   * Inserts a repetition of
   * SPM-14: "Specimen Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertSpecimenDescription(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(14, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-14: "Specimen Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertSpm14_SpecimenDescription(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * SPM-14: "Specimen Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeSpecimenDescription(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * SPM-14: "Specimen Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeSpm14_SpecimenDescription(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(14, rep);
  }


  /**
   * Returns all repetitions of Specimen Handling Code (SPM-15).
   */
  public CWE[] getSpecimenHandlingCode() {
    CWE[] retVal = this.getTypedField(15, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Handling Code (SPM-15).
   */
  public CWE[] getSpm15_SpecimenHandlingCode() {
    CWE[] retVal = this.getTypedField(15, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Handling Code (SPM-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenHandlingCodeReps() {
    return this.getReps(15);
  }


  /**
   * Returns a specific repetition of
   * SPM-15: "Specimen Handling Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getSpecimenHandlingCode(int rep) {
    CWE retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-15: "Specimen Handling Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getSpm15_SpecimenHandlingCode(int rep) {
    CWE retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Handling Code (SPM-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm15_SpecimenHandlingCodeReps() {
    return this.getReps(15);
  }


  /**
   * Inserts a repetition of
   * SPM-15: "Specimen Handling Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertSpecimenHandlingCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(15, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-15: "Specimen Handling Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertSpm15_SpecimenHandlingCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * SPM-15: "Specimen Handling Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeSpecimenHandlingCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * SPM-15: "Specimen Handling Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeSpm15_SpecimenHandlingCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(15, rep);
  }


  /**
   * Returns all repetitions of Specimen Risk Code (SPM-16).
   */
  public CWE[] getSpecimenRiskCode() {
    CWE[] retVal = this.getTypedField(16, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Risk Code (SPM-16).
   */
  public CWE[] getSpm16_SpecimenRiskCode() {
    CWE[] retVal = this.getTypedField(16, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Risk Code (SPM-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenRiskCodeReps() {
    return this.getReps(16);
  }


  /**
   * Returns a specific repetition of
   * SPM-16: "Specimen Risk Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getSpecimenRiskCode(int rep) {
    CWE retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-16: "Specimen Risk Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getSpm16_SpecimenRiskCode(int rep) {
    CWE retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Risk Code (SPM-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm16_SpecimenRiskCodeReps() {
    return this.getReps(16);
  }


  /**
   * Inserts a repetition of
   * SPM-16: "Specimen Risk Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertSpecimenRiskCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(16, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-16: "Specimen Risk Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertSpm16_SpecimenRiskCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * SPM-16: "Specimen Risk Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeSpecimenRiskCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * SPM-16: "Specimen Risk Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeSpm16_SpecimenRiskCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(16, rep);
  }


  /**
   * Returns
   * SPM-17: "Specimen Collection Date/Time" - creates it if necessary
   */
  public DR_ELR getSpecimenCollectionDateTime() {
    DR_ELR retVal = this.getTypedField(17, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-17: "Specimen Collection Date/Time" - creates it if necessary
   */
  public DR_ELR getSpm17_SpecimenCollectionDateTime() {
    DR_ELR retVal = this.getTypedField(17, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-18: "Specimen Received Date/Time" - creates it if necessary
   */
  public TS_ELR getSpecimenReceivedDateTime() {
    TS_ELR retVal = this.getTypedField(18, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-18: "Specimen Received Date/Time" - creates it if necessary
   */
  public TS_ELR getSpm18_SpecimenReceivedDateTime() {
    TS_ELR retVal = this.getTypedField(18, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-19: "Specimen Expiration Date/Time" - creates it if necessary
   */
  public TS getSpecimenExpirationDateTime() {
    TS retVal = this.getTypedField(19, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-19: "Specimen Expiration Date/Time" - creates it if necessary
   */
  public TS getSpm19_SpecimenExpirationDateTime() {
    TS retVal = this.getTypedField(19, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-20: "Specimen Availability" - creates it if necessary
   */
  public ID getSpecimenAvailability() {
    ID retVal = this.getTypedField(20, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-20: "Specimen Availability" - creates it if necessary
   */
  public ID getSpm20_SpecimenAvailability() {
    ID retVal = this.getTypedField(20, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Reject Reason (SPM-21).
   */
  public CWE_ELR[] getSpecimenRejectReason() {
    CWE_ELR[] retVal = this.getTypedField(21, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Reject Reason (SPM-21).
   */
  public CWE_ELR[] getSpm21_SpecimenRejectReason() {
    CWE_ELR[] retVal = this.getTypedField(21, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Reject Reason (SPM-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenRejectReasonReps() {
    return this.getReps(21);
  }


  /**
   * Returns a specific repetition of
   * SPM-21: "Specimen Reject Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpecimenRejectReason(int rep) {
    CWE_ELR retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-21: "Specimen Reject Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getSpm21_SpecimenRejectReason(int rep) {
    CWE_ELR retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Reject Reason (SPM-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm21_SpecimenRejectReasonReps() {
    return this.getReps(21);
  }


  /**
   * Inserts a repetition of
   * SPM-21: "Specimen Reject Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpecimenRejectReason(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(21, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-21: "Specimen Reject Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertSpm21_SpecimenRejectReason(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * SPM-21: "Specimen Reject Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpecimenRejectReason(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * SPM-21: "Specimen Reject Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeSpm21_SpecimenRejectReason(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(21, rep);
  }


  /**
   * Returns
   * SPM-22: "Specimen Quality" - creates it if necessary
   */
  public CWE getSpecimenQuality() {
    CWE retVal = this.getTypedField(22, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-22: "Specimen Quality" - creates it if necessary
   */
  public CWE getSpm22_SpecimenQuality() {
    CWE retVal = this.getTypedField(22, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-23: "Specimen Appropriateness" - creates it if necessary
   */
  public CWE getSpecimenAppropriateness() {
    CWE retVal = this.getTypedField(23, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-23: "Specimen Appropriateness" - creates it if necessary
   */
  public CWE getSpm23_SpecimenAppropriateness() {
    CWE retVal = this.getTypedField(23, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Condition (SPM-24).
   */
  public CWE[] getSpecimenCondition() {
    CWE[] retVal = this.getTypedField(24, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Condition (SPM-24).
   */
  public CWE[] getSpm24_SpecimenCondition() {
    CWE[] retVal = this.getTypedField(24, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Condition (SPM-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenConditionReps() {
    return this.getReps(24);
  }


  /**
   * Returns a specific repetition of
   * SPM-24: "Specimen Condition" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getSpecimenCondition(int rep) {
    CWE retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * SPM-24: "Specimen Condition" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getSpm24_SpecimenCondition(int rep) {
    CWE retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Condition (SPM-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpm24_SpecimenConditionReps() {
    return this.getReps(24);
  }


  /**
   * Inserts a repetition of
   * SPM-24: "Specimen Condition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertSpecimenCondition(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(24, rep);
  }


  /**
   * Inserts a repetition of
   * SPM-24: "Specimen Condition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertSpm24_SpecimenCondition(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * SPM-24: "Specimen Condition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeSpecimenCondition(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * SPM-24: "Specimen Condition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeSpm24_SpecimenCondition(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(24, rep);
  }


  /**
   * Returns
   * SPM-25: "Specimen Current Quantity" - creates it if necessary
   */
  public CQ getSpecimenCurrentQuantity() {
    CQ retVal = this.getTypedField(25, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-25: "Specimen Current Quantity" - creates it if necessary
   */
  public CQ getSpm25_SpecimenCurrentQuantity() {
    CQ retVal = this.getTypedField(25, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-26: "Number of Specimen Containers" - creates it if necessary
   */
  public NM getNumberOfSpecimenContainers() {
    NM retVal = this.getTypedField(26, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-26: "Number of Specimen Containers" - creates it if necessary
   */
  public NM getSpm26_NumberOfSpecimenContainers() {
    NM retVal = this.getTypedField(26, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-27: "Container Type" - creates it if necessary
   */
  public CWE getContainerType() {
    CWE retVal = this.getTypedField(27, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-27: "Container Type" - creates it if necessary
   */
  public CWE getSpm27_ContainerType() {
    CWE retVal = this.getTypedField(27, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-28: "Container Condition" - creates it if necessary
   */
  public CWE getContainerCondition() {
    CWE retVal = this.getTypedField(28, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-28: "Container Condition" - creates it if necessary
   */
  public CWE getSpm28_ContainerCondition() {
    CWE retVal = this.getTypedField(28, 0);
    return retVal;
  }


  /**
   * Returns
   * SPM-29: "Specimen Child Role" - creates it if necessary
   */
  public CWE getSpecimenChildRole() {
    CWE retVal = this.getTypedField(29, 0);
    return retVal;
  }

  /**
   * Returns
   * SPM-29: "Specimen Child Role" - creates it if necessary
   */
  public CWE getSpm29_SpecimenChildRole() {
    CWE retVal = this.getTypedField(29, 0);
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
        return new EIP_ELR(getMessage());
      case 2:
        return new EIP(getMessage());
      case 3:
        return new CWE_ELR(getMessage());
      case 4:
        return new CWE_ELR(getMessage());
      case 5:
        return new CWE_ELR(getMessage());
      case 6:
        return new CWE_ELR(getMessage());
      case 7:
        return new CWE_ELR(getMessage());
      case 8:
        return new CWE_ELR(getMessage());
      case 9:
        return new CWE(getMessage());
      case 10:
        return new CWE_ELR(getMessage());
      case 11:
        return new CQ_ELR(getMessage());
      case 12:
        return new NM(getMessage());
      case 13:
        return new ST(getMessage());
      case 14:
        return new CWE(getMessage());
      case 15:
        return new CWE(getMessage());
      case 16:
        return new DR_ELR(getMessage());
      case 17:
        return new TS_ELR(getMessage());
      case 18:
        return new TS(getMessage());
      case 19:
        return new ID(getMessage(), 136);
      case 20:
        return new CWE_ELR(getMessage());
      case 21:
        return new CWE(getMessage());
      case 22:
        return new CWE(getMessage());
      case 23:
        return new CWE(getMessage());
      case 24:
        return new CQ(getMessage());
      case 25:
        return new NM(getMessage());
      case 26:
        return new CWE(getMessage());
      case 27:
        return new CWE(getMessage());
      case 28:
        return new CWE(getMessage());
      default:
        return null;
    }
  }


}

