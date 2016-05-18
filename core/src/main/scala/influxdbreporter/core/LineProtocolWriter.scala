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
package influxdbreporter.core

import java.text.{DecimalFormatSymbols, DecimalFormat}

object LineProtocolWriter extends Writer[String] {

  import LineProtocolTagFieldFormatter.{formatKey, formatValue}

  override def write(measurement: String,
                     fields: List[Field],
                     tags: List[Tag],
                     timestamp: Long): WriterData[String] =
    WriterData {
      s"${formatKey(measurement)}${tagsToString(tags)}${fieldsToString(fields)} $timestamp\n"
    }

  private def tagsToString(tags: List[Tag]): String =
    tags.foldLeft(List.empty[String]) {
      case (acc, tag) => s""",${formatKey(tag.key)}=${formatValue(tag.value)}""" :: acc
    }.mkString

  private def fieldsToString(fields: List[Field]): String =
    fields.foldLeft(List.empty[String]) {
      case (acc, tag) => s"""${formatKey(tag.key)}=${formatValue(tag.value)}""" :: acc
    }.mkString(",") match {
      case str if str.nonEmpty => s" $str"
      case str => str
    }

}

private object LineProtocolTagFieldFormatter {

  private val EscapedCharacters = List(" ", ",")

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

  def formatKey(key: String): String = escape(key)

  def formatValue(value: Any): String = escape {
    value match {
      case v: Double => customDecimalFormat.format(value)
      case v: Float => customDecimalFormat.format(value)
      case v: String => s"""\"$v\""""
      case _ => value.toString
    }
  }

  private def escape(value: String): String =
    EscapedCharacters.foldLeft(value) {
      case (acc, escapedChar) => acc.replaceAll(escapedChar, """\\""" + escapedChar)
    }
}
