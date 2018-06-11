package twitter.shared

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.util.Try

object DateUtils {
  def parseDate(value: String): Try[Date] = {
    for {
      day <- Try(value.substring(0, 2).toInt)
      month <- Try(value.substring(3, 5).toInt)
      year <- Try(value.substring(6).toInt)
    } yield new Date(year, month - 1, day)
  }

  def showDate(date: Date): String = {
    val day = date.getDate()
    val month = date.getMonth() + 1
    val year = date.getFullYear()
    f"$day%02d-$month%02d-$year"
  }

  def min(a: Date, b: Date): Date = if (a.getTime() < b.getTime()) a else b

  def max(a: Date, b: Date): Date = if (a.getTime() > b.getTime()) a else b

  def nowWithoutTime(): Date = {
    val date = new Date()
    date.setHours(0, 0, 0, 0)
    date
  }

  val i18n = js.Dynamic.literal(
    cancel = "Anuluj",
    clear = "Wyczyść",
    months = js.Array("Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec", "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"),
    monthsShort = js.Array("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru"),
    weekdays = js.Array("Niedziela", "Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota"),
    weekdaysShort = js.Array("Nie", "Pon", "Wto", "Śro", "Czw", "Pią", "Sob"),
    weekdaysAbbrev = js.Array("N", "Pn", "W", "Ś", "Cz", "Pt", "S")
  )
}
