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
package com.phloc.json;

import java.io.InputStream;
import java.io.Reader;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phloc.json.impl.JSONParsingException;
import com.phloc.json2.serialize.JsonReadException;

/**
 * Utility class around the Jackson JSON implementation
 * 
 * @author Philip Helger
 */
@Immutable
public final class JacksonHelper
{
  private JacksonHelper ()
  {}

  @Nonnull
  public static ObjectMapper createObjectMapper ()
  {
    return com.phloc.json2.serialize.JacksonHelper.createObjectMapper ();
  }

  /**
   * Parse the passed JSON string into a {@link JsonNode} structure for further
   * processing
   * 
   * @param sJSON
   *        the JSON string to convert, can be any valid JSON mark-up
   * @return the resulting JSON node structure
   * @throws JSONParsingException
   *         in case parsing failed
   */
  @Nonnull
  public static JsonNode parseToNode (@Nonnull final String sJSON) throws JSONParsingException
  {
    try
    {
      return com.phloc.json2.serialize.JacksonHelper.parseToNode (sJSON);
    }
    catch (final JsonReadException ex)
    {
      throw new JSONParsingException (ex.getMessage (), ex.getCause ()); // NOPMD
    }
  }

  /**
   * Parse the passed {@link InputStream} into a {@link JsonNode} structure for
   * further processing
   * 
   * @param aIS
   *        the JSON input stream to convert, can be any valid JSON mark-up
   * @return the resulting JSON node structure
   * @throws JSONParsingException
   *         in case parsing failed
   */
  @Nonnull
  public static JsonNode parseToNode (@Nonnull @WillClose final InputStream aIS) throws JSONParsingException
  {
    if (aIS == null)
    {
      throw new NullPointerException ("aIS"); //$NON-NLS-1$
    }
    try
    {
      return com.phloc.json2.serialize.JacksonHelper.parseToNode (aIS);
    }
    catch (final JsonReadException ex)
    {
      throw new JSONParsingException (ex.getMessage (), ex.getCause ());// NOPMD
    }
  }

  /**
   * Parse the passed {@link Reader} into a {@link JsonNode} structure for
   * further processing
   * 
   * @param aReader
   *        the JSON Reader to convert, can be any valid JSON mark-up
   * @return the resulting JSON node structure
   * @throws JSONParsingException
   *         in case parsing failed
   */
  @Nonnull
  public static JsonNode parseToNode (@Nonnull @WillClose final Reader aReader) throws JSONParsingException
  {
    if (aReader == null)
    {
      throw new NullPointerException ("aReader"); //$NON-NLS-1$
    }
    try
    {
      return com.phloc.json2.serialize.JacksonHelper.parseToNode (aReader);
    }
    catch (final JsonReadException ex)
    {
      throw new JSONParsingException (ex.getMessage (), ex.getCause ());// NOPMD
    }
  }
}
