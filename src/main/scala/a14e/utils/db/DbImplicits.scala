package a14e.utils.db

import a14e.bson.decoder._
import a14e.bson._
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import a14e.utils.db.publisher.MongoPublisher
import org.mongodb.scala.{Document, Observable}

import scala.concurrent.Future
import org.mongodb.scala.bson.{BsonDocument, BsonValue}

import scala.util.{Failure, Success}

object DbImplicits {

  implicit class RichFindObservable[T, C](observable: C)
                                         (implicit conv: C <:< Observable[T]) {
    def doOnOption[B](opt: Option[B])
                     (f: B => C => C): C = {
      opt.fold(observable)(v => f(v)(observable))
    }

    def toSource: Source[T, NotUsed] = {
      Source.fromPublisher(new MongoPublisher(observable))
    }
  }

  implicit class RichBsonFlow[T, N](source: Source[T, N]) {
    def as[B](implicit
              decoder: BsonDecoder[B],
              toBson: T <:< BsonValue): Source[B, N] = {
      source.map(toBson(_).as[B])
    }

    def runHeadOption(implicit materializer: Materializer): Future[Option[T]] = {
      source.runWith(Sink.headOption)
    }

    def runSeq(implicit materializer: Materializer): Future[Seq[T]] = {
      source.runWith(Sink.seq)
    }

  }


}
