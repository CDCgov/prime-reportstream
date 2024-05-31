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


package fhirengine.translation.hl7.structures.fhirinventory.group;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.fhirinventory.segment.NTE;
import fhirengine.translation.hl7.structures.fhirinventory.segment.OBX;
import fhirengine.translation.hl7.structures.fhirinventory.segment.PRT;


/**
 * <p>Represents a ORU_R01_OBSERVATION group structure (a Group object).
 * A Group is an ordered collection of message segments that can repeat together or be optionally in/excluded together.
 * This Group contains the following elements:
 * </p>
 * <ul>
 * <li>1: OBX (Observation/Result) <b>  </b></li>
 * <li>2: PRT (Participation Information) <b>optional repeating </b></li>
 * <li>3: NTE (Notes and Comments) <b>optional repeating </b></li>
 * </ul>
 */
//@SuppressWarnings("unused")
public class ORU_R01_OBSERVATION extends AbstractGroup {

  /**
   * Creates a new ORU_R01_OBSERVATION group
   */
  public ORU_R01_OBSERVATION(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(OBX.class, true, false, false);
      this.add(PRT.class, false, true, false);
      this.add(NTE.class, false, true, false);
    } catch (HL7Exception e) {
      log.error("Unexpected error creating ORU_R01_OBSERVATION - this is probably a bug in the source code generator.", e);
    }
  }

  /**
   * Returns "2.7"
   */
  public String getVersion() {
    return "2.7";
  }


  /**
   * Returns
   * OBX (Observation/Result) - creates it if necessary
   */
  public OBX getOBX() {
    OBX retVal = getTyped("OBX", OBX.class);
    return retVal;
  }


  /**
   * Returns
   * the first repetition of
   * PRT (Participation Information) - creates it if necessary
   */
  public PRT getPRT() {
    PRT retVal = getTyped("PRT", PRT.class);
    return retVal;
  }


  /**
   * Returns a specific repetition of
   * PRT (Participation Information) - creates it if necessary
   *
   * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
   * @throws HL7Exception if the repetition requested is more than one
   *                      greater than the number of existing repetitions.
   */
  public PRT getPRT(int rep) {
    PRT retVal = getTyped("PRT", rep, PRT.class);
    return retVal;
  }

  /**
   * Returns the number of existing repetitions of PRT
   */
  public int getPRTReps() {
    return getReps("PRT");
  }

  /**
   * <p>
   * Returns a non-modifiable List containing all current existing repetitions of PRT.
   * <p>
   * <p>
   * Note that unlike {@link #getPRT()}, this method will not create any reps
   * if none are already present, so an empty list may be returned.
   * </p>
   */
  public java.util.List<PRT> getPRTAll() throws HL7Exception {
    return getAllAsList("PRT", PRT.class);
  }

  /**
   * Inserts a specific repetition of PRT (Participation Information)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public void insertPRT(PRT structure, int rep) throws HL7Exception {
    super.insertRepetition("PRT", structure, rep);
  }


  /**
   * Inserts a specific repetition of PRT (Participation Information)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public PRT insertPRT(int rep) throws HL7Exception {
    return (PRT) super.insertRepetition("PRT", rep);
  }


  /**
   * Removes a specific repetition of PRT (Participation Information)
   *
   * @see AbstractGroup#removeRepetition(String, int)
   */
  public PRT removePRT(int rep) throws HL7Exception {
    return (PRT) super.removeRepetition("PRT", rep);
  }


  /**
   * Returns
   * the first repetition of
   * NTE (Notes and Comments) - creates it if necessary
   */
  public NTE getNTE() {
    NTE retVal = getTyped("NTE", NTE.class);
    return retVal;
  }


  /**
   * Returns a specific repetition of
   * NTE (Notes and Comments) - creates it if necessary
   *
   * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
   * @throws HL7Exception if the repetition requested is more than one
   *                      greater than the number of existing repetitions.
   */
  public NTE getNTE(int rep) {
    NTE retVal = getTyped("NTE", rep, NTE.class);
    return retVal;
  }

  /**
   * Returns the number of existing repetitions of NTE
   */
  public int getNTEReps() {
    return getReps("NTE");
  }

  /**
   * <p>
   * Returns a non-modifiable List containing all current existing repetitions of NTE.
   * <p>
   * <p>
   * Note that unlike {@link #getNTE()}, this method will not create any reps
   * if none are already present, so an empty list may be returned.
   * </p>
   */
  public java.util.List<NTE> getNTEAll() throws HL7Exception {
    return getAllAsList("NTE", NTE.class);
  }

  /**
   * Inserts a specific repetition of NTE (Notes and Comments)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public void insertNTE(NTE structure, int rep) throws HL7Exception {
    super.insertRepetition("NTE", structure, rep);
  }


  /**
   * Inserts a specific repetition of NTE (Notes and Comments)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public NTE insertNTE(int rep) throws HL7Exception {
    return (NTE) super.insertRepetition("NTE", rep);
  }


  /**
   * Removes a specific repetition of NTE (Notes and Comments)
   *
   * @see AbstractGroup#removeRepetition(String, int)
   */
  public NTE removeNTE(int rep) throws HL7Exception {
    return (NTE) super.removeRepetition("NTE", rep);
  }


}

