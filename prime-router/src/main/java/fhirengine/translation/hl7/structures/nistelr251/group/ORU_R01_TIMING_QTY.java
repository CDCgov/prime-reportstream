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
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.nistelr251.segment.TQ1;
import fhirengine.translation.hl7.structures.nistelr251.segment.TQ2;

/**
 * <p>Represents a ORU_R01_TIMING_QTY group structure (PATIENT_RESULT.ORDER_OBSERVATION.TIMING_QTY).
 * A Group is an ordered collection of message segments that can repeat together or be optionally in/excluded together.
 * This Group contains the following elements:
 * </p>
 * <ul>
 * <li>1: TQ1 (Timing/Quantity) <b>  </b></li>
 * <li>2: TQ2 (Timing/Quantity Relationship) <b>optional  </b></li>
 * </ul>
 */
//@SuppressWarnings("unused")
public class ORU_R01_TIMING_QTY extends AbstractGroup {

  /**
   * Creates a new ORU_R01_TIMING_QTY group
   */
  public ORU_R01_TIMING_QTY(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(TQ1.class, true, false, false);
      this.add(TQ2.class, false, false, false);
    } catch (HL7Exception e) {
      log.error("Unexpected error creating ORU_R01_TIMING_QTY - this is probably a bug in the source code generator.", e);
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
   * TQ1 (Timing/Quantity) - creates it if necessary
   */
  public TQ1 getTQ1() {
    TQ1 retVal = getTyped("TQ1", TQ1.class);
    return retVal;
  }


  /**
   * Returns
   * TQ2 (Timing/Quantity Relationship) - creates it if necessary
   */
  public TQ2 getTQ2() {
    TQ2 retVal = getTyped("TQ2", TQ2.class);
    return retVal;
  }


}

