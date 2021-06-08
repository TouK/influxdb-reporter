/*
 * Copyright 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package influxdbreporter.core.writers

import java.text.{DecimalFormat, DecimalFormatSymbols}

import com.typesafe.scalalogging.LazyLogging
import influxdbreporter.core.{Field, Tag}

class LineProtocolWriter(staticTags: List[Tag] = Nil)
  extends BaseWriterWithStaticTags[String](staticTags) with LazyLogging {

  import LineProtocolPartsFormatter._

  override def write(measurement: String,
                     fields: List[Field],
                     tags: Set[Tag],
                     timestamp: Long): WriterData[String] = {
    val allTags = tags ++ staticTags
    val filteredTags = filterTagsWithDuplicatedName(allTags)
    if(allTags.size != filteredTags.size) {
      val diff = allTags.diff(filteredTags)
      logger.warn(s"Duplicated tags [${diff.map(t => s"${t.key}=${t.value.toString}").mkString(",")}] were rejected and not sent to influxdb")
    }
    WriterData {
      s"${formatMeasurementName(measurement)}${tagsToString(filteredTags)}${fieldsToString(fields)} $timestamp\n"
    }
  }

  private def filterTagsWithDuplicatedName(tags: Set[Tag]): Set[Tag] = {
    tags.groupBy(_.key).map(_._2.head).toSet
  }

  private def tagsToString(tags: Set[Tag]): String =
    tags.toList
      .sortBy(_.key)(Ordering[String].reverse)
      .map(tag => (formatTagName(tag.key), formatTagValueIfValueIsCorrect(tag.value)))
      .foldLeft(List.empty[String]) {
        case (acc, (formattedTagKey, Some(formattedTagValue))) => s""",$formattedTagKey=$formattedTagValue""" :: acc
        case (acc, _) => acc
      }.mkString

  private def fieldsToString(fields: List[Field]): String =
    fields.foldLeft(List.empty[String]) {
      case (acc, field) => s"""${formatFieldName(field.key)}=${formatFieldValue(field.value)}""" :: acc
    }.mkString(",") match {
      case str if str.nonEmpty => s" $str"
      case str => str
    }
}

private object LineProtocolPartsFormatter {

  private val MeasurementNameEscapedCharacters = List(" ", ",")
  private val FieldNameEscapedCharacters = List(" ", ",", "=")
  private val FieldValueEscapedCharacters = List("\"")
  private val TagNameEscapedCharacters = List(" ", ",", "=")
  private val TagValueEscapedCharacters = List(" ", ",", "=")

  private val TrueString = "true"
  private val FalseString = "false"

  private val customDecimalFormat = {
    val df = new DecimalFormat()
    df.setGroupingUsed(false)
    df.setMinimumFractionDigits(1)
    df.setMaximumFractionDigits(Integer.MAX_VALUE)
    val decimalFormatSymbols = {
      val dfs = new DecimalFormatSymbols()
      dfs.setDecimalSeparator('.')
      dfs.setGroupingSeparator(0)
      dfs
    }
    df.setDecimalFormatSymbols(decimalFormatSymbols)
    df
  }

  def formatMeasurementName(name: String): String = escape(name, MeasurementNameEscapedCharacters)

  def formatFieldName(name: String): String = escape(name, FieldNameEscapedCharacters)

  def formatTagName(name: String): String = escape(name, TagNameEscapedCharacters)

  def formatTagValueIfValueIsCorrect(value: Any): Option[String] = value match {
    case v: Double => Some(customDecimalFormat.format(value))
    case v: Float => Some(customDecimalFormat.format(value))
    case true => Some(TrueString)
    case false => Some(FalseString)
    case "" => None
    case _ => Some(escape(value.toString, TagValueEscapedCharacters))
  }

  def formatFieldValue(value: Any): String = value match {
    case v: Double => customDecimalFormat.format(value)
    case v: Float => customDecimalFormat.format(value)
    case v: Int => s"${v}i"
    case v: Long => customDecimalFormat.format(value)
    case v: BigDecimal => customDecimalFormat.format(v.doubleValue)
    case true => TrueString
    case false => FalseString
    case v => s"""\"${escape(v.toString, FieldValueEscapedCharacters)}\""""
  }

  private def escape(value: String, toEscape: List[String]): String =
    toEscape.foldLeft(value) {
      case (acc, escapedChar) => acc.replaceAll(escapedChar, """\\""" + escapedChar)
    }
}
