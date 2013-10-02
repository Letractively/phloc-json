/**
 * Copyright (C) 2006-2013 phloc systems
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
package com.phloc.json2;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.state.EChange;
import com.phloc.commons.string.ToStringGenerator;
import com.phloc.json2.convert.JsonConverter;

/**
 * Default implementation of {@link IJsonArray}
 * 
 * @author Philip Helger
 */
public class JsonArray implements IJsonArray
{
  private final List <IJson> m_aValues = new ArrayList <IJson> ();

  public JsonArray ()
  {}

  public boolean isArray ()
  {
    return true;
  }

  public boolean isObject ()
  {
    return false;
  }

  public boolean isValue ()
  {
    return false;
  }

  @Nonnegative
  public int size ()
  {
    return m_aValues.size ();
  }

  public boolean isEmpty ()
  {
    return m_aValues.isEmpty ();
  }

  @Nonnull
  public JsonArray add (@Nonnull final IJson aValue)
  {
    if (aValue == null)
      throw new NullPointerException ("value");
    m_aValues.add (aValue);
    return this;
  }

  @Nonnull
  public JsonArray add (@Nullable final Object aValue)
  {
    final IJson aJson = JsonConverter.convertToJson (aValue);
    return add (aJson);
  }

  @Nonnull
  public JsonArray add (final boolean bValue)
  {
    return add (JsonValue.create (bValue));
  }

  @Nonnull
  public JsonArray add (final byte nValue)
  {
    return add (JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (final int nValue)
  {
    return add (JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (final long nValue)
  {
    return add (JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (final short nValue)
  {
    return add (JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (final float fValue)
  {
    return add (JsonValue.create (fValue));
  }

  @Nonnull
  public JsonArray add (final double dValue)
  {
    return add (JsonValue.create (dValue));
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, @Nonnull final IJson aValue)
  {
    if (aValue == null)
      throw new NullPointerException ("value");
    m_aValues.add (nIndex, aValue);
    return this;
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, @Nullable final Object aValue)
  {
    final IJson aJson = JsonConverter.convertToJson (aValue);
    return add (nIndex, aJson);
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, final boolean bValue)
  {
    return add (nIndex, JsonValue.create (bValue));
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, final byte nValue)
  {
    return add (nIndex, JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, final int nValue)
  {
    return add (nIndex, JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, final long nValue)
  {
    return add (nIndex, JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, final short nValue)
  {
    return add (nIndex, JsonValue.create (nValue));
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, final float fValue)
  {
    return add (nIndex, JsonValue.create (fValue));
  }

  @Nonnull
  public JsonArray add (@Nonnegative final int nIndex, final double dValue)
  {
    return add (nIndex, JsonValue.create (dValue));
  }

  @Nonnull
  public EChange removeAtIndex (@Nonnegative final int nIndex)
  {
    return ContainerHelper.removeElementAtIndex (m_aValues, nIndex);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <IJson> getAll ()
  {
    return ContainerHelper.newList (m_aValues);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("values", m_aValues).toString ();
  }
}
