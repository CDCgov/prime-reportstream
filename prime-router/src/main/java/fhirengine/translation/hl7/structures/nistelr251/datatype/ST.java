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
 * The Original Code is "ST.java".  Description:
 * "Primitive class ST"
 *
 * The Initial Developer of the Original Code is University Health Network. Copyright (C)
 * 2013.  All Rights Reserved.
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


package fhirengine.translation.hl7.structures.nistelr251.datatype;

import ca.uhn.hl7v2.model.Message;


/**
 * <p>Represents an HL7 ST (Field Separator) data type.
 * A ST contains a single String value.</p>
 */
@SuppressWarnings("unused")
public class ST extends ca.uhn.hl7v2.model.primitive.AbstractTextPrimitive {

  /**
   * Constructs an uninitialized ST.
   *
   * @param message the Message to which this Type belongs
   */
  public ST(Message message) {
    super(message);
  }


  /**
   * @return "2.5.1"
   */
  public String getVersion() {
    return "2.5.1";
  }

}

