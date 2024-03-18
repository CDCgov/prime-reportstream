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


package fhirengine.translation.hl7.structures.nistelr251.group;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.nistelr251.segment.NK1;
import fhirengine.translation.hl7.structures.nistelr251.segment.NTE;
import fhirengine.translation.hl7.structures.nistelr251.segment.PD1;
import fhirengine.translation.hl7.structures.nistelr251.segment.PID;

/**
 * <p>Represents a ORU_R01_PATIENT group structure (PATIENT_RESULT.PATIENT).
 * A Group is an ordered collection of message segments that can repeat together or be optionally in/excluded together.
 * This Group contains the following elements:
 * </p>
 * <ul>
 * <li>1: PID (Patient Identification) <b>  </b></li>
 * <li>2: PD1 (Additional Demographic) <b>optional  </b></li>
 * <li>3: NTE (Notes and Comments for PID) <b>optional repeating </b></li>
 * <li>4: NK1 (Next of Kin / Associated Parties) <b>optional repeating </b></li>
 * <li>5: ORU_R01_VISIT (PATIENT_RESULT.PATIENT.VISIT) <b>optional  </b></li>
 * </ul>
 */
//@SuppressWarnings("unused")
public class ORU_R01_PATIENT extends AbstractGroup {

  /**
   * Creates a new ORU_R01_PATIENT group
   */
  public ORU_R01_PATIENT(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(PID.class, true, false, false);
      this.add(PD1.class, false, false, false);
      this.add(NTE.class, false, true, false);
      this.add(NK1.class, false, true, false);
      this.add(ORU_R01_VISIT.class, false, false, false);
    } catch (HL7Exception e) {
      log.error("Unexpected error creating ORU_R01_PATIENT - this is probably a bug in the source code generator.", e);
    }
  }

  /**
   * Returns "2.5.1"
   */
  public String getVersion() {
    return "2.5.1";
  }


  /**
   * Returns
   * PID (Patient Identification) - creates it if necessary
   */
  public PID getPID() {
    PID retVal = getTyped("PID", PID.class);
    return retVal;
  }


  /**
   * Returns
   * PD1 (Additional Demographic) - creates it if necessary
   */
  public PD1 getPD1() {
    PD1 retVal = getTyped("PD1", PD1.class);
    return retVal;
  }


  /**
   * Returns
   * the first repetition of
   * NTE (Notes and Comments for PID) - creates it if necessary
   */
  public NTE getNTE() {
    NTE retVal = getTyped("NTE", NTE.class);
    return retVal;
  }


  /**
   * Returns a specific repetition of
   * NTE (Notes and Comments for PID) - creates it if necessary
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
   * Inserts a specific repetition of NTE (Notes and Comments for PID)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public void insertNTE(NTE structure, int rep) throws HL7Exception {
    super.insertRepetition("NTE", structure, rep);
  }


  /**
   * Inserts a specific repetition of NTE (Notes and Comments for PID)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public NTE insertNTE(int rep) throws HL7Exception {
    return (NTE) super.insertRepetition("NTE", rep);
  }


  /**
   * Removes a specific repetition of NTE (Notes and Comments for PID)
   *
   * @see AbstractGroup#removeRepetition(String, int)
   */
  public NTE removeNTE(int rep) throws HL7Exception {
    return (NTE) super.removeRepetition("NTE", rep);
  }


  /**
   * Returns
   * the first repetition of
   * NK1 (Next of Kin / Associated Parties) - creates it if necessary
   */
  public NK1 getNK1() {
    NK1 retVal = getTyped("NK1", NK1.class);
    return retVal;
  }


  /**
   * Returns a specific repetition of
   * NK1 (Next of Kin / Associated Parties) - creates it if necessary
   *
   * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
   * @throws HL7Exception if the repetition requested is more than one
   *                      greater than the number of existing repetitions.
   */
  public NK1 getNK1(int rep) {
    NK1 retVal = getTyped("NK1", rep, NK1.class);
    return retVal;
  }

  /**
   * Returns the number of existing repetitions of NK1
   */
  public int getNK1Reps() {
    return getReps("NK1");
  }

  /**
   * <p>
   * Returns a non-modifiable List containing all current existing repetitions of NK1.
   * <p>
   * <p>
   * Note that unlike {@link #getNK1()}, this method will not create any reps
   * if none are already present, so an empty list may be returned.
   * </p>
   */
  public java.util.List<NK1> getNK1All() throws HL7Exception {
    return getAllAsList("NK1", NK1.class);
  }

  /**
   * Inserts a specific repetition of NK1 (Next of Kin / Associated Parties)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public void insertNK1(NK1 structure, int rep) throws HL7Exception {
    super.insertRepetition("NK1", structure, rep);
  }


  /**
   * Inserts a specific repetition of NK1 (Next of Kin / Associated Parties)
   *
   * @see AbstractGroup#insertRepetition(Structure, int)
   */
  public NK1 insertNK1(int rep) throws HL7Exception {
    return (NK1) super.insertRepetition("NK1", rep);
  }


  /**
   * Removes a specific repetition of NK1 (Next of Kin / Associated Parties)
   *
   * @see AbstractGroup#removeRepetition(String, int)
   */
  public NK1 removeNK1(int rep) throws HL7Exception {
    return (NK1) super.removeRepetition("NK1", rep);
  }


  /**
   * Returns
   * VISIT (PATIENT_RESULT.PATIENT.VISIT) - creates it if necessary
   */
  public ORU_R01_VISIT getVISIT() {
    ORU_R01_VISIT retVal = getTyped("VISIT", ORU_R01_VISIT.class);
    return retVal;
  }


}

