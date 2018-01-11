package a14e.utils.db

import java.time.Instant
import java.util.Date

import akka.NotUsed
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.{BsonTransformer, BsonValue}
import org.mongodb.scala.{FindObservable, MongoCollection, Observable}
import a14e.utils.concurrent.FutureImplicits._
import org.bson.BsonDocument

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object DBUtils {

  object IndexType {
    final val Descending = -1
    final val Ascending = 1
  }

  //  def simpleIndex(keys: (String, IndexType)*): Document = Index(keys)

  // удаляет ненужные индексы
  // и добавляет те индексы, которые недостают)
  def createIndexes[T](needToBeInDBIndexes: Seq[BsonDocument],
                       collection: MongoCollection[T])
                      (implicit context: ExecutionContext): Future[Unit] = {
    Future.serially(needToBeInDBIndexes) { d =>
      collection.createIndex(d).toFuture()
    }.toUnit

  }


}
