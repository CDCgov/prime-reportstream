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

package fhirengine.translation.hl7.structures.fhirinventory.message;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.v27.segment.SFT;
import ca.uhn.hl7v2.model.v27.segment.UAC;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.fhirinventory.group.OML_O21_ORDER;
import fhirengine.translation.hl7.structures.fhirinventory.group.OML_O21_PATIENT;
import fhirengine.translation.hl7.structures.fhirinventory.segment.NTE;

/**
 * <p>Represents a OML_O21 message structure (see chapter 4.4.6). This structure contains the
 * following elements: </p>
 * <ul>
 * <li>1: MSH (Message Header) <b> </b> </li>
 * <li>2: SFT (Software Segment) <b>optional repeating</b> </li>
 * <li>3: UAC (User Authentication Credential Segment) <b>optional </b> </li>
 * <li>4: NTE (Notes and Comments) <b>optional repeating</b> </li>
 * <li>5: OML_O21_PATIENT (a Group object) <b>optional </b> </li>
 * <li>6: OML_O21_ORDER (a Group object) <b> repeating</b> </li>
 * </ul>
 */
//@SuppressWarnings("unused")
public class OML_O21 extends AbstractMessage  {

  /**
   * Creates a new OML_O21 message with DefaultModelClassFactory.
   */
  public OML_O21() {
    this(new DefaultModelClassFactory());
  }

  /**
   * Creates a new OML_O21 message with custom ModelClassFactory.
   */
  public OML_O21(ModelClassFactory factory) {
    super(factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(MSH.class, true, false);
      this.add(SFT.class, false, true);
      this.add(UAC.class, false, false);
      this.add(NTE.class, false, true);
      this.add(OML_O21_PATIENT.class, false, false);
      this.add(OML_O21_ORDER.class, true, true);
    } catch(HL7Exception e) {
      log.error("Unexpected error creating OML_O21 - this is probably a bug in the source code generator.", e);
    }
  }

  /**
   * Returns "2.7"
   */
  public String getVersion() { return "2.7"; }

  /**
   * <p>
   * Returns
   * MSH (Message Header) - creates it if necessary
   * </p>
   */
  public MSH getMSH() { return getTyped("MSH", MSH.class); }

  /**
   * <p>
   * Returns
   * the first repetition of
   * SFT (Software Segment) - creates it if necessary
   * </p>
   */
  public SFT getSFT() { return getTyped("SFT", SFT.class); }


  /**
   * <p>
   * Returns a specific repetition of
   * SFT (Software Segment) - creates it if necessary
   * </p>
   *
   * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
   * @throws HL7Exception if the repetition requested is more than one
   *     greater than the number of existing repetitions.
   */
  public SFT getSFT(int rep) { return getTyped("SFT", rep, SFT.class); }

  /**
   * <p>
   * Returns the number of existing repetitions of SFT
   * </p>
   */
  public int getSFTReps() { return getReps("SFT"); }

  /**
   * <p>
   * Returns a non-modifiable List containing all current existing repetitions of SFT.
   * <p>
   * Note that unlike {@link #getSFT()}, this method will not create any reps
   * if none are already present, so an empty list may be returned.
   * </p>
   */
  public java.util.List<SFT> getSFTAll() throws HL7Exception {
    return getAllAsList("SFT", SFT.class);
  }

  /**
   * <p>
   * Inserts a specific repetition of SFT (Software Segment)
   * </p>
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public void insertSFT(SFT structure, int rep) throws HL7Exception {
    super.insertRepetition( "SFT", structure, rep);
  }

  /**
   * <p>
   * Inserts a specific repetition of SFT (Software Segment)
   * </p>
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public SFT insertSFT(int rep) throws HL7Exception {
    return (SFT)super.insertRepetition("SFT", rep);
  }

  /**
   * <p>
   * Removes a specific repetition of SFT (Software Segment)
   * </p>
   *
   * @see AbstractGroup#removeRepetition(String, int)
   */
  public SFT removeSFT(int rep) throws HL7Exception {
    return (SFT)super.removeRepetition("SFT", rep);
  }

  /**
   * <p>
   * Returns
   * UAC (User Authentication Credential Segment) - creates it if necessary
   * </p>
   */
  public UAC getUAC() {
    return getTyped("UAC", UAC.class);
  }

  /**
   * <p>
   * Returns
   * the first repetition of
   * NTE (Notes and Comments) - creates it if necessary
   * </p>
   */
  public NTE getNTE() {
    return getTyped("NTE", NTE.class);
  }

  /**
   * <p>
   * Returns a specific repetition of
   * NTE (Notes and Comments) - creates it if necessary
   * </p>
   *
   * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
   * @throws HL7Exception if the repetition requested is more than one
   *     greater than the number of existing repetitions.
   */
  public NTE getNTE(int rep) {
    return getTyped("NTE", rep, NTE.class);
  }

  /**
   * <p>
   * Returns the number of existing repetitions of NTE
   * </p>
   */
  public int getNTEReps() {
    return getReps("NTE");
  }

  /**
   * <p>
   * Returns a non-modifiable List containing all current existing repetitions of NTE.
   * <p>
   * Note that unlike {@link #getNTE()}, this method will not create any reps
   * if none are already present, so an empty list may be returned.
   * </p>
   */
  public java.util.List<NTE> getNTEAll() throws HL7Exception {
    return getAllAsList("NTE", NTE.class);
  }

  /**
   * <p>
   * Inserts a specific repetition of NTE (Notes and Comments)
   * </p>
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public void insertNTE(NTE structure, int rep) throws HL7Exception {
    super.insertRepetition( "NTE", structure, rep);
  }

  /**
   * <p>
   * Inserts a specific repetition of NTE (Notes and Comments)
   * </p>
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public NTE insertNTE(int rep) throws HL7Exception {
    return (NTE)super.insertRepetition("NTE", rep);
  }

  /**
   * <p>
   * Removes a specific repetition of NTE (Notes and Comments)
   * </p>
   *
   * @see AbstractGroup#removeRepetition(String, int)
   */
  public NTE removeNTE(int rep) throws HL7Exception {
    return (NTE)super.removeRepetition("NTE", rep);
  }

  /**
   * <p>
   * Returns
   * PATIENT (a Group object) - creates it if necessary
   * </p>
   */
  public OML_O21_PATIENT getPATIENT() {
    return getTyped("PATIENT", OML_O21_PATIENT.class);
  }

  /**
   * <p>
   * Returns
   * the first repetition of
   * ORDER (a Group object) - creates it if necessary
   * </p>
   */
  public OML_O21_ORDER getORDER() {
    return getTyped("ORDER", OML_O21_ORDER.class);
  }

  /**
   * <p>
   * Returns a specific repetition of
   * ORDER (a Group object) - creates it if necessary
   * </p>
   *
   * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
   * @throws HL7Exception if the repetition requested is more than one
   *     greater than the number of existing repetitions.
   */
  public OML_O21_ORDER getORDER(int rep) {
    return getTyped("ORDER", rep, OML_O21_ORDER.class);
  }

  /**
   * <p>
   * Returns the number of existing repetitions of ORDER
   * </p>
   */
  public int getORDERReps() {
    return getReps("ORDER");
  }

  /**
   * <p>
   * Returns a non-modifiable List containing all current existing repetitions of ORDER.
   * <p>
   * Note that unlike {@link #getORDER()}, this method will not create any reps
   * if none are already present, so an empty list may be returned.
   * </p>
   */
  public java.util.List<OML_O21_ORDER> getORDERAll() throws HL7Exception {
    return getAllAsList("ORDER", OML_O21_ORDER.class);
  }

  /**
   * <p>
   * Inserts a specific repetition of ORDER (a Group object)
   * </p>
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public void insertORDER(OML_O21_ORDER structure, int rep) throws HL7Exception {
    super.insertRepetition( "ORDER", structure, rep);
  }

  /**
   * <p>
   * Inserts a specific repetition of ORDER (a Group object)
   * </p>
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public OML_O21_ORDER insertORDER(int rep) throws HL7Exception {
    return (OML_O21_ORDER)super.insertRepetition("ORDER", rep);
  }

  /**
   * <p>
   * Removes a specific repetition of ORDER (a Group object)
   * </p>
   *
   * @see AbstractGroup#removeRepetition(String, int)
   */
  public OML_O21_ORDER removeORDER(int rep) throws HL7Exception {
    return (OML_O21_ORDER)super.removeRepetition("ORDER", rep);
  }
}