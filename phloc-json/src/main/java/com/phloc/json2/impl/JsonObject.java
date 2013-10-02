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
package com.phloc.json2.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.state.EChange;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.ToStringGenerator;
import com.phloc.json2.IJson;
import com.phloc.json2.IJsonObject;
import com.phloc.json2.convert.JsonConverter;

/**
 * Default implementation of {@link IJsonObject}.
 * 
 * @author Philip Helger
 */
public class JsonObject implements IJsonObject
{
  private final Map <String, IJson> m_aValues;

  public JsonObject ()
  {
    this (16);
  }

  public JsonObject (@Nonnegative final int nInitialCapacity)
  {
    m_aValues = new LinkedHashMap <String, IJson> (nInitialCapacity);
  }

  public boolean isArray ()
  {
    return false;
  }

  public boolean isObject ()
  {
    return true;
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
  public Iterator <Entry <String, IJson>> iterator ()
  {
    return m_aValues.entrySet ().iterator ();
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, @Nonnull final IJson aValue)
  {
    if (StringHelper.hasNoText (sName))
      throw new IllegalArgumentException ("name");
    if (aValue == null)
      throw new NullPointerException ("value");

    m_aValues.put (sName, aValue);
    return this;
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, @Nullable final Object aValue)
  {
    final IJson aJson = JsonConverter.convertToJson (aValue);
    return add (sName, aJson);
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final boolean bValue)
  {
    return add (sName, JsonValue.create (bValue));
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final byte nValue)
  {
    return add (sName, JsonValue.create (nValue));
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final char cValue)
  {
    return add (sName, JsonValue.create (cValue));
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final double dValue)
  {
    return add (sName, JsonValue.create (dValue));
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final float fValue)
  {
    return add (sName, JsonValue.create (fValue));
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final int nValue)
  {
    return add (sName, JsonValue.create (nValue));
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final long nValue)
  {
    return add (sName, JsonValue.create (nValue));
  }

  @Nonnull
  public JsonObject add (@Nonnull @Nonempty final String sName, final short nValue)
  {
    return add (sName, JsonValue.create (nValue));
  }

  @Nonnull
  public EChange removeKey (@Nullable final String sName)
  {
    return EChange.valueOf (m_aValues.remove (sName) != null);
  }

  public boolean containsKey (@Nullable final String sName)
  {
    return m_aValues.containsKey (sName);
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> keySet ()
  {
    return ContainerHelper.newSet (m_aValues.keySet ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <IJson> values ()
  {
    return ContainerHelper.newList (m_aValues.values ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, IJson> getAll ()
  {
    return ContainerHelper.newMap (m_aValues);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("values", m_aValues).toString ();
  }
}