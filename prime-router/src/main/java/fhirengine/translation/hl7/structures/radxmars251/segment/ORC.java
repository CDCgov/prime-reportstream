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


import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v251.datatype.*;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.radxmars251.datatype.*;


/**
 * <p>Represents an HL7 ORC message segment (Common Order).
 * This segment has the following fields:</p>
 * <ul>
 * <li>ORC-1: Order Control (ID) <b> </b>
 * <li>ORC-2: Placer Order Number (EI) <b>optional </b>
 * <li>ORC-3: Filler Order Number (EI) <b> </b>
 * <li>ORC-4: Placer Group Number (EI) <b>optional </b>
 * <li>ORC-5: Order Status (ID) <b>optional </b>
 * <li>ORC-6: Response Flag (ID) <b>optional </b>
 * <li>ORC-7: Quantity/Timing (TQ) <b>optional repeating</b>
 * <li>ORC-8: Parent (EIP) <b>optional </b>
 * <li>ORC-9: Date/Time of Transaction (TS) <b>optional </b>
 * <li>ORC-10: Entered By (XCN_NIH) <b>optional repeating</b>
 * <li>ORC-11: Verified By (XCN_NIH) <b>optional repeating</b>
 * <li>ORC-12: Ordering Provider (XCN_NIH2) <b>optional repeating</b>
 * <li>ORC-13: Enterer's Location (PL) <b>optional </b>
 * <li>ORC-14: Call Back Phone Number (XTN) <b>optional repeating</b>
 * <li>ORC-15: Order Effective Date/Time (TS) <b>optional </b>
 * <li>ORC-16: Order Control Code Reason (CWE_NIH) <b>optional </b>
 * <li>ORC-17: Entering Organization (CWE_NIH) <b>optional </b>
 * <li>ORC-18: Entering Device (CWE_NIH) <b>optional </b>
 * <li>ORC-19: Action By (XCN_NIH) <b>optional repeating</b>
 * <li>ORC-20: Advanced Beneficiary Notice Code (CWE_NIH) <b>optional repeating</b>
 * <li>ORC-21: Ordering Facility Name (XON_ORC-21) <b> </b>
 * <li>ORC-22: Ordering Facility Address (XAD_NIH) <b> </b>
 * <li>ORC-23: Ordering Facility Phone Number (XTN_ORC-23) <b> repeating</b>
 * <li>ORC-24: Ordering Provider Address (XAD_ORC-24) <b>optional repeating</b>
 * <li>ORC-25: Order Status Modifier (CWE_NIH) <b>optional </b>
 * <li>ORC-26: Advanced Beneficiary Notice Override Reason (CWE_NIH) <b>optional repeating</b>
 * <li>ORC-27: Filler's Expected Availability Date/Time (TS) <b>optional </b>
 * <li>ORC-28: Confidentiality Code (CWE_NIH) <b>optional </b>
 * <li>ORC-29: Order Type (CWE_NIH) <b>optional </b>
 * <li>ORC-30: Enterer Authorization Mode (CNE) <b>optional </b>
 * <li>ORC-31: Parent Universal Service Identifier (CWE_NIH) <b>optional </b>
 * </ul>
 */
@SuppressWarnings("unused")
public class ORC extends AbstractSegment {

  /**
   * Creates a new ORC segment
   */
  public ORC(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(ID.class, true, 1, 0, new Object[]{getMessage(), Integer.valueOf(119)}, "Order Control");
      this.add(NULLDT.class, false, 1, 0, new Object[]{getMessage()}, "Placer Order Number");
      this.add(EI_NIH3.class, true, 1, 0, new Object[]{getMessage()}, "Filler Order Number");
      this.add(EI_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Placer Group Number");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), Integer.valueOf(38)}, "Order Status");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), Integer.valueOf(121)}, "Response Flag");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Quantity/Timing");
      this.add(EIP.class, false, 1, 0, new Object[]{getMessage()}, "Parent");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Date/Time of Transaction");
      this.add(XCN_NIH.class, false, -1, 0, new Object[]{getMessage()}, "Entered By");
      this.add(XCN_NIH.class, false, -1, 0, new Object[]{getMessage()}, "Verified By");
      this.add(XCN_NIH2.class, false, -1, 0, new Object[]{getMessage()}, "Ordering Provider");
      this.add(PL.class, false, 1, 0, new Object[]{getMessage()}, "Enterer's Location");
      this.add(XTN_NIH.class, false, 2, 0, new Object[]{getMessage()}, "Call Back Phone Number");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Order Effective Date/Time");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Order Control Code Reason");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Entering Organization");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Entering Device");
      this.add(XCN_NIH.class, false, -1, 0, new Object[]{getMessage()}, "Action By");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Advanced Beneficiary Notice Code");
      this.add(XON_ORC_21.class, false, 0, 0, new Object[]{getMessage()}, "Ordering Facility Name");
      this.add(XAD_NIH.class, false, 0, 0, new Object[]{getMessage()}, "Ordering Facility Address");
      this.add(XTN_ORC_23.class, false, 0, 0, new Object[]{getMessage()}, "Ordering Facility Phone Number");
      this.add(XAD_ORC_24.class, false, -1, 0, new Object[]{getMessage()}, "Ordering Provider Address");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Order Status Modifier");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Advanced Beneficiary Notice Override Reason");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Filler's Expected Availability Date/Time");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Confidentiality Code");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Order Type");
      this.add(CNE.class, false, 1, 0, new Object[]{getMessage()}, "Enterer Authorization Mode");
      this.add(CWE_NIH.class, false, 1, 0, new Object[]{getMessage()}, "Parent Universal Service Identifier");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating ORC - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns
   * ORC-1: "Order Control" - creates it if necessary
   */
  public ID getOrderControl() {
    ID retVal = this.getTypedField(1, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-1: "Order Control" - creates it if necessary
   */
  public ID getOrc1_OrderControl() {
    ID retVal = this.getTypedField(1, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-2: "Placer Order Number" - creates it if necessary
   */
  public NULLDT getPlacerOrderNumber() {
    NULLDT retVal = this.getTypedField(2, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-2: "Placer Order Number" - creates it if necessary
   */
  public NULLDT getOrc2_PlacerOrderNumber() {
    NULLDT retVal = this.getTypedField(2, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-3: "Filler Order Number" - creates it if necessary
   */
  public EI_NIH3 getFillerOrderNumber() {
    EI_NIH3 retVal = this.getTypedField(3, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-3: "Filler Order Number" - creates it if necessary
   */
  public EI_NIH3 getOrc3_FillerOrderNumber() {
    EI_NIH3 retVal = this.getTypedField(3, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-4: "Placer Group Number" - creates it if necessary
   */
  public EI_NIH getPlacerGroupNumber() {
    EI_NIH retVal = this.getTypedField(4, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-4: "Placer Group Number" - creates it if necessary
   */
  public EI_NIH getOrc4_PlacerGroupNumber() {
    EI_NIH retVal = this.getTypedField(4, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-5: "Order Status" - creates it if necessary
   */
  public ID getOrderStatus() {
    ID retVal = this.getTypedField(5, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-5: "Order Status" - creates it if necessary
   */
  public ID getOrc5_OrderStatus() {
    ID retVal = this.getTypedField(5, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-6: "Response Flag" - creates it if necessary
   */
  public ID getResponseFlag() {
    ID retVal = this.getTypedField(6, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-6: "Response Flag" - creates it if necessary
   */
  public ID getOrc6_ResponseFlag() {
    ID retVal = this.getTypedField(6, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Quantity/Timing (ORC-7).
   */
  public NULLDT[] getQuantityTiming() {
    NULLDT[] retVal = this.getTypedField(7, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Quantity/Timing (ORC-7).
   */
  public NULLDT[] getOrc7_QuantityTiming() {
    NULLDT[] retVal = this.getTypedField(7, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Quantity/Timing (ORC-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getQuantityTimingReps() {
    return this.getReps(7);
  }


  /**
   * Returns a specific repetition of
   * ORC-7: "Quantity/Timing" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getQuantityTiming(int rep) {
    NULLDT retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-7: "Quantity/Timing" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getOrc7_QuantityTiming(int rep) {
    NULLDT retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Quantity/Timing (ORC-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc7_QuantityTimingReps() {
    return this.getReps(7);
  }


  /**
   * Inserts a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertQuantityTiming(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(7, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertOrc7_QuantityTiming(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeQuantityTiming(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeOrc7_QuantityTiming(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(7, rep);
  }


  /**
   * Returns
   * ORC-8: "Parent" - creates it if necessary
   */
  public EIP getORCParent() {
    EIP retVal = this.getTypedField(8, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-8: "Parent" - creates it if necessary
   */
  public EIP getOrc8_Parent() {
    EIP retVal = this.getTypedField(8, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-9: "Date/Time of Transaction" - creates it if necessary
   */
  public TS getDateTimeOfTransaction() {
    TS retVal = this.getTypedField(9, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-9: "Date/Time of Transaction" - creates it if necessary
   */
  public TS getOrc9_DateTimeOfTransaction() {
    TS retVal = this.getTypedField(9, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Entered By (ORC-10).
   */
  public XCN_NIH[] getEnteredBy() {
    XCN_NIH[] retVal = this.getTypedField(10, new XCN_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Entered By (ORC-10).
   */
  public XCN_NIH[] getOrc10_EnteredBy() {
    XCN_NIH[] retVal = this.getTypedField(10, new XCN_NIH[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Entered By (ORC-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEnteredByReps() {
    return this.getReps(10);
  }


  /**
   * Returns a specific repetition of
   * ORC-10: "Entered By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH getEnteredBy(int rep) {
    XCN_NIH retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-10: "Entered By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH getOrc10_EnteredBy(int rep) {
    XCN_NIH retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Entered By (ORC-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc10_EnteredByReps() {
    return this.getReps(10);
  }


  /**
   * Inserts a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH insertEnteredBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.insertRepetition(10, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH insertOrc10_EnteredBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.insertRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH removeEnteredBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.removeRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH removeOrc10_EnteredBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.removeRepetition(10, rep);
  }


  /**
   * Returns all repetitions of Verified By (ORC-11).
   */
  public XCN_NIH[] getVerifiedBy() {
    XCN_NIH[] retVal = this.getTypedField(11, new XCN_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Verified By (ORC-11).
   */
  public XCN_NIH[] getOrc11_VerifiedBy() {
    XCN_NIH[] retVal = this.getTypedField(11, new XCN_NIH[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Verified By (ORC-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVerifiedByReps() {
    return this.getReps(11);
  }


  /**
   * Returns a specific repetition of
   * ORC-11: "Verified By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH getVerifiedBy(int rep) {
    XCN_NIH retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-11: "Verified By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH getOrc11_VerifiedBy(int rep) {
    XCN_NIH retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Verified By (ORC-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc11_VerifiedByReps() {
    return this.getReps(11);
  }


  /**
   * Inserts a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH insertVerifiedBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.insertRepetition(11, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH insertOrc11_VerifiedBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.insertRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH removeVerifiedBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.removeRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH removeOrc11_VerifiedBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.removeRepetition(11, rep);
  }


  /**
   * Returns all repetitions of Ordering Provider (ORC-12).
   */
  public XCN_NIH2[] getOrderingProvider() {
    XCN_NIH2[] retVal = this.getTypedField(12, new XCN_NIH2[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Provider (ORC-12).
   */
  public XCN_NIH2[] getOrc12_OrderingProvider() {
    XCN_NIH2[] retVal = this.getTypedField(12, new XCN_NIH2[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Provider (ORC-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingProviderReps() {
    return this.getReps(12);
  }


  /**
   * Returns a specific repetition of
   * ORC-12: "Ordering Provider" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH2 getOrderingProvider(int rep) {
    XCN_NIH2 retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-12: "Ordering Provider" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH2 getOrc12_OrderingProvider(int rep) {
    XCN_NIH2 retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Provider (ORC-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc12_OrderingProviderReps() {
    return this.getReps(12);
  }


  /**
   * Inserts a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH2 insertOrderingProvider(int rep) throws HL7Exception {
    return (XCN_NIH2) super.insertRepetition(12, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH2 insertOrc12_OrderingProvider(int rep) throws HL7Exception {
    return (XCN_NIH2) super.insertRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH2 removeOrderingProvider(int rep) throws HL7Exception {
    return (XCN_NIH2) super.removeRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH2 removeOrc12_OrderingProvider(int rep) throws HL7Exception {
    return (XCN_NIH2) super.removeRepetition(12, rep);
  }


  /**
   * Returns
   * ORC-13: "Enterer's Location" - creates it if necessary
   */
  public PL getEntererSLocation() {
    PL retVal = this.getTypedField(13, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-13: "Enterer's Location" - creates it if necessary
   */
  public PL getOrc13_EntererSLocation() {
    PL retVal = this.getTypedField(13, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Call Back Phone Number (ORC-14).
   */
  public XTN_NIH[] getCallBackPhoneNumber() {
    XTN_NIH[] retVal = this.getTypedField(14, new XTN_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Call Back Phone Number (ORC-14).
   */
  public XTN[] getOrc14_CallBackPhoneNumber() {
    XTN[] retVal = this.getTypedField(14, new XTN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Call Back Phone Number (ORC-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCallBackPhoneNumberReps() {
    return this.getReps(14);
  }


  /**
   * Returns a specific repetition of
   * ORC-14: "Call Back Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_NIH getCallBackPhoneNumber(int rep) {
    XTN_NIH retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-14: "Call Back Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_NIH getOrc14_CallBackPhoneNumber(int rep) {
    XTN_NIH retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Call Back Phone Number (ORC-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc14_CallBackPhoneNumberReps() {
    return this.getReps(14);
  }


  /**
   * Inserts a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_NIH insertCallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_NIH) super.insertRepetition(14, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_NIH insertOrc14_CallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_NIH) super.insertRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_NIH removeCallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_NIH) super.removeRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_NIH removeOrc14_CallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_NIH) super.removeRepetition(14, rep);
  }


  /**
   * Returns
   * ORC-15: "Order Effective Date/Time" - creates it if necessary
   */
  public TS getOrderEffectiveDateTime() {
    TS retVal = this.getTypedField(15, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-15: "Order Effective Date/Time" - creates it if necessary
   */
  public TS getOrc15_OrderEffectiveDateTime() {
    TS retVal = this.getTypedField(15, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-16: "Order Control Code Reason" - creates it if necessary
   */
  public CWE_NIH getOrderControlCodeReason() {
    CWE_NIH retVal = this.getTypedField(16, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-16: "Order Control Code Reason" - creates it if necessary
   */
  public CWE_NIH getOrc16_OrderControlCodeReason() {
    CWE_NIH retVal = this.getTypedField(16, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-17: "Entering Organization" - creates it if necessary
   */
  public CWE_NIH getEnteringOrganization() {
    CWE_NIH retVal = this.getTypedField(17, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-17: "Entering Organization" - creates it if necessary
   */
  public CWE_NIH getOrc17_EnteringOrganization() {
    CWE_NIH retVal = this.getTypedField(17, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-18: "Entering Device" - creates it if necessary
   */
  public CWE_NIH getEnteringDevice() {
    CWE_NIH retVal = this.getTypedField(18, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-18: "Entering Device" - creates it if necessary
   */
  public CWE_NIH getOrc18_EnteringDevice() {
    CWE_NIH retVal = this.getTypedField(18, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Action By (ORC-19).
   */
  public XCN_NIH[] getActionBy() {
    XCN_NIH[] retVal = this.getTypedField(19, new XCN_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Action By (ORC-19).
   */
  public XCN_NIH[] getOrc19_ActionBy() {
    XCN_NIH[] retVal = this.getTypedField(19, new XCN_NIH[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Action By (ORC-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getActionByReps() {
    return this.getReps(19);
  }


  /**
   * Returns a specific repetition of
   * ORC-19: "Action By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH getActionBy(int rep) {
    XCN_NIH retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-19: "Action By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_NIH getOrc19_ActionBy(int rep) {
    XCN_NIH retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Action By (ORC-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc19_ActionByReps() {
    return this.getReps(19);
  }


  /**
   * Inserts a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH insertActionBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.insertRepetition(19, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH insertOrc19_ActionBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.insertRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH removeActionBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.removeRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_NIH removeOrc19_ActionBy(int rep) throws HL7Exception {
    return (XCN_NIH) super.removeRepetition(19, rep);
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Code (ORC-20).
   */
  public NULLDT[] getAdvancedBeneficiaryNoticeCode() {
    NULLDT[] retVal = this.getTypedField(20, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Code (ORC-20).
   */
  public NULLDT[] getOrc20_AdvancedBeneficiaryNoticeCode() {
    NULLDT[] retVal = this.getTypedField(20, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Code (ORC-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAdvancedBeneficiaryNoticeCodeReps() {
    return this.getReps(20);
  }


  /**
   * Returns a specific repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getAdvancedBeneficiaryNoticeCode(int rep) {
    NULLDT retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getOrc20_AdvancedBeneficiaryNoticeCode(int rep) {
    NULLDT retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Code (ORC-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc20_AdvancedBeneficiaryNoticeCodeReps() {
    return this.getReps(20);
  }


  /**
   * Inserts a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertAdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(20, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertOrc20_AdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeAdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeOrc20_AdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(20, rep);
  }


  /**
   * Returns all repetitions of Ordering Facility Name (ORC-21).
   */
  public XON_ORC_21[] getOrderingFacilityName() {
    XON_ORC_21[] retVal = this.getTypedField(21, new XON_ORC_21[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Facility Name (ORC-21).
   */
  public XON_ORC_21[] getOrc21_OrderingFacilityName() {
    XON_ORC_21[] retVal = this.getTypedField(21, new XON_ORC_21[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Facility Name (ORC-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingFacilityNameReps() {
    return this.getReps(21);
  }


  /**
   * Returns a specific repetition of
   * ORC-21: "Ordering Facility Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XON_ORC_21 getOrderingFacilityName(int rep) {
    XON_ORC_21 retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-21: "Ordering Facility Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XON_ORC_21 getOrc21_OrderingFacilityName(int rep) {
    XON_ORC_21 retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Facility Name (ORC-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc21_OrderingFacilityNameReps() {
    return this.getReps(21);
  }


  /**
   * Inserts a repetition of
   * ORC-21: "Ordering Facility Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON_ORC_21 insertOrderingFacilityName(int rep) throws HL7Exception {
    return (XON_ORC_21) super.insertRepetition(21, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-21: "Ordering Facility Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON_ORC_21 insertOrc21_OrderingFacilityName(int rep) throws HL7Exception {
    return (XON_ORC_21) super.insertRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * ORC-21: "Ordering Facility Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON_ORC_21 removeOrderingFacilityName(int rep) throws HL7Exception {
    return (XON_ORC_21) super.removeRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * ORC-21: "Ordering Facility Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON_ORC_21 removeOrc21_OrderingFacilityName(int rep) throws HL7Exception {
    return (XON_ORC_21) super.removeRepetition(21, rep);
  }


  /**
   * Returns all repetitions of Ordering Facility Address (ORC-22).
   */
  public XAD_NIH[] getOrderingFacilityAddress() {
    XAD_NIH[] retVal = this.getTypedField(22, new XAD_NIH[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Facility Address (ORC-22).
   */
  public XAD_NIH[] getOrc22_OrderingFacilityAddress() {
    XAD_NIH[] retVal = this.getTypedField(22, new XAD_NIH[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Facility Address (ORC-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingFacilityAddressReps() {
    return this.getReps(22);
  }


  /**
   * Returns a specific repetition of
   * ORC-22: "Ordering Facility Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD_NIH getOrderingFacilityAddress(int rep) {
    XAD_NIH retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-22: "Ordering Facility Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD_NIH getOrc22_OrderingFacilityAddress(int rep) {
    XAD_NIH retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Facility Address (ORC-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc22_OrderingFacilityAddressReps() {
    return this.getReps(22);
  }


  /**
   * Inserts a repetition of
   * ORC-22: "Ordering Facility Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_NIH insertOrderingFacilityAddress(int rep) throws HL7Exception {
    return (XAD_NIH) super.insertRepetition(22, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-22: "Ordering Facility Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_NIH insertOrc22_OrderingFacilityAddress(int rep) throws HL7Exception {
    return (XAD_NIH) super.insertRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * ORC-22: "Ordering Facility Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_NIH removeOrderingFacilityAddress(int rep) throws HL7Exception {
    return (XAD_NIH) super.removeRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * ORC-22: "Ordering Facility Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_NIH removeOrc22_OrderingFacilityAddress(int rep) throws HL7Exception {
    return (XAD_NIH) super.removeRepetition(22, rep);
  }


  /**
   * Returns all repetitions of Ordering Facility Phone Number (ORC-23).
   */
  public XTN_ORC_23[] getOrderingFacilityPhoneNumber() {
    XTN_ORC_23[] retVal = this.getTypedField(23, new XTN_ORC_23[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Facility Phone Number (ORC-23).
   */
  public XTN_ORC_23[] getOrc23_OrderingFacilityPhoneNumber() {
    XTN_ORC_23[] retVal = this.getTypedField(23, new XTN_ORC_23[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Facility Phone Number (ORC-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingFacilityPhoneNumberReps() {
    return this.getReps(23);
  }


  /**
   * Returns a specific repetition of
   * ORC-23: "Ordering Facility Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_ORC_23 getOrderingFacilityPhoneNumber(int rep) {
    XTN_ORC_23 retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-23: "Ordering Facility Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_ORC_23 getOrc23_OrderingFacilityPhoneNumber(int rep) {
    XTN_ORC_23 retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Facility Phone Number (ORC-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc23_OrderingFacilityPhoneNumberReps() {
    return this.getReps(23);
  }


  /**
   * Inserts a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ORC_23 insertOrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ORC_23) super.insertRepetition(23, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ORC_23 insertOrc23_OrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ORC_23) super.insertRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ORC_23 removeOrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ORC_23) super.removeRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ORC_23 removeOrc23_OrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ORC_23) super.removeRepetition(23, rep);
  }


  /**
   * Returns all repetitions of Ordering Provider Address (ORC-24).
   */
  public XAD_ORC_24[] getOrderingProviderAddress() {
    XAD_ORC_24[] retVal = this.getTypedField(24, new XAD_ORC_24[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Provider Address (ORC-24).
   */
  public XAD_ORC_24[] getOrc24_OrderingProviderAddress() {
    XAD_ORC_24[] retVal = this.getTypedField(24, new XAD_ORC_24[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Provider Address (ORC-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingProviderAddressReps() {
    return this.getReps(24);
  }


  /**
   * Returns a specific repetition of
   * ORC-24: "Ordering Provider Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD_ORC_24 getOrderingProviderAddress(int rep) {
    XAD_ORC_24 retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-24: "Ordering Provider Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD_ORC_24 getOrc24_OrderingProviderAddress(int rep) {
    XAD_ORC_24 retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Provider Address (ORC-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc24_OrderingProviderAddressReps() {
    return this.getReps(24);
  }


  /**
   * Inserts a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ORC_24 insertOrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ORC_24) super.insertRepetition(24, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ORC_24 insertOrc24_OrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ORC_24) super.insertRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ORC_24 removeOrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ORC_24) super.removeRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ORC_24 removeOrc24_OrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ORC_24) super.removeRepetition(24, rep);
  }


  /**
   * Returns
   * ORC-25: "Order Status Modifier" - creates it if necessary
   */
  public CWE_NIH getOrderStatusModifier() {
    CWE_NIH retVal = this.getTypedField(25, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-25: "Order Status Modifier" - creates it if necessary
   */
  public CWE_NIH getOrc25_OrderStatusModifier() {
    CWE_NIH retVal = this.getTypedField(25, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   */
  public NULLDT[] getAdvancedBeneficiaryNoticeOverrideReason() {
    NULLDT[] retVal = this.getTypedField(26, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   */
  public NULLDT[] getOrc26_AdvancedBeneficiaryNoticeOverrideReason() {
    NULLDT[] retVal = this.getTypedField(26, new NULLDT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAdvancedBeneficiaryNoticeOverrideReasonReps() {
    return this.getReps(26);
  }


  /**
   * Returns a specific repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getAdvancedBeneficiaryNoticeOverrideReason(int rep) {
    NULLDT retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NULLDT getOrc26_AdvancedBeneficiaryNoticeOverrideReason(int rep) {
    NULLDT retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc26_AdvancedBeneficiaryNoticeOverrideReasonReps() {
    return this.getReps(26);
  }


  /**
   * Inserts a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertAdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(26, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT insertOrc26_AdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (NULLDT) super.insertRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeAdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NULLDT removeOrc26_AdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (NULLDT) super.removeRepetition(26, rep);
  }


  /**
   * Returns
   * ORC-27: "Filler's Expected Availability Date/Time" - creates it if necessary
   */
  public TS getFillerSExpectedAvailabilityDateTime() {
    TS retVal = this.getTypedField(27, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-27: "Filler's Expected Availability Date/Time" - creates it if necessary
   */
  public TS getOrc27_FillerSExpectedAvailabilityDateTime() {
    TS retVal = this.getTypedField(27, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-28: "Confidentiality Code" - creates it if necessary
   */
  public CWE_NIH getConfidentialityCode() {
    CWE_NIH retVal = this.getTypedField(28, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-28: "Confidentiality Code" - creates it if necessary
   */
  public CWE_NIH getOrc28_ConfidentialityCode() {
    CWE_NIH retVal = this.getTypedField(28, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-29: "Order Type" - creates it if necessary
   */
  public CWE_NIH getOrderType() {
    CWE_NIH retVal = this.getTypedField(29, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-29: "Order Type" - creates it if necessary
   */
  public CWE_NIH getOrc29_OrderType() {
    CWE_NIH retVal = this.getTypedField(29, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-30: "Enterer Authorization Mode" - creates it if necessary
   */
  public CNE getEntererAuthorizationMode() {
    CNE retVal = this.getTypedField(30, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-30: "Enterer Authorization Mode" - creates it if necessary
   */
  public CNE getOrc30_EntererAuthorizationMode() {
    CNE retVal = this.getTypedField(30, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-31: "Parent Universal Service Identifier" - creates it if necessary
   */
  public CWE_NIH getParentUniversalServiceIdentifier() {
    CWE_NIH retVal = this.getTypedField(31, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-31: "Parent Universal Service Identifier" - creates it if necessary
   */
  public CWE_NIH getOrc31_ParentUniversalServiceIdentifier() {
    CWE_NIH retVal = this.getTypedField(31, 0);
    return retVal;
  }


  /**
   * {@inheritDoc}
   */
  protected Type createNewTypeWithoutReflection(int field) {
    switch (field) {
      case 0:
        return new ID(getMessage(), 119);
      case 1:
        return new NULLDT(getMessage());
      case 2:
        return new EI_NIH3(getMessage());
      case 3:
        return new EI_NIH(getMessage());
      case 4:
        return new ID(getMessage(), 38);
      case 5:
        return new ID(getMessage(), 121);
      case 6:
        return new NULLDT(getMessage());
      case 7:
        return new EIP(getMessage());
      case 8:
        return new TS(getMessage());
      case 9:
        return new XCN_NIH(getMessage());
      case 10:
        return new XCN_NIH(getMessage());
      case 11:
        return new XCN_NIH2(getMessage());
      case 12:
        return new PL(getMessage());
      case 13:
        return new XTN_NIH(getMessage());
      case 14:
        return new TS(getMessage());
      case 15:
        return new CWE_NIH(getMessage());
      case 16:
        return new CWE_NIH(getMessage());
      case 17:
        return new CWE_NIH(getMessage());
      case 18:
        return new XCN_NIH(getMessage());
      case 19:
        return new NULLDT(getMessage());
      case 20:
        return new XON_ORC_21(getMessage());
      case 21:
        return new XAD_NIH(getMessage());
      case 22:
        return new XTN_ORC_23(getMessage());
      case 23:
        return new XAD_ORC_24(getMessage());
      case 24:
        return new CWE_NIH(getMessage());
      case 25:
        return new NULLDT(getMessage());
      case 26:
        return new TS(getMessage());
      case 27:
        return new CWE_NIH(getMessage());
      case 28:
        return new CWE_NIH(getMessage());
      case 29:
        return new CNE(getMessage());
      case 30:
        return new CWE_NIH(getMessage());
      default:
        return null;
    }
  }


}

