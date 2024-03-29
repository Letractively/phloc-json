/**
 * Copyright (C) 2006-2014 phloc systems
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
package com.phloc.json.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.equals.EqualsUtils;
import com.phloc.commons.hash.HashCodeGenerator;
import com.phloc.commons.state.EChange;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.ToStringGenerator;
import com.phloc.commons.typeconvert.TypeConverter;
import com.phloc.json.IJSONObject;
import com.phloc.json.IJSONProperty;
import com.phloc.json.IJSONPropertyValue;
import com.phloc.json.IJSONPropertyValueList;
import com.phloc.json.IJSONPropertyValueNotParsable;
import com.phloc.json.JSONUtil;
import com.phloc.json.impl.value.AbstractJSONPropertyValue;
import com.phloc.json.impl.value.AbstractJSONPropertyValueNumeric;
import com.phloc.json.impl.value.JSONPropertyValueBigDecimal;
import com.phloc.json.impl.value.JSONPropertyValueBigInteger;
import com.phloc.json.impl.value.JSONPropertyValueBoolean;
import com.phloc.json.impl.value.JSONPropertyValueDouble;
import com.phloc.json.impl.value.JSONPropertyValueFunction;
import com.phloc.json.impl.value.JSONPropertyValueFunctionPrebuild;
import com.phloc.json.impl.value.JSONPropertyValueInteger;
import com.phloc.json.impl.value.JSONPropertyValueJSONObject;
import com.phloc.json.impl.value.JSONPropertyValueKeyword;
import com.phloc.json.impl.value.JSONPropertyValueList;
import com.phloc.json.impl.value.JSONPropertyValueLong;
import com.phloc.json.impl.value.JSONPropertyValueString;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents a JSON object having a map of named JSON properties
 * 
 * @author Boris Gregorcic, philip
 */
@SuppressWarnings ("deprecation")
public class JSONObject extends AbstractJSONPropertyValue <IJSONObject> implements IJSONObject
{

  private static final long serialVersionUID = 6726669796398394782L;

  private static final Logger LOG = LoggerFactory.getLogger (JSONObject.class);

  private final Map <String, IJSONProperty <?>> m_aProperties = new LinkedHashMap <String, IJSONProperty <?>> ();

  /**
   * Default Ctor. Handle with care as it by default sets a <code>null</code>
   * value which is most probable be crashing somewhere inside this class, if no
   * data is provided afterwards!
   */
  public JSONObject ()
  {
    super ();
  }

  public JSONObject (@Nonnull final Iterable <? extends IJSONProperty <?>> aProperties)
  {
    super ();
    if (aProperties == null)
    {
      throw new NullPointerException ("properties"); //$NON-NLS-1$
    }
    for (final IJSONProperty <?> aProperty : aProperties)
      setProperty (aProperty);
  }

  /**
   * Override since otherwise JSONObjects might return null for certain
   * constructors
   * 
   * @return this
   */
  @Override
  @Nonnull
  public IJSONObject getData ()
  {
    return this;
  }

  @Override
  @Nonnull
  public JSONObject setProperty (@Nonnull final IJSONProperty <?> aProperty)
  {
    if (aProperty == null)
    {
      throw new NullPointerException ("property"); //$NON-NLS-1$
    }
    this.m_aProperties.put (aProperty.getName (), aProperty.getClone ());
    return this;
  }

  @Override
  @Nullable
  public IJSONProperty <?> getProperty (@Nullable final String sName)
  {
    return this.m_aProperties.get (sName);
  }

  @Override
  @Nullable
  public Object getPropertyValueData (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    return aValue == null ? null : aValue.getData ();
  }

  @Override
  @Nullable
  public Object getNumericProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof AbstractJSONPropertyValueNumeric)
    {
      return aValue.getData ();
    }
    return null;
  }

  @Override
  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllPropertyNames ()
  {
    return ContainerHelper.newOrderedSet (this.m_aProperties.keySet ());
  }

  @Nullable
  private IJSONPropertyValue <?> getPropertyValueInternal (@Nullable final String sName)
  {
    final IJSONProperty <?> aProperty = this.m_aProperties.get (sName);
    return aProperty == null ? null : aProperty.getValue ();
  }

  // we need to differentiate here the case of set boolean values from the case
  // the property is not found or of wrong type (null). Un-boxing is prevented
  // by PMD anyway and the method is annotated as @Nullable
  @Override
  @Nullable
  @SuppressFBWarnings ("NP_BOOLEAN_RETURN_NULL")
  public Boolean getBooleanProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof JSONPropertyValueBoolean)
    {
      return ((JSONPropertyValueBoolean) aValue).getData ();
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setBooleanProperty (@Nonnull final String sName, final boolean bDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueBoolean (bDataValue)));
  }

  @Override
  @Nonnull
  public JSONObject setBooleanProperty (@Nonnull final String sName, @Nonnull final Boolean aDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueBoolean (aDataValue)));
  }

  @Override
  @Nullable
  public Double getDoubleProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof AbstractJSONPropertyValueNumeric)
    {
      return Double.valueOf (((AbstractJSONPropertyValueNumeric <?>) aValue).getData ().doubleValue ());
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setDoubleProperty (@Nonnull final String sName, final double nDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueDouble (nDataValue)));
  }

  @Override
  @Nonnull
  public JSONObject setDoubleProperty (@Nonnull final String sName, @Nonnull final Double aDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueDouble (aDataValue)));
  }

  @Override
  @Nullable
  public Integer getIntegerProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof AbstractJSONPropertyValueNumeric)
    {
      return Integer.valueOf (((AbstractJSONPropertyValueNumeric <?>) aValue).getData ().intValue ());
    }
    return null;
  }

  @Override
  @Nonnull
  public Integer getIntegerPropertyNonNull (@Nullable final String sName)
  {
    final Integer aValue = getIntegerProperty (sName);
    if (aValue == null)
    {
      throw new NullPointerException ("No Integer value available for property '" + sName + "'!"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return aValue;
  }

  @Override
  @Nonnull
  public JSONObject setIntegerProperty (@Nonnull final String sName, final int nDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueInteger (nDataValue)));
  }

  @Override
  @Nonnull
  public JSONObject setIntegerProperty (@Nonnull final String sName, @Nonnull final Integer aDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueInteger (aDataValue)));
  }

  @Override
  @Nullable
  public Long getLongProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof AbstractJSONPropertyValueNumeric)
    {
      return Long.valueOf (((AbstractJSONPropertyValueNumeric <?>) aValue).getData ().longValue ());
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setLongProperty (@Nonnull final String sName, final long nDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueLong (nDataValue)));
  }

  @Override
  @Nonnull
  public JSONObject setLongProperty (@Nonnull final String sName, @Nonnull final Long aDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueLong (aDataValue)));
  }

  @Override
  @Nullable
  public String getKeywordProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof JSONPropertyValueKeyword)
    {
      return ((JSONPropertyValueKeyword) aValue).getData ();
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setKeywordProperty (@Nonnull final String sName, @Nonnull final String sDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueKeyword (sDataValue)));
  }

  @Override
  @Nonnull
  public JSONObject setFunctionProperty (@Nonnull final String sName,
                                         @Nonnull final String sBody,
                                         @Nullable final String... aParams)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueFunction (sBody, aParams)));
  }

  @Override
  @Nonnull
  public JSONObject setFunctionPrebuildProperty (@Nonnull final String sName, @Nonnull final String sFunctionCode)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueFunctionPrebuild (sFunctionCode)));
  }

  @Override
  @SuppressWarnings ("unchecked")
  public <I> List <I> getListProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof IJSONPropertyValueList <?>)
    {
      return (List <I>) ((IJSONPropertyValueList <?>) aValue).getDataValues ();
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setListProperty (@Nonnull final String sName, @Nonnull final IJSONPropertyValueList <?> aList)
  {
    return setProperty (JSONProperty.create (sName, aList));
  }

  @Override
  @Nullable
  public IJSONObject getObjectProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof IJSONObject)
    {
      return (IJSONObject) aValue;
    }
    if (aValue instanceof JSONPropertyValueJSONObject)
    {
      return ((JSONPropertyValueJSONObject) aValue).getData ();
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setObjectProperty (@Nonnull final String sName, @Nonnull final IJSONObject aObject)
  {
    return setProperty (JSONProperty.create (sName, aObject));
  }

  @Override
  @Nullable
  public String getStringProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof JSONPropertyValueString)
    {
      return ((JSONPropertyValueString) aValue).getData ();
    }
    return null;
  }

  @Override
  public String getStringPropertyNonEmpty (@Nonnull @Nonempty final String sName)
  {
    if (StringHelper.hasNoText (sName))
    {
      throw new IllegalArgumentException ("sName must not be null or empty!"); //$NON-NLS-1$
    }
    final String sValue = getStringProperty (sName);
    if (StringHelper.hasNoText (sValue))
    {
      throw new IllegalArgumentException ("Value for property '" + sName + "' must not be empty or null!"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return sValue;
  }

  @Override
  @Nonnull
  public JSONObject setStringProperty (@Nonnull final String sName, @Nonnull final String sDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueString (sDataValue)));
  }

  @Override
  @Nonnull
  public IJSONObject setStringProperties (@Nonnull final Map <String, String> aMap)
  {
    for (final Map.Entry <String, String> aEntry : aMap.entrySet ())
    {
      setStringProperty (aEntry.getKey (), aEntry.getValue ());
    }
    return this;
  }

  @Override
  @Nullable
  public BigInteger getBigIntegerProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof AbstractJSONPropertyValueNumeric)
    {
      return BigInteger.valueOf (((AbstractJSONPropertyValueNumeric <?>) aValue).getData ().longValue ());
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setBigIntegerProperty (@Nonnull final String sName, @Nonnull final BigInteger aDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueBigInteger (aDataValue)));
  }

  @Override
  @Nullable
  public BigDecimal getBigDecimalProperty (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof AbstractJSONPropertyValueNumeric)
    {
      return BigDecimal.valueOf (((AbstractJSONPropertyValueNumeric <?>) aValue).getData ().doubleValue ());
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setBigDecimalProperty (@Nonnull final String sName, @Nonnull final BigDecimal aDataValue)
  {
    return setProperty (JSONProperty.create (sName, new JSONPropertyValueBigDecimal (aDataValue)));
  }

  @Override
  @Nonnull
  public JSONObject setProperty (@Nonnull final String sName, @Nullable final Object aValue)
  {
    // Default: no type converter
    return setProperty (sName, aValue, false);
  }

  @Override
  @Nonnull
  public JSONObject setProperty (@Nonnull final String sName,
                                 @Nullable final Object aValue,
                                 final boolean bUseTypeConverter)
  {
    if (aValue == null)
    {
      removeProperty (sName);
      return this;
    }
    if (aValue instanceof IJSONObject)
    {
      return setObjectProperty (sName, (IJSONObject) aValue);
    }
    if (aValue instanceof IJSONPropertyValue <?>)
    {
      return setProperty (JSONProperty.create (sName, (IJSONPropertyValue <?>) aValue));
    }
    if (aValue instanceof Boolean)
    {
      return setBooleanProperty (sName, (Boolean) aValue);
    }
    if (aValue instanceof BigInteger)
    {
      return setBigIntegerProperty (sName, (BigInteger) aValue);
    }
    if (aValue instanceof BigDecimal)
    {
      return setBigDecimalProperty (sName, (BigDecimal) aValue);
    }
    if (aValue instanceof Double)
    {
      return setDoubleProperty (sName, (Double) aValue);
    }
    if (aValue instanceof Integer)
    {
      return setIntegerProperty (sName, (Integer) aValue);
    }
    if (aValue instanceof Long)
    {
      return setLongProperty (sName, (Long) aValue);
    }
    if (aValue instanceof String)
    {
      return setStringProperty (sName, (String) aValue);
    }
    // Unknown type -> use type converter?
    String sValue;
    if (bUseTypeConverter)
    {
      sValue = TypeConverter.convertIfNecessary (aValue, String.class);
    }
    else
    {
      LOG.warn ("Setting property of type " + //$NON-NLS-1$
                aValue.getClass ().getName () +
                " as String without TypeConverter!"); //$NON-NLS-1$
      sValue = String.valueOf (aValue);
    }
    return setStringProperty (sName, sValue);
  }

  @Override
  @Nullable
  public List <? extends IJSONPropertyValue <?>> getListValues (@Nullable final String sName)
  {
    final IJSONPropertyValue <?> aValue = getPropertyValueInternal (sName);
    if (aValue instanceof IJSONPropertyValueList <?>)
    {
      return ((IJSONPropertyValueList <?>) aValue).getValues ();
    }
    return null;
  }

  @Override
  @Nonnull
  public JSONObject setObjectListProperty (@Nonnull final String sName,
                                           @Nonnull final Iterable <? extends IJSONObject> aObjectList)
  {
    final IJSONPropertyValueList <IJSONObject> aList = new JSONPropertyValueList <IJSONObject> ();
    for (final IJSONObject aObject : aObjectList)
    {
      aList.addValue (aObject);
    }
    return setProperty (JSONProperty.create (sName, aList));
  }

  @Override
  @Nonnull
  public List <IJSONObject> getObjectListProperty (@Nullable final String sName)
  {
    final List <IJSONObject> aReturn = new ArrayList <IJSONObject> ();
    final List <?> aList = getListProperty (sName);
    if (aList != null)
    {
      for (final Object aValue : aList)
      {
        if (aValue instanceof IJSONObject)
        {
          aReturn.add ((IJSONObject) aValue);
        }
      }
    }
    return aReturn;
  }

  @Override
  @Nonnull
  public JSONObject setStringListProperty (@Nonnull final String sName, @Nonnull final Iterable <String> aStringList)
  {
    final IJSONPropertyValueList <JSONPropertyValueString> aList = new JSONPropertyValueList <JSONPropertyValueString> ();
    for (final String sValue : aStringList)
    {
      aList.addValue (new JSONPropertyValueString (sValue));
    }
    return setProperty (JSONProperty.create (sName, aList));
  }

  @Override
  @Nonnull
  public JSONObject setMixedListProperty (@Nonnull final String sName, @Nonnull final Iterable <?> aValues)
  {
    final IJSONPropertyValueList <IJSONPropertyValue <?>> aList = new JSONPropertyValueList <IJSONPropertyValue <?>> ();
    for (final Object aValue : aValues)
    {
      aList.addValue (JSONUtil.getJSONValue (aValue));
    }
    return setProperty (JSONProperty.create (sName, aList));
  }

  @Override
  @Nonnull
  public JSONObject setIntegerListProperty (@Nonnull final String sName, @Nonnull final int [] aIntList)
  {
    final IJSONPropertyValueList <JSONPropertyValueInteger> aList = new JSONPropertyValueList <JSONPropertyValueInteger> ();
    for (final int nValue : aIntList)
    {
      aList.addValue (new JSONPropertyValueInteger (nValue));
    }
    return setProperty (JSONProperty.create (sName, aList));
  }

  @Override
  public JSONObject setIntegerListProperty (@Nonnull final String sName, @Nonnull final List <Integer> aIntegerList)
  {
    final IJSONPropertyValueList <JSONPropertyValueInteger> aList = new JSONPropertyValueList <JSONPropertyValueInteger> ();
    for (final Integer nValue : aIntegerList)
    {
      aList.addValue (new JSONPropertyValueInteger (nValue));
    }
    return setProperty (JSONProperty.create (sName, aList));
  }

  @Override
  @Deprecated
  @Nonnull
  public JSONObject setListOfListProperty (@Nonnull final String sName,
                                           @Nonnull final Iterable <? extends Iterable <String>> aListOfList)
  {
    return setListOfStringListProperty (sName, aListOfList);
  }

  @Override
  @Nonnull
  public JSONObject setListOfStringListProperty (@Nonnull final String sName,
                                                 @Nonnull final Iterable <? extends Iterable <String>> aListOfList)
  {
    final JSONPropertyValueList <IJSONPropertyValueList <JSONPropertyValueString>> aList = new JSONPropertyValueList <IJSONPropertyValueList <JSONPropertyValueString>> ();
    for (final Iterable <String> aRow : aListOfList)
    {
      final IJSONPropertyValueList <JSONPropertyValueString> aRowList = new JSONPropertyValueList <JSONPropertyValueString> ();
      for (final String aCell : aRow)
      {
        aRowList.addValue (new JSONPropertyValueString (aCell));
      }
      aList.addValue (aRowList);
    }
    return setProperty (JSONProperty.create (sName, aList));
  }

  @Override
  public void appendJSONString (@Nonnull final StringBuilder aResult, final boolean bAlignAndIndent, final int nLevel)
  {
    appendNewLine (aResult, bAlignAndIndent);
    indent (aResult, nLevel, bAlignAndIndent);
    aResult.append (CJSONConstants.OBJECT_START);
    appendNewLine (aResult, bAlignAndIndent);

    final Set <String> aPropertyNames = getAllPropertyNames ();
    int nIndex = 0;
    for (final String sProperty : aPropertyNames)
    {
      final IJSONProperty <?> aProperty = getProperty (sProperty);
      aProperty.appendJSONString (aResult, bAlignAndIndent, nLevel + 1);
      if (nIndex < aPropertyNames.size () - 1)
      {
        aResult.append (CJSONConstants.TOKEN_SEPARATOR);
      }
      appendNewLine (aResult, bAlignAndIndent);
      nIndex++;
    }
    indent (aResult, nLevel, bAlignAndIndent);
    aResult.append (CJSONConstants.OBJECT_END);
  }

  @Override
  @Nonnull
  public EChange removeProperty (@Nullable final String sName)
  {
    return EChange.valueOf (this.m_aProperties.remove (sName) != null);
  }

  @Override
  public EChange apply (@Nullable final IJSONObject aObjectToApply)
  {
    EChange eChange = EChange.UNCHANGED;
    if (aObjectToApply != null)
    {
      for (final String sPropName : aObjectToApply.getAllPropertyNames ())
      {
        setProperty (sPropName, aObjectToApply.getProperty (sPropName).getValue ());
        eChange = EChange.CHANGED;
      }
    }
    return eChange;
  }

  @Override
  public EChange apply (@Nullable final IJSONObject aObjectToApply, @Nonnull @Nonempty final String sPropertyName)
  {
    EChange eChange = EChange.UNCHANGED;
    if (aObjectToApply != null)
    {
      final IJSONProperty <?> aProperty = aObjectToApply.getProperty (sPropertyName);
      if (aProperty != null)
      {
        this.setProperty (aProperty);
        eChange = EChange.CHANGED;
      }
    }
    return eChange;
  }

  @Override
  public boolean containsNotParsableProperty ()
  {
    for (final IJSONProperty <?> aProperty : this.m_aProperties.values ())
    {
      final IJSONPropertyValue <?> aValue = aProperty.getValue ();
      if (aValue instanceof IJSONPropertyValueNotParsable <?>)
      {
        return true;
      }
      if (aValue instanceof IJSONObject && ((IJSONObject) aValue).containsNotParsableProperty ())
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty ()
  {
    return this.m_aProperties.isEmpty ();
  }

  @Override
  @Nonnegative
  public int getPropertyCount ()
  {
    return this.m_aProperties.size ();
  }

  @Override
  @Nonnull
  public JSONObject getClone ()
  {
    return new JSONObject (this.m_aProperties.values ());
  }

  @Override
  public boolean equals (final Object aOther)
  {
    if (aOther == this)
    {
      return true;
    }
    if (!super.equals (aOther))
    {
      return false;
    }
    final JSONObject rhs = (JSONObject) aOther;
    return EqualsUtils.equals (this.m_aProperties, rhs.m_aProperties);
  }

  @Override
  public int hashCode ()
  {
    return HashCodeGenerator.getDerived (super.hashCode ()).append (this.m_aProperties).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("properties", this.m_aProperties).toString (); //$NON-NLS-1$
  }
}
