package influxdbreporter.core

case class Tag(key: String, value: Any) extends Ordered[Tag] {
  override def compare(that: Tag): Int = this.key.compareTo(that.key)
}