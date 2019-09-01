package model

case class Picture(width: Int, height: Int, url: Option[String])

trait Identifiable {
  def id: String
}