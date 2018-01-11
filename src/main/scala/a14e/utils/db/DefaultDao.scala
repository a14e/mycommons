package a14e.utils.db

import a14e.utils.concurrent.FutureImplicits._
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonDocument, BsonTransformer}
import org.mongodb.scala.bson.conversions.Bson
import DbImplicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import DBUtils._
import a14e.bson._
import a14e.bson.decoder.BsonDecoder
import a14e.bson.encoder.BsonEncoder
import akka.stream.Materializer
import BsonEncoder._
import BsonDecoder._
import akka.stream.scaladsl.Source

class DefaultDao[T: BsonEncoder : BsonDecoder, ID: BsonEncoder](val database: MongoDatabase,
                                                                val collectionName: String)
                                                               (implicit
                                                                context: ExecutionContext,
                                                                materializer: Materializer) {


  def collection: MongoCollection[BsonDocument] = database.getCollection[BsonDocument](collectionName)

  def findById(id: ID): Future[Option[T]] = {
    collection.find(Bson.obj("_id" -> id)).toSource.as[T].runHeadOption
  }

  def findByIds(ids: Seq[ID]): Future[Seq[T]] = {
    val query = Bson.obj("_id" -> Bson.obj("$in" -> ids))
    listByQuery(query)
  }

  def updateOne(id: ID, obj: T): Future[Unit] = {
    updateByQuery(Bson.obj("_id" -> id), obj)
  }

  def updateOne(query: Bson, obj: Bson): Future[Unit] = {
    collection.updateOne(query, obj).toFuture().toUnit
  }

  def updateById(id: ID, obj: Bson): Future[Unit] = {
    updateOne(Bson.obj("_id" -> id), obj)
  }

  def updateByQuery(query: Bson, obj: T): Future[Unit] = {
    collection.updateOne(query, Bson.obj("$set" -> obj)).toFuture().toUnit
  }

  def deleteOne(id: ID): Future[Unit] = {
    collection.deleteOne(Bson.obj("_id" -> id)).toFuture().toUnit
  }

  def insert(obj: T): Future[Unit] = {
    collection.insertOne(obj.asBson.asDocument()).toFuture().toUnit
  }


  def listByQuerySource(query: Bson,
                        offsetOpt: Option[Int] = None,
                        limitOpt: Option[Int] = None,
                        sortByOpt: Option[Bson] = None): Source[T, _] = {
    collection
      .find(query)
      .doOnOption(sortByOpt)(sortBy => _.sort(sortBy))
      .doOnOption(offsetOpt)(offset => _.skip(offset))
      .doOnOption(limitOpt)(limit => _.limit(limit))
      .toSource
      .as[T]
  }

  def listByQuery(query: Bson,
                  offsetOpt: Option[Int] = None,
                  limitOpt: Option[Int] = None,
                  sortByOpt: Option[Bson] = None): Future[Seq[T]] = {
    listByQuerySource(query,offsetOpt, limitOpt, sortByOpt).runSeq
  }

}


