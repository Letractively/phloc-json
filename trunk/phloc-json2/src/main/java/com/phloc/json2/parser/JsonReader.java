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
package com.phloc.json2.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.phloc.commons.ValueEnforcer;
import com.phloc.commons.annotations.PresentForCodeCoverage;
import com.phloc.commons.charset.CharsetManager;
import com.phloc.commons.charset.EUnicodeBOM;
import com.phloc.commons.collections.ArrayHelper;
import com.phloc.commons.collections.pair.ReadonlyPair;
import com.phloc.commons.io.IInputStreamProvider;
import com.phloc.commons.io.IReadableResource;
import com.phloc.commons.io.IReaderProvider;
import com.phloc.commons.io.resource.FileSystemResource;
import com.phloc.commons.io.streamprovider.StringInputStreamProvider;
import com.phloc.commons.io.streamprovider.StringReaderProvider;
import com.phloc.commons.io.streams.NonBlockingStringReader;
import com.phloc.commons.io.streams.StreamUtils;
import com.phloc.json2.IJson;
import com.phloc.json2.parser.errorhandler.DoNothingJsonParseExceptionHandler;
import com.phloc.json2.parser.errorhandler.IJsonParseErrorHandler;
import com.phloc.json2.parser.errorhandler.IJsonParseExceptionHandler;
import com.phloc.json2.parser.errorhandler.LoggingJsonParseExceptionHandler;
import com.phloc.json2.parser.errorhandler.ThrowingJsonParseErrorHandler;

/**
 * This is the central user class for reading and parsing Json from different
 * sources. This class reads full Json declarations only.
 * 
 * @author Philip Helger
 */
@ThreadSafe
public final class JsonReader
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (JsonReader.class);
  private static final ReadWriteLock s_aRWLock = new ReentrantReadWriteLock ();

  // Use the ThrowingJsonParseErrorHandler for maximum backward compatibility
  @GuardedBy ("s_aRWLock")
  private static IJsonParseErrorHandler s_aDefaultParseErrorHandler = ThrowingJsonParseErrorHandler.getInstance ();

  // Use the LoggingJsonParseExceptionHandler for maximum backward compatibility
  @GuardedBy ("s_aRWLock")
  private static IJsonParseExceptionHandler s_aDefaultParseExceptionHandler = new LoggingJsonParseExceptionHandler ();

  @PresentForCodeCoverage
  @SuppressWarnings ("unused")
  private static final JsonReader s_aInstance = new JsonReader ();

  private JsonReader ()
  {}

  /**
   * @return The default Json parse error handler. May be <code>null</code>. For
   *         backwards compatibility reasons this is be default an instance of
   *         {@link ThrowingJsonParseErrorHandler}.
   */
  @Nullable
  public static IJsonParseErrorHandler getDefaultParseErrorHandler ()
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_aDefaultParseErrorHandler;
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  /**
   * Set the default Json parse error handler (for recoverable errors).
   * 
   * @param aDefaultParseErrorHandler
   *        The new default error handler to be used. May be <code>null</code>
   *        to indicate that no special error handler should be used.
   */
  public static void setDefaultParseErrorHandler (@Nullable final IJsonParseErrorHandler aDefaultParseErrorHandler)
  {
    s_aRWLock.writeLock ().lock ();
    try
    {
      s_aDefaultParseErrorHandler = aDefaultParseErrorHandler;
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  /**
   * @return The default Json parse exception handler. May not be
   *         <code>null</code>. For backwards compatibility reasons this is be
   *         default an instance of {@link LoggingJsonParseExceptionHandler}.
   */
  @Nonnull
  public static IJsonParseExceptionHandler getDefaultParseExceptionHandler ()
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_aDefaultParseExceptionHandler;
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  /**
   * Set the default Json parse exception handler (for unrecoverable errors).
   * 
   * @param aDefaultParseExceptionHandler
   *        The new default exception handler to be used. May not be
   *        <code>null</code>.
   */
  public static void setDefaultParseExceptionHandler (@Nonnull final IJsonParseExceptionHandler aDefaultParseExceptionHandler)
  {
    ValueEnforcer.notNull (aDefaultParseExceptionHandler, "DefaultParseExceptionHandler");

    s_aRWLock.writeLock ().lock ();
    try
    {
      s_aDefaultParseExceptionHandler = aDefaultParseExceptionHandler;
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  /**
   * Main reading of the Json
   * 
   * @param aStream
   *        The stream to read from. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        A custom handler for recoverable errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        A custom handler for unrecoverable errors. May not be
   *        <code>null</code>.
   * @return <code>null</code> if parsing failed with an unrecoverable error
   *         (and no throwing exception handler is used), or <code>null</code>
   *         if a recoverable error occurred and no
   *         {@link com.phloc.css.reader.errorhandler.ThrowingJsonParseErrorHandler}
   *         was used or non-<code>null</code> if parsing succeeded.
   */
  @Nullable
  private static JsonNode _readStyleSheet (@Nonnull final CharStream aStream,
                                           @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                           @Nonnull final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    final ParserJsonTokenManager aTokenHdl = new ParserJsonTokenManager (aStream);
    final ParserJson aParser = new ParserJson (aTokenHdl);
    aParser.setCustomErrorHandler (aCustomErrorHandler);
    try
    {
      // Main parsing
      return aParser.json ();
    }
    catch (final ParseException ex)
    {
      // Unrecoverable error
      aCustomExceptionHandler.onException (ex);
      return null;
    }
  }

  /**
   * Check if the passed Json file can be parsed without error
   * 
   * @param aFile
   *        The file to be parsed. May not be <code>null</code>.
   * @param sCharset
   *        The charset to be used for reading the Json file in case neither a
   *        <code>@charset</code> rule nor a BOM is present. May not be
   *        <code>null</code>.
   * @return <code>true</code> if the file can be parsed without error,
   *         <code>false</code> if not
   * @throws IllegalArgumentException
   *         if the passed charset is unknown
   */
  @Deprecated
  public static boolean isValidJson (@Nonnull final File aFile, @Nonnull final String sCharset)
  {
    return isValidJson (new FileSystemResource (aFile), sCharset);
  }

  /**
   * Check if the passed Json file can be parsed without error
   * 
   * @param aFile
   *        The file to be parsed. May not be <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used for reading the Json file in case neither a
   *        <code>@charset</code> rule nor a BOM is present. May not be
   *        <code>null</code>.
   * @return <code>true</code> if the file can be parsed without error,
   *         <code>false</code> if not
   */
  public static boolean isValidJson (@Nonnull final File aFile, @Nonnull final Charset aFallbackCharset)
  {
    return isValidJson (new FileSystemResource (aFile), aFallbackCharset);
  }

  /**
   * Check if the passed Json resource can be parsed without error
   * 
   * @param aRes
   *        The resource to be parsed. May not be <code>null</code>.
   * @param sCharset
   *        The charset to be used for reading the Json file in case neither a
   *        <code>@charset</code> rule nor a BOM is present. May not be
   *        <code>null</code>.
   * @return <code>true</code> if the file can be parsed without error,
   *         <code>false</code> if not
   * @throws IllegalArgumentException
   *         if the passed charset is unknown
   */
  @Deprecated
  public static boolean isValidJson (@Nonnull final IReadableResource aRes, @Nonnull final String sCharset)
  {
    final Charset aFallbackCharset = CharsetManager.getCharsetFromName (sCharset);
    return isValidJson (aRes, aFallbackCharset);
  }

  /**
   * Check if the passed Json resource can be parsed without error
   * 
   * @param aRes
   *        The resource to be parsed. May not be <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used for reading the Json file in case neither a
   *        <code>@charset</code> rule nor a BOM is present. May not be
   *        <code>null</code>.
   * @return <code>true</code> if the file can be parsed without error,
   *         <code>false</code> if not
   */
  public static boolean isValidJson (@Nonnull final IReadableResource aRes, @Nonnull final Charset aFallbackCharset)
  {
    if (aRes == null)
      throw new NullPointerException ("resources");
    if (aFallbackCharset == null)
      throw new NullPointerException ("charset");

    final Reader aReader = aRes.getReader (aFallbackCharset);
    if (aReader == null)
    {
      s_aLogger.warn ("Failed to open Json reader " + aRes);
      return false;
    }
    return isValidJson (aReader);
  }

  /**
   * Check if the passed input stream can be resembled to valid Json content.
   * This is accomplished by fully parsing the Json file each time the method is
   * called. This is similar to calling
   * {@link #readFromStream(IInputStreamProvider, String)} and checking for a
   * non-<code>null</code> result.
   * 
   * @param aIS
   *        The input stream to use. Is automatically closed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @return <code>true</code> if the Json is valid according to the version,
   *         <code>false</code> if not
   */
  @Deprecated
  public static boolean isValidJson (@Nonnull @WillClose final InputStream aIS, @Nonnull final String sCharset)
  {
    if (aIS == null)
      throw new NullPointerException ("inputStream");
    if (sCharset == null)
      throw new NullPointerException ("charset");

    return isValidJson (StreamUtils.createReader (aIS, sCharset));
  }

  /**
   * Check if the passed input stream can be resembled to valid Json content.
   * This is accomplished by fully parsing the Json file each time the method is
   * called. This is similar to calling
   * {@link #readFromStream(IInputStreamProvider,Charset)} and checking for a
   * non-<code>null</code> result.
   * 
   * @param aIS
   *        The input stream to use. Is automatically closed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @return <code>true</code> if the Json is valid according to the version,
   *         <code>false</code> if not
   */
  public static boolean isValidJson (@Nonnull @WillClose final InputStream aIS, @Nonnull final Charset aFallbackCharset)
  {
    if (aIS == null)
      throw new NullPointerException ("inputStream");
    if (aFallbackCharset == null)
      throw new NullPointerException ("charset");

    return isValidJson (StreamUtils.createReader (aIS, aFallbackCharset));
  }

  /**
   * Check if the passed String can be resembled to valid Json content. This is
   * accomplished by fully parsing the Json file each time the method is called.
   * This is similar to calling {@link #readFromString(String, Charset)} and
   * checking for a non-<code>null</code> result.
   * 
   * @param sJson
   *        The Json string to scan. May not be <code>null</code>.
   * @return <code>true</code> if the Json is valid according to the version,
   *         <code>false</code> if not
   */
  public static boolean isValidJson (@Nonnull final String sJson)
  {
    if (sJson == null)
      throw new NullPointerException ("reader");

    return isValidJson (new NonBlockingStringReader (sJson));
  }

  /**
   * Check if the passed reader can be resembled to valid Json content. This is
   * accomplished by fully parsing the Json each time the method is called. This
   * is similar to calling
   * {@link #readFromStream(IInputStreamProvider, Charset)} and checking for a
   * non-<code>null</code> result.
   * 
   * @param aReader
   *        The reader to use. May not be <code>null</code>.
   * @return <code>true</code> if the Json is valid according to the version,
   *         <code>false</code> if not
   */
  public static boolean isValidJson (@Nonnull @WillClose final Reader aReader)
  {
    if (aReader == null)
      throw new NullPointerException ("reader");

    try
    {
      final JsonCharStream aCharStream = new JsonCharStream (aReader);
      final JsonNode aNode = _readStyleSheet (aCharStream,
                                              getDefaultParseErrorHandler (),
                                              DoNothingJsonParseExceptionHandler.getInstance ());
      return aNode != null;
    }
    finally
    {
      StreamUtils.close (aReader);
    }
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromString (@Nonnull final String sJson, @Nonnull final String sCharset)
  {
    return readFromString (sJson, sCharset, null, null);
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson, @Nonnull final Charset aFallbackCharset)
  {
    return readFromString (sJson, aFallbackCharset, null, null);
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nonnull final String sCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler)
  {
    return readFromString (sJson, sCharset, aCustomErrorHandler, null);
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nonnull final Charset aFallbackCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler)
  {
    return readFromString (sJson, aFallbackCharset, aCustomErrorHandler, null);
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nonnull final String sCharset,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromString (sJson, sCharset, null, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nonnull final Charset aFallbackCharset,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromString (sJson, aFallbackCharset, null, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nonnull final String sCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (new StringInputStreamProvider (sJson, sCharset),
                           sCharset,
                           aCustomErrorHandler,
                           aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed String using a byte stream.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nonnull final Charset aFallbackCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (new StringInputStreamProvider (sJson, aFallbackCharset),
                           aFallbackCharset,
                           aCustomErrorHandler,
                           aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed String using a character stream. An
   * eventually contained <code>@charset</code> rule is ignored.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson)
  {
    return readFromReader (new StringReaderProvider (sJson),
                           (IJsonParseErrorHandler) null,
                           (IJsonParseExceptionHandler) null);
  }

  /**
   * Read the Json from the passed String using a character stream. An
   * eventually contained <code>@charset</code> rule is ignored.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler)
  {
    return readFromReader (new StringReaderProvider (sJson), aCustomErrorHandler, (IJsonParseExceptionHandler) null);
  }

  /**
   * Read the Json from the passed String using a character stream. An
   * eventually contained <code>@charset</code> rule is ignored.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromReader (new StringReaderProvider (sJson), (IJsonParseErrorHandler) null, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed String using a character stream. An
   * eventually contained <code>@charset</code> rule is ignored.
   * 
   * @param sJson
   *        The source string containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromString (@Nonnull final String sJson,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromReader (new StringReaderProvider (sJson), aCustomErrorHandler, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromFile (@Nonnull final File aFile, @Nonnull final String sCharset)
  {
    return readFromFile (aFile, sCharset, null, null);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromFile (@Nonnull final File aFile, @Nonnull final Charset aFallbackCharset)
  {
    return readFromFile (aFile, aFallbackCharset, null, null);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromFile (@Nonnull final File aFile,
                                    @Nonnull final String sCharset,
                                    @Nullable final IJsonParseErrorHandler aCustomErrorHandler)
  {
    return readFromFile (aFile, sCharset, aCustomErrorHandler, null);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromFile (@Nonnull final File aFile,
                                    @Nonnull final Charset aFallbackCharset,
                                    @Nullable final IJsonParseErrorHandler aCustomErrorHandler)
  {
    return readFromFile (aFile, aFallbackCharset, aCustomErrorHandler, null);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromFile (@Nonnull final File aFile,
                                    @Nonnull final String sCharset,
                                    @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (new FileSystemResource (aFile), sCharset, null, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromFile (@Nonnull final File aFile,
                                    @Nonnull final Charset aFallbackCharset,
                                    @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (new FileSystemResource (aFile), aFallbackCharset, null, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromFile (@Nonnull final File aFile,
                                    @Nonnull final String sCharset,
                                    @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                    @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (new FileSystemResource (aFile), sCharset, aCustomErrorHandler, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed File.
   * 
   * @param aFile
   *        The file containing the Json to be parsed. May not be
   *        <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromFile (@Nonnull final File aFile,
                                    @Nonnull final Charset aFallbackCharset,
                                    @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                    @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (new FileSystemResource (aFile),
                           aFallbackCharset,
                           aCustomErrorHandler,
                           aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP, @Nonnull final String sCharset)
  {
    return readFromStream (aISP, sCharset, null, null);
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP,
                                      @Nonnull final String sCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler)
  {
    return readFromStream (aISP, sCharset, aCustomErrorHandler, null);
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP, @Nonnull final Charset aFallbackCharset)
  {
    return readFromStream (aISP, aFallbackCharset, null, null);
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP,
                                      @Nonnull final Charset aFallbackCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler)
  {
    return readFromStream (aISP, aFallbackCharset, aCustomErrorHandler, null);
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP,
                                      @Nonnull final String sCharset,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (aISP, sCharset, null, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param sCharset
   *        The charset name to be used in case neither a <code>@charset</code>
   *        rule nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  @Deprecated
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP,
                                      @Nonnull final String sCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    final Charset aFallbackCharset = CharsetManager.getCharsetFromName (sCharset);
    return readFromStream (aISP, aFallbackCharset, aCustomErrorHandler, aCustomExceptionHandler);
  }

  /**
   * Open the {@link InputStream} provided by the passed
   * {@link IInputStreamProvider}. If a BOM is present in the
   * {@link InputStream} it is read and if possible the charset is automatically
   * determined from the BOM.
   * 
   * @param aISP
   *        The input stream provider to use. May not be <code>null</code>.
   * @return <code>null</code> if no InputStream could be opened, the pair with
   *         non-<code>null</code> {@link InputStream} and a potentially
   *         <code>null</code> {@link Charset} otherwise.
   */
  @Nullable
  private static ReadonlyPair <InputStream, Charset> _getInputStreamWithoutBOM (@Nonnull final IInputStreamProvider aISP)
  {
    // Try to open input stream
    final InputStream aIS = aISP.getInputStream ();
    if (aIS == null)
      return null;

    // Check for BOM
    final int nMaxBOMBytes = EUnicodeBOM.getMaximumByteCount ();
    final PushbackInputStream aPIS = new PushbackInputStream (aIS, nMaxBOMBytes);
    try
    {
      final byte [] aBOM = new byte [nMaxBOMBytes];
      final int nReadBOMBytes = aPIS.read (aBOM);
      Charset aDeterminedCharset = null;
      if (nReadBOMBytes > 0)
      {
        // Some byte BOMs were read
        final EUnicodeBOM eBOM = EUnicodeBOM.getFromBytesOrNull (ArrayHelper.getCopy (aBOM, 0, nReadBOMBytes));
        if (eBOM == null)
        {
          // Unread the whole BOM
          aPIS.unread (aBOM, 0, nReadBOMBytes);
        }
        else
        {
          // Unread the unnecessary parts of the BOM
          final int nBOMBytes = eBOM.getByteCount ();
          if (nBOMBytes < nReadBOMBytes)
            aPIS.unread (aBOM, nBOMBytes, nReadBOMBytes - nBOMBytes);

          // Use the Charset of the BOM - maybe null!
          aDeterminedCharset = eBOM.getCharset ();
        }
      }
      return new ReadonlyPair <InputStream, Charset> (aPIS, aDeterminedCharset);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Failed to determine BOM", ex);
      return null;
    }
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP,
                                      @Nonnull final Charset aFallbackCharset,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    return readFromStream (aISP, aFallbackCharset, null, aCustomExceptionHandler);
  }

  /**
   * Read the Json from the passed {@link IInputStreamProvider}. If the Json
   * contains an explicit charset, the whole Json is parsed again, with the
   * charset found inside the file, so the passed {@link IInputStreamProvider}
   * must be able to create a new input stream on second invocation!
   * 
   * @param aISP
   *        The input stream provider to use. Must be able to create new input
   *        streams on every invocation, in case an explicit charset node was
   *        found. May not be <code>null</code>.
   * @param aFallbackCharset
   *        The charset to be used in case neither a <code>@charset</code> rule
   *        nor a BOM is present. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromStream (@Nonnull final IInputStreamProvider aISP,
                                      @Nonnull final Charset aFallbackCharset,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    ValueEnforcer.notNull (aISP, "InputStreamProvider");
    ValueEnforcer.notNull (aFallbackCharset, "FallbackCharset");

    // Open input stream
    final ReadonlyPair <InputStream, Charset> aISAndBOM = _getInputStreamWithoutBOM (aISP);
    if (aISAndBOM == null || aISAndBOM.getFirst () == null)
    {
      // Failed to open stream!
      return null;
    }

    final Charset aCharsetToUse = aISAndBOM.getSecond () != null ? aISAndBOM.getSecond () : aFallbackCharset;

    final InputStream aIS = aISAndBOM.getFirst ();

    try
    {
      final JsonCharStream aCharStream = new JsonCharStream (StreamUtils.createReader (aIS, aCharsetToUse));

      // Use the default Json parse error handler if none is provided
      final IJsonParseErrorHandler aRealErrorHandler = aCustomErrorHandler == null ? getDefaultParseErrorHandler ()
                                                                                  : aCustomErrorHandler;
      // Use the default Json exception handler if none is provided
      final IJsonParseExceptionHandler aRealExceptionHandler = aCustomExceptionHandler == null ? getDefaultParseExceptionHandler ()
                                                                                              : aCustomExceptionHandler;
      final JsonNode aNode = _readStyleSheet (aCharStream, aRealErrorHandler, aRealExceptionHandler);

      // Failed to interpret content as Json?
      if (aNode == null)
        return null;

      // Convert the AST to a domain object
      return JsonHandler.readCascadingStyleSheetFromNode (aNode);
    }
    finally
    {
      StreamUtils.close (aIS);
    }
  }

  /**
   * Read the Json from the passed {@link IReaderProvider}. If the Json contains
   * an explicit <code>@charset</code> rule, it is ignored and the charset used
   * to create the reader is used instead!
   * 
   * @param aRP
   *        The reader provider to use. The reader is retrieved exactly once and
   *        closed anyway. May not be <code>null</code>.
   * @param aCustomErrorHandler
   *        An optional custom error handler that can be used to collect the
   *        recoverable parsing errors. May be <code>null</code>.
   * @param aCustomExceptionHandler
   *        An optional custom exception handler that can be used to collect the
   *        unrecoverable parsing errors. May be <code>null</code>.
   * @return <code>null</code> if reading failed, the Json declarations
   *         otherwise.
   */
  @Nullable
  public static IJson readFromReader (@Nonnull final IReaderProvider aRP,
                                      @Nullable final IJsonParseErrorHandler aCustomErrorHandler,
                                      @Nullable final IJsonParseExceptionHandler aCustomExceptionHandler)
  {
    if (aRP == null)
      throw new NullPointerException ("ReaderProvider");

    // Create the reader
    final Reader aReader = aRP.getReader ();
    if (aReader == null)
    {
      // Failed to open reader
      return null;
    }

    // No charset determination, as the Reader already has an implicit Charset

    try
    {
      final JsonCharStream aCharStream = new JsonCharStream (aReader);
      // Use the default Json parse error handler if none is provided
      final IJsonParseErrorHandler aRealErrorHandler = aCustomErrorHandler == null ? getDefaultParseErrorHandler ()
                                                                                  : aCustomErrorHandler;
      // Use the default Json exception handler if none is provided
      final IJsonParseExceptionHandler aRealExceptionHandler = aCustomExceptionHandler == null ? getDefaultParseExceptionHandler ()
                                                                                              : aCustomExceptionHandler;
      final JsonNode aNode = _readStyleSheet (aCharStream, aRealErrorHandler, aRealExceptionHandler);

      // Failed to interpret content as Json?
      if (aNode == null)
        return null;

      // Convert the AST to a domain object
      return JsonHandler.readCascadingStyleSheetFromNode (aNode);
    }
    finally
    {
      StreamUtils.close (aReader);
    }
  }
}