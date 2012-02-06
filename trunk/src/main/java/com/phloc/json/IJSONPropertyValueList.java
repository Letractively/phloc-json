/**
 * Copyright (C) 2006-2012 phloc systems
 * http://www.phloc.com
 * office[at]phloc[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phloc.json;

import java.util.List;

/**
 * Interface representing a list of property values ({@link IJSONPropertyValue})
 * 
 * @author Boris Gregorcic
 * @param <DATATYPE>
 *        The inner data type
 */
public interface IJSONPropertyValueList <DATATYPE> extends
                                                   IJSONPropertyValueComplex <List <IJSONPropertyValue <DATATYPE>>>
{
  /**
   * @return the list of {@link IJSONPropertyValue}s
   */
  List <IJSONPropertyValue <DATATYPE>> getValues ();

  /**
   * @return A list of the inner data values (the data items stored inside the
   *         {@link IJSONPropertyValue}s)
   */
  List <DATATYPE> getDataValues ();

  /**
   * adds the passed value in the list
   * 
   * @param aValue
   *        the value to add
   */
  void addValue (IJSONPropertyValue <DATATYPE> aValue);

  /**
   * adds all passed values in the list
   * 
   * @param aValues
   *        the values to add
   */
  void addAllValues (List <IJSONPropertyValue <DATATYPE>> aValues);

  IJSONPropertyValueList <DATATYPE> getClone ();
}
