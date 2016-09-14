package util

import models._
import play.api.Logger
import play.api.libs.json._
import services._

import java.util.Date
import scala.collection.immutable.List


object SearchUtils {
  lazy val files: FileService = DI.injector.getInstance(classOf[FileService])
  lazy val datasets: DatasetService = DI.injector.getInstance(classOf[DatasetService])
  lazy val collections: CollectionService = DI.injector.getInstance(classOf[CollectionService])
  lazy val metadatas: MetadataService = DI.injector.getInstance(classOf[MetadataService])
  lazy val comments: CommentService = DI.injector.getInstance(classOf[CommentService])

  /**Convert File to ElasticsearchObject and return, fetching metadata as necessary**/
  def getElasticsearchObject(f: File): Option[ElasticsearchObject] = {
    val id = f.id

    // Get child_of relationships for File
    val child_of = datasets.findByFileId(id).map( ds => ds.id.toString )

    // Get comments for file
    val fcomments = for (c <- comments.findCommentsByFileId(id)) yield {
      Comment.toElasticsearchComment(c)
    }

    // Get metadata for File
    var metadata = Map[String, JsValue]()
    for (md <- metadatas.getMetadataByAttachTo(ResourceRef(ResourceRef.file, id))) {
      val creator = md.creator.displayName

      // If USER metadata, ignore the name and set the Metadata Definition field to the creator
      if (md.creator.typeOfAgent=="cat:user") {
        val subjson = md.content.as[JsObject]
        subjson.keys.foreach(subkey => {
          // If we already have some metadata from this creator, merge the results; otherwise, create new entry
          if (metadata.keySet.exists(_ == subkey)) {
            metadata += (subkey -> metadata(subkey).as[JsArray].append((subjson \ subkey)))
          }
          else {
            metadata += (subkey -> Json.arr((subjson \ subkey)))
          }
        })
      } else if (md.creator.typeOfAgent=="user") {
        // Override the creator if this is non-UI user-submitted metadata and group the objects together
        val creator = "user-submitted"
        if (metadata.keySet.exists(_ == creator))
          metadata += (creator -> (metadata(creator).as[JsObject] ++ (md.content.as[JsObject])))
        else
          metadata += (creator -> md.content.as[JsObject])
      }
      else {
        // If we already have some metadata from this creator, merge the results; otherwise, create new entry
        if (metadata.keySet.exists(_ == creator))
          metadata += (creator -> (metadata(creator).as[JsObject] ++ (md.content.as[JsObject])))
        else
          metadata += (creator -> md.content.as[JsObject])
      }
    }

    Some(new ElasticsearchObject(
      ResourceRef('file, id),
      f.filename,
      f.author.id.toString,
      f.uploadDate,
      List.empty,
      child_of,
      f.description,
      f.tags.map( (t:Tag) => Tag.toElasticsearchTag(t) ),
      fcomments,
      metadata
    ))
  }

  /**Convert Dataset to ElasticsearchObject and return, fetching metadata as necessary**/
  def getElasticsearchObject(ds: Dataset): Option[ElasticsearchObject] = {
    val id = ds.id

    // Get comments for dataset
    val dscomments = for (c <- comments.findCommentsByDatasetId(id)) yield {
      Comment.toElasticsearchComment(c)
    }

    // Get metadata for Dataset
    var metadata = Map[String, JsValue]()
    for (md <- metadatas.getMetadataByAttachTo(ResourceRef(ResourceRef.dataset, id))) {

      val creator = md.creator.displayName

      // If USER metadata, ignore the name and set the Metadata Definition field to the creator
      if (md.creator.typeOfAgent=="cat:user") {
        val subjson = md.content.as[JsObject]
        subjson.keys.foreach(subkey => {
          // If we already have some metadata from this creator, merge the results; otherwise, create new entry
          if (metadata.keySet.exists(_ == subkey)) {
            metadata += (subkey -> metadata(subkey).as[JsArray].append((subjson \ subkey)))
          }
          else {
            metadata += (subkey -> Json.arr((subjson \ subkey)))
          }
        })
      } else {
        // If we already have some metadata from this creator, merge the results; otherwise, create new entry
        if (metadata.keySet.exists(_ == creator))
          metadata += (creator -> (metadata(creator).as[JsObject] ++ (md.content.as[JsObject])))
        else
          metadata += (creator -> md.content.as[JsObject])
      }
    }

    Some(new ElasticsearchObject(
      ResourceRef('dataset, id),
      ds.name,
      ds.author.id.toString,
      ds.created,
      ds.files.map(fileId => fileId.toString),
      ds.collections.map(collId => collId.toString),
      ds.description,
      ds.tags.map( (t:Tag) => Tag.toElasticsearchTag(t) ),
      dscomments,
      metadata
    ))
  }

  /**Convert Collection to ElasticsearchObject and return, fetching metadata as necessary**/
  def getElasticsearchObject(c: Collection): Option[ElasticsearchObject] = {
    // Get parent_of relationships for Collection
    var parent_of = datasets.listCollection(c.id.toString).map( ds => ds.id.toString )
    parent_of = parent_of ++ c.parent_collection_ids.map( pc_id => pc_id.toString)

    Some(new ElasticsearchObject(
      ResourceRef('collection, c.id),
      c.name,
      c.author.id.toString,
      c.created,
      parent_of,
      c.child_collection_ids.map( cc_id => cc_id.toString),
      c.description,
      List.empty,
      List.empty,
      Map()
    ))
  }

  /**Convert Section to ElasticsearchObject and return**/
  def getElasticsearchObject(s: Section): Option[ElasticsearchObject] = {
    val id = s.id

    // For Section, child_of will be a one-item list containing parent file ID
    val child_of = List(s.id.toString)

    // Get metadata for Section
    var metadata = Map[String, JsValue]()
    for (md <- metadatas.getMetadataByAttachTo(ResourceRef(ResourceRef.section, id))) {
      val creator = md.creator.displayName

      // If USER metadata, ignore the name and set the Metadata Definition field to the creator
      if (md.creator.typeOfAgent=="cat:user") {
        val subjson = md.content.as[JsObject]
        subjson.keys.foreach(subkey => {
          // If we already have some metadata from this creator, merge the results; otherwise, create new entry
          if (metadata.keySet.exists(_ == subkey)) {
            metadata += (subkey -> metadata(subkey).as[JsArray].append((subjson \ subkey)))
          }
          else {
            metadata += (subkey -> Json.arr((subjson \ subkey)))
          }
        })
      } else if (md.creator.typeOfAgent=="user") {
        // Override the creator if this is non-UI user-submitted metadata and group the objects together
        val creator = "user-submitted"
        if (metadata.keySet.exists(_ == creator))
          metadata += (creator -> (metadata(creator).as[JsObject] ++ (md.content.as[JsObject])))
        else
          metadata += (creator -> md.content.as[JsObject])
      }
      else {
        // If we already have some metadata from this creator, merge the results; otherwise, create new entry
        if (metadata.keySet.exists(_ == creator))
          metadata += (creator -> (metadata(creator).as[JsObject] ++ (md.content.as[JsObject])))
        else
          metadata += (creator -> md.content.as[JsObject])
      }
    }

    Some(new ElasticsearchObject(
      ResourceRef('section, id),
      "section-"+id.toString,
      "",
      new Date,
      List.empty,
      child_of,
      s.description.getOrElse(""),
      s.tags.map( (t:Tag) => Tag.toElasticsearchTag(t) ),
      List.empty,
      metadata
    ))
  }

  /**Convert TempFile to ElasticsearchObject and return, fetching metadata as necessary**/
  def getElasticsearchObject(file: TempFile): Option[ElasticsearchObject] = {
    Some(new ElasticsearchObject(
      ResourceRef('file, file.id),
      file.filename,
      "",
      file.uploadDate,
      List.empty,
      List.empty,
      "",
      List.empty,
      List.empty,
      Map()
    ))
  }
}
