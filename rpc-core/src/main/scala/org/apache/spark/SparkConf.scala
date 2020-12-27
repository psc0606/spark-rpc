/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.collection.mutable.LinkedHashSet
import scala.concurrent.duration._

import org.apache.avro.{Schema, SchemaNormalization}

import org.apache.spark.internal.Logging
//import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.util.Utils

/**
 * Configuration for a Spark application. Used to set various Spark parameters as key-value pairs.
 *
 * Most of the time, you would create a SparkConf object with `new SparkConf()`, which will load
 * values from any `spark.*` Java system properties set in your application as well. In this case,
 * parameters you set directly on the `SparkConf` object take priority over system properties.
 *
 * For unit tests, you can also call `new SparkConf(false)` to skip loading external settings and
 * get the same configuration no matter what the system properties are.
 *
 * All setter methods in this class support chaining. For example, you can write
 * `new SparkConf().setMaster("local").setAppName("My app")`.
 *
 * @param loadDefaults whether to also load values from Java system properties
 *
 * @note Once a SparkConf object is passed to Spark, it is cloned and can no longer be modified
 * by the user. Spark does not support modifying the configuration at runtime.
 */
class SparkConf(loadDefaults: Boolean) extends Cloneable with Logging with Serializable {

  /** Create a SparkConf that loads defaults from system properties and the classpath */
  def this() = this(true)

  private val settings = new ConcurrentHashMap[String, String]()

  if (loadDefaults) {
    loadFromSystemProperties(false)
  }

  private[spark] def loadFromSystemProperties(silent: Boolean): SparkConf = {
    // Load any spark.* system properties
    for ((key, value) <- Utils.getSystemProperties if key.startsWith("spark.")) {
      set(key, value, silent)
    }
    this
  }

  /** Set a configuration variable. */
  def set(key: String, value: String): SparkConf = {
    set(key, value, false)
  }

  private[spark] def set(key: String, value: String, silent: Boolean): SparkConf = {
    if (key == null) {
      throw new NullPointerException("null key")
    }
    if (value == null) {
      throw new NullPointerException("null value for " + key)
    }
    if (!silent) {
      // logDeprecationWarning(key)
    }
    settings.put(key, value)
    this
  }

  /** Set multiple parameters together */
  def setAll(settings: Traversable[(String, String)]): SparkConf = {
    settings.foreach { case (k, v) => set(k, v) }
    this
  }

  /** Set a parameter if it isn't already configured */
  def setIfMissing(key: String, value: String): SparkConf = {
    if (settings.putIfAbsent(key, value) == null) {
      // logDeprecationWarning(key)
    }
    this
  }

  /**
   * Use Kryo serialization and register the given set of classes with Kryo.
   * If called multiple times, this will append the classes from all calls together.
   */
//  def registerKryoClasses(classes: Array[Class[_]]): SparkConf = {
//    val allClassNames = new LinkedHashSet[String]()
//    allClassNames ++= get("spark.kryo.classesToRegister", "").split(',').map(_.trim)
//      .filter(!_.isEmpty)
//    allClassNames ++= classes.map(_.getName)
//
//    set("spark.kryo.classesToRegister", allClassNames.mkString(","))
//    set("spark.serializer", classOf[KryoSerializer].getName)
//    this
//  }

  private final val avroNamespace = "avro.schema."

  /**
   * Use Kryo serialization and register the given set of Avro schemas so that the generic
   * record serializer can decrease network IO
   */
  def registerAvroSchemas(schemas: Schema*): SparkConf = {
    for (schema <- schemas) {
      set(avroNamespace + SchemaNormalization.parsingFingerprint64(schema), schema.toString)
    }
    this
  }

  /** Gets all the avro schemas in the configuration used in the generic Avro record serializer */
  def getAvroSchema: Map[Long, String] = {
    getAll.filter { case (k, v) => k.startsWith(avroNamespace) }
      .map { case (k, v) => (k.substring(avroNamespace.length).toLong, v) }
      .toMap
  }

  /** Remove a parameter from the configuration */
  def remove(key: String): SparkConf = {
    settings.remove(key)
    this
  }

  /** Get a parameter; throws a NoSuchElementException if it's not set */
  def get(key: String): String = {
    getOption(key).getOrElse(throw new NoSuchElementException(key))
  }

  /** Get a parameter, falling back to a default if not set */
  def get(key: String, defaultValue: String): String = {
    getOption(key).getOrElse(defaultValue)
  }

  /**
   * Get a time parameter as seconds; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then seconds are assumed.
   * @throws java.util.NoSuchElementException If the time parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as seconds
   */
  def getTimeAsSeconds(key: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsSeconds(get(key))
  }

  /**
   * Get a time parameter as seconds, falling back to a default if not set. If no
   * suffix is provided then seconds are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as seconds
   */
  def getTimeAsSeconds(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsSeconds(get(key, defaultValue))
  }

  /**
   * Get a time parameter as milliseconds; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then milliseconds are assumed.
   * @throws java.util.NoSuchElementException If the time parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as milliseconds
   */
  def getTimeAsMs(key: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsMs(get(key))
  }

  /**
   * Get a time parameter as milliseconds, falling back to a default if not set. If no
   * suffix is provided then milliseconds are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as milliseconds
   */
  def getTimeAsMs(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsMs(get(key, defaultValue))
  }

  /**
   * Get a size parameter as bytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then bytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as bytes
   */
  def getSizeAsBytes(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsBytes(get(key))
  }

  /**
   * Get a size parameter as bytes, falling back to a default if not set. If no
   * suffix is provided then bytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as bytes
   */
  def getSizeAsBytes(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsBytes(get(key, defaultValue))
  }

  /**
   * Get a size parameter as bytes, falling back to a default if not set.
   * @throws NumberFormatException If the value cannot be interpreted as bytes
   */
  def getSizeAsBytes(key: String, defaultValue: Long): Long = catchIllegalValue(key) {
    Utils.byteStringAsBytes(get(key, defaultValue + "B"))
  }

  /**
   * Get a size parameter as Kibibytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then Kibibytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as Kibibytes
   */
  def getSizeAsKb(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsKb(get(key))
  }

  /**
   * Get a size parameter as Kibibytes, falling back to a default if not set. If no
   * suffix is provided then Kibibytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as Kibibytes
   */
  def getSizeAsKb(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsKb(get(key, defaultValue))
  }

  /**
   * Get a size parameter as Mebibytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then Mebibytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as Mebibytes
   */
  def getSizeAsMb(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsMb(get(key))
  }

  /**
   * Get a size parameter as Mebibytes, falling back to a default if not set. If no
   * suffix is provided then Mebibytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as Mebibytes
   */
  def getSizeAsMb(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsMb(get(key, defaultValue))
  }

  /**
   * Get a size parameter as Gibibytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then Gibibytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as Gibibytes
   */
  def getSizeAsGb(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsGb(get(key))
  }

  /**
   * Get a size parameter as Gibibytes, falling back to a default if not set. If no
   * suffix is provided then Gibibytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as Gibibytes
   */
  def getSizeAsGb(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsGb(get(key, defaultValue))
  }

  /** Get a parameter as an Option */
  def getOption(key: String): Option[String] = {
    Option(settings.get(key)).orElse(None)
  }

  /** Get all parameters as a list of pairs */
  def getAll: Array[(String, String)] = {
    settings.entrySet().asScala.map(x => (x.getKey, x.getValue)).toArray
  }

  /**
   * Get all parameters that start with `prefix`
   */
  def getAllWithPrefix(prefix: String): Array[(String, String)] = {
    getAll.filter { case (k, v) => k.startsWith(prefix) }
      .map { case (k, v) => (k.substring(prefix.length), v) }
  }

  /**
   * Get a parameter as an integer, falling back to a default if not set
   * @throws NumberFormatException If the value cannot be interpreted as an integer
   */
  def getInt(key: String, defaultValue: Int): Int = catchIllegalValue(key) {
    getOption(key).map(_.toInt).getOrElse(defaultValue)
  }

  /**
   * Get a parameter as a long, falling back to a default if not set
   * @throws NumberFormatException If the value cannot be interpreted as a long
   */
  def getLong(key: String, defaultValue: Long): Long = catchIllegalValue(key) {
    getOption(key).map(_.toLong).getOrElse(defaultValue)
  }

  /**
   * Get a parameter as a double, falling back to a default if not ste
   * @throws NumberFormatException If the value cannot be interpreted as a double
   */
  def getDouble(key: String, defaultValue: Double): Double = catchIllegalValue(key) {
    getOption(key).map(_.toDouble).getOrElse(defaultValue)
  }

  /**
   * Get a parameter as a boolean, falling back to a default if not set
   * @throws IllegalArgumentException If the value cannot be interpreted as a boolean
   */
  def getBoolean(key: String, defaultValue: Boolean): Boolean = catchIllegalValue(key) {
    getOption(key).map(_.toBoolean).getOrElse(defaultValue)
  }

  /** Copy this object */
  override def clone: SparkConf = {
    val cloned = new SparkConf(false)
    settings.entrySet().asScala.foreach { e =>
      cloned.set(e.getKey(), e.getValue(), true)
    }
    cloned
  }

  /**
   * By using this instead of System.getenv(), environment variables can be mocked
   * in unit tests.
   */
  private[spark] def getenv(name: String): String = System.getenv(name)

  /**
   * Wrapper method for get() methods which require some specific value format. This catches
   * any [[NumberFormatException]] or [[IllegalArgumentException]] and re-raises it with the
   * incorrectly configured key in the exception message.
   */
  private def catchIllegalValue[T](key: String)(getValue: => T): T = {
    try {
      getValue
    } catch {
      case e: NumberFormatException =>
        // NumberFormatException doesn't have a constructor that takes a cause for some reason.
        throw new NumberFormatException(s"Illegal value for config key $key: ${e.getMessage}")
            .initCause(e)
      case e: IllegalArgumentException =>
        throw new IllegalArgumentException(s"Illegal value for config key $key: ${e.getMessage}", e)
    }
  }
}
