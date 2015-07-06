package controllers

import java.net.URL

import models.UUID
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.data._
import play.api.mvc.Request
import models.Dataset
import models.Collection
import org.apache.commons.lang.StringEscapeUtils

object Utils {
  /**
   * Return base url given a request. This will add http or https to the front, for example
   * https://localhost:9443 will be returned if it is using https.
   */
  def baseUrl(request: Request[Any]) = {
    val httpsPort = System.getProperties().getProperty("https.port", "")
    val protocol = if (httpsPort == request.host.split(':').last)  "https" else "http"
    protocol + "://" + request.host
  }

  /**
   * Returns protocol in request stripping it of the : trailing character.
   * @param request
   * @return
   */
  def protocol(request: Request[Any]) = {
    val httpsPort = System.getProperties().getProperty("https.port", "")
    if (httpsPort == request.host.split(':').last)  "https" else "http"
  }

  //TODO UrlFormat2 definition is fine, have not been able to get it to work in Mapping. I think UrlFormat is better choice anyway
  /*
  val urlFormat2 = new Formatter[URL] {
    def bind(key: String, data: Map[String, String]) = {
      stringFormat.bind(key, data).right.flatMap { value =>
        scala.util.control.Exception.allCatch[URL]
          .either(toURL(value))
          .left.map(e => Seq(FormError(key, "error.url", Nil)))
      }
    }
    def unbind(key: String, value: URL) = Map(key -> value.toString)
  }

  def toURL(v:String):URL= {new URL(v)}
  */
  /**
   * Default formatter for the `String` type.
   */
  implicit def stringFormat: Formatter[String] = new Formatter[String] {
    def bind(key: String, data: Map[String, String]) = data.get(key).toRight(Seq(FormError(key, "error.required", Nil)))
    def unbind(key: String, value: String) = Map(key -> value)
  }
  /**
   * Exact copy of private function in play.api.data.format.Formats
   */
  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
    stringFormat.bind(key, data).right.flatMap { s =>
      scala.util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }

  /**
   * Default formatter for the `URL` type.
   */
  object CustomMappings {
    implicit def urlFormat: Formatter[URL] = new Formatter[URL] {
      override val format = Some(("format.url", Nil))
      def bind(key: String, data: Map[String, String]) = parsing(v => new URL(v), "error.url", Nil)(key, data)
      def unbind(key: String, value: URL) = Map(key -> value.toString)
    }
    def urlType: Mapping[URL] = Forms.of[URL]

    implicit def uuidFormat: Formatter[UUID] = new Formatter[UUID] {
      override val format = Some(("format.uuid", Nil))
      def bind(key: String, data: Map[String, String]) = parsing(v => UUID(v), "error.url", Nil)(key, data)
      def unbind(key: String, value: UUID) = Map(key -> value.toString)
    }
    def uuidType: Mapping[UUID] = Forms.of[UUID]
  }

  /**
   * Utility method to modify the elements in a dataset that are encoded when submitted and stored. These elements
   * are decoded when a view requests the objects, so that they can be human readable.
   * 
   * Currently, the following dataset elements are encoded:
   * name
   * description
   *  
   */
  def decodeDatasetElements(dataset: Dataset) : Dataset = {
      val updatedName = updateEncodedTextNewlines(dataset.name)
      val updatedDesc = updateEncodedTextNewlines(dataset.description)
      dataset.copy(name = updatedName, description = updatedDesc)
  }
  
  /**
   * Utility method to modify the elements in a collection that are encoded when submitted and stored. These elements
   * are decoded when a view requests the objects, so that they can be human readable.
   * 
   * Currently, the following collection elements are encoded:
   * 
   * name
   * description
   *  
   */
  def decodeCollectionElements(collection: Collection) : Collection  = {
      val updatedName = updateEncodedTextNewlines(collection.name)
      val updatedDesc = updateEncodedTextNewlines(collection.description)
      collection.copy(name = updatedName, description = updatedDesc)
  }


  /**
   * Encoded text can have newlines. When displayed via a view, they must be translated into linebreaks
   * in order to render correctly.
   *
   * @param text The text to be updated with linebreaks
   * @return An updated String with newlines replaced.
   */
  def updateEncodedTextNewlines(text: String): String = {
    text.replace("\n", "<br>")
  }
}