package a14e.commons.camundadsl

import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}

import a14e.commons.`enum`.{EnumFinder, FindableEnum}
import org.camunda.bpm.client.task.ExternalTask
import org.camunda.bpm.client.variable.impl.value.JsonValueImpl
import org.camunda.bpm.engine.variable.VariableMap
import org.camunda.bpm.engine.variable.impl.VariableMapImpl
import org.camunda.bpm.engine.variable.impl.value.NullValueImpl
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl._
import org.camunda.bpm.engine.variable.value.TypedValue
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness, labelled}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

trait RootEncoder[A] {
  self =>

  def encode(x: A): VariableMap

  def encodeDiffOnly(x: A,
                     oldValues: VariableMap): VariableMap = {
    import scala.jdk.CollectionConverters._

    val newMap = self.encode(x)
    val duplicates = newMap.entrySet().iterator().asScala.map(_.getKey).filter { key =>

      val oldCtx = oldValues.asVariableContext().resolve(key)
      val newCtx = newMap.asVariableContext().resolve(key)
      // чтобы проверить на тип null
      (oldCtx.getType == newCtx.getType) && (oldCtx.getValue == oldCtx.getValue)
    }.to(List) // тут превращаем в лист, чтобы не сломать итератор во время выполнения удаления
    for (key <- duplicates)
      newMap.remove(key)
    newMap
  }

  def contramap[B](f: B => A): RootEncoder[B] = x => self.encode(f(x))

  def zip[B](second: RootEncoder[B]): RootEncoder[(A, B)] = {
    case (a, b) =>
      val result = new VariableMapImpl()
      result.putAll(self.encode(a))
      result.putAll(second.encode(b))
      result
  }
}

object RootEncoder {
  def apply[T: RootEncoder]: RootEncoder[T] = implicitly[RootEncoder[T]]

  // спрятано, так как небезопасно
  object either {
    implicit def eitherRootEncoder[A: RootEncoder, B: RootEncoder]: RootEncoder[Either[A, B]] = {
      case Left(a) => RootEncoder[A].encode(a)
      case Right(b) => RootEncoder[B].encode(b)
    }
  }

  object tuples {
    implicit def tuple2RootEncoder[A: RootEncoder, B: RootEncoder]: RootEncoder[(A, B)] = {
      RootEncoder[A].zip(RootEncoder[B])
    }

    implicit def tuple3RootEncoder[A: RootEncoder, B: RootEncoder, C: RootEncoder]: RootEncoder[(A, B, C)] = {
      case (a, b, c) =>
        val result = new VariableMapImpl()
        result.putAll(RootEncoder[A].encode(a))
        result.putAll(RootEncoder[B].encode(b))
        result.putAll(RootEncoder[C].encode(c))
        result
    }

    implicit def tuple4RootEncoder[A: RootEncoder, B: RootEncoder, C: RootEncoder, D: RootEncoder]: RootEncoder[(A, B, C, D)] = {
      case (a, b, c, d) =>
        val result = new VariableMapImpl()
        result.putAll(RootEncoder[A].encode(a))
        result.putAll(RootEncoder[B].encode(b))
        result.putAll(RootEncoder[C].encode(c))
        result.putAll(RootEncoder[D].encode(d))
        result
    }
  }


  implicit def unitRootEncoder: RootEncoder[Unit] = _ => new VariableMapImpl()

  implicit def varMapEncoder: RootEncoder[VariableMap] = x => x

  import shapeless.Witness
  import shapeless.labelled._

  implicit def nilRootEncoder: RootEncoder[HNil] = _ => new VariableMapImpl()

  // формат камунды не поддерживает рекурсию
  implicit def hlistRootEncoder[Key <: Symbol, Head, Tail <: HList](implicit
                                                                    classFieldKey: Witness.Aux[Key],
                                                                    headEncoder: Lazy[FieldEncoder[Head]],
                                                                    tailEncoder: Lazy[RootEncoder[Tail]]): RootEncoder[FieldType[Key, Head] :: Tail] = {

    val key: String = classFieldKey.value.name
    hlist =>
      val tailMap = tailEncoder.value.encode(hlist.tail)
      headEncoder.value.encode(key, hlist.head, tailMap)
  }
}

trait RootDecoder[A] {
  self =>

  def decode(task: ExternalTask): Try[A]

  def map[B](f: A => B): RootDecoder[B] = task => self.decode(task).map(f)

  def flatMap[B](f: A => RootDecoder[B]): RootDecoder[B] = task => self.decode(task).flatMap(x => f(x).decode(task))

  def zip[B](second: RootDecoder[B]): RootDecoder[(A, B)] = {
    for {
      a <- self
      b <- second
    } yield (a, b)
  }
}

object RootDecoder {
  implicit def indentityRootDecoder: RootDecoder[ExternalTask] = x => Success(x)

  implicit def unitRootDecoder: RootDecoder[Unit] = RootDecoder.pure(())

  def apply[T: RootDecoder]: RootDecoder[T] = implicitly[RootDecoder[T]]

  def pure[T](x: T): RootDecoder[T] = _ => Success(x)

  // спрятано, так как небезопасно
  object tuples {
    implicit def tuple2RootDecoder[A: RootDecoder, B: RootDecoder]: RootDecoder[(A, B)] = {
      RootDecoder[A].zip(RootDecoder[B])
    }

    implicit def tuple3RootDecoder[A: RootDecoder, B: RootDecoder, C: RootDecoder]: RootDecoder[(A, B, C)] = {
      for {
        a <- RootDecoder[A]
        b <- RootDecoder[B]
        c <- RootDecoder[C]
      } yield (a, b, c)
    }

    implicit def tuple4RootDecoder[A: RootDecoder, B: RootDecoder, C: RootDecoder, D: RootDecoder]: RootDecoder[(A, B, C, D)] = {
      for {
        a <- RootDecoder[A]
        b <- RootDecoder[B]
        c <- RootDecoder[C]
        d <- RootDecoder[D]
      } yield (a, b, c, d)
    }
  }

  implicit def nilRootDecoder: RootDecoder[HNil] = RootDecoder.pure(HNil)

  // формат камунды не поддерживает рекурсию, поэтому тут отдельный тип
  implicit def hlistRootDecoder[Key <: Symbol, Head, Tail <: HList](implicit
                                                                    classFieldKey: Witness.Aux[Key],
                                                                    headDecoder: Lazy[FieldDecoder[Head]],
                                                                    tailDecoder: Lazy[RootDecoder[Tail]]): RootDecoder[FieldType[Key, Head] :: Tail] = {

    val key: String = classFieldKey.value.name
    (task: ExternalTask) => {

      for {
        head <- headDecoder.value.decode(key, task)
        tail <- tailDecoder.value.decode(task)
      } yield labelled.field[Key][Head](head) :: tail
    }
  }
}

object Encodings {

  object auto extends AutoDecoders with AutoEncoders

  object semiauto {
    def derivedDecoder[T <: Product with Serializable] = new DummyApplyDecoderWrapper[T]

    class DummyApplyDecoderWrapper[T <: Product with Serializable] {
      def apply[Repr]()(implicit
                        lgen: LabelledGeneric.Aux[T, Repr],
                        reprWrites: Lazy[RootDecoder[Repr]]): RootDecoder[T] = auto.caseClassDecoder[T, Repr]
    }

    def derivedEncoder[T <: Product with Serializable] = new DummyApplyEncoderWrapper[T]

    class DummyApplyEncoderWrapper[T <: Product with Serializable] {
      def apply[Repr]()(implicit
                        lgen: LabelledGeneric.Aux[T, Repr],
                        reprWrites: Lazy[RootEncoder[Repr]]): RootEncoder[T] = auto.caseClassEncoder[T, Repr]
    }

  }

}

trait AutoDecoders extends {

  import shapeless.{LabelledGeneric, _}


  implicit def caseClassDecoder[T <: Product with Serializable, Repr](implicit
                                                                      lgen: LabelledGeneric.Aux[T, Repr],
                                                                      reprWrites: Lazy[RootDecoder[Repr]]): RootDecoder[T] =
    (task: ExternalTask) => reprWrites.value.decode(task).map(lgen.from)

}

trait AutoEncoders extends {


  implicit def caseClassEncoder[T <: Product with Serializable, Repr](implicit
                                                                      lgen: LabelledGeneric.Aux[T, Repr],
                                                                      reprWrites: Lazy[RootEncoder[Repr]]): RootEncoder[T] =
    (obj: T) => reprWrites.value.encode(lgen.to(obj))

}


trait FieldEncoder[T] {
  self =>

  def encode(name: String,
             x: T,
             map: VariableMap): VariableMap

  def contramap[B](f: B => T): FieldEncoder[B] =
    (name: String, x: B, map: VariableMap) => self.encode(name, f(x), map)

}

object FieldEncoder extends LowPriorityEncoders {

  def apply[T: FieldEncoder]: FieldEncoder[T] = implicitly[FieldEncoder[T]]

  def forTyped[T](toTyped: T => TypedValue): FieldEncoder[T] =
    (name: String, x: T, map: VariableMap) => map.putValueTyped(name, toTyped(x))


  implicit lazy val stringEncoderCamund: FieldEncoder[String] = forTyped(new StringValueImpl(_))
  implicit lazy val numberEncoderCamund: FieldEncoder[Number] = forTyped(new NumberValueImpl(_))
  implicit lazy val shortEncoderCamund: FieldEncoder[Short] = forTyped(new ShortValueImpl(_))
  implicit lazy val longEncoderCamund: FieldEncoder[Long] = forTyped(new LongValueImpl(_))
  implicit lazy val intEncoderCamund: FieldEncoder[Int] = forTyped(new IntegerValueImpl(_))
  implicit lazy val doubleEncoderCamund: FieldEncoder[Double] = forTyped(new DoubleValueImpl(_))
  implicit lazy val dateEncoderCamund: FieldEncoder[Date] = forTyped(new DateValueImpl(_))
  implicit lazy val bytesEncoderCamund: FieldEncoder[Array[Byte]] = forTyped(new BytesValueImpl(_))
  implicit lazy val boolEncoderCamund: FieldEncoder[Boolean] = forTyped(new BooleanValueImpl(_))

  implicit lazy val uuidEncoderCamud: FieldEncoder[UUID] = FieldEncoder[String].contramap(_.toString)
  implicit lazy val javaDurationEncoderCamud: FieldEncoder[java.time.Duration] = {
    FieldEncoder[String].contramap(_.toString)
  }
  implicit lazy val scalaDurationEncoderCamud: FieldEncoder[FiniteDuration] = {
    FieldEncoder[java.time.Duration].contramap(d => java.time.Duration.ofMillis(d.toMillis))
  }
  implicit lazy val instantEncoderCamud: FieldEncoder[Instant] = {
    FieldEncoder[Date].contramap(x => new Date(x.toEpochMilli))
  }
  implicit lazy val offsetDateTimeEncoderCamud: FieldEncoder[OffsetDateTime] = {
    FieldEncoder[Instant].contramap(_.toInstant)
  }

  implicit def enumEncoderCamund[VALUE <: Enumeration#Value]: FieldEncoder[VALUE] = {
    FieldEncoder[String].contramap(_.toString)
  }
}


trait LowPriorityEncoders {


  import io.circe.Encoder

  implicit def jsonEncoder[T: Encoder]: FieldEncoder[T] = (name: String, x: T, map: VariableMap) => {
    val jsonString = Encoder[T].apply(x).noSpaces
    val jsonValue = new JsonValueImpl(jsonString)
    map.putValueTyped(name, jsonValue)
  }

  implicit def optionEncoder[T: FieldEncoder]: FieldEncoder[Option[T]] =
    (name: String, valueOpt: Option[T], map: VariableMap) => {
      valueOpt match {
        case None => map.putValueTyped(name, NullValueImpl.INSTANCE)
        case Some(x) => FieldEncoder[T].encode(name, x, map)
      }
    }
}


trait FieldDecoder[T] {
  self =>

  def map[B](f: T => B): FieldDecoder[B] =
    (name: String, task: ExternalTask) => self.docodeImpl(name, task).map(f)


  def decode(name: String,
             task: ExternalTask): Try[T] = {
    val res = docodeImpl(name, task)
    if (res == null && !nullEnabled)
      Failure(new RuntimeException("null value is not supported here"))
    else res
  }

  protected def docodeImpl(name: String,
                           task: ExternalTask): Try[T]

  protected def nullEnabled = false

  def enableNull: FieldDecoder[T] = new FieldDecoder[T] {
    override def docodeImpl(name: String,
                            task: ExternalTask): Try[T] = self.docodeImpl(name, task)

    protected override def nullEnabled = true
  }


}

object FieldDecoder extends LowPriorityDecoders {

  def apply[T: FieldDecoder]: FieldDecoder[T] = implicitly[FieldDecoder[T]]

  implicit lazy val stringDecoderCamund: FieldDecoder[String] = (name, task) => task.getVariable(name)
  implicit lazy val numberDecoderCamund: FieldDecoder[Number] = (name, task) => task.getVariable(name)
  implicit lazy val shortDecoderCamund: FieldDecoder[Short] = (name, task) => task.getVariable(name)
  implicit lazy val longDecoderCamund: FieldDecoder[Long] = (name, task) => task.getVariable(name)
  implicit lazy val intDecoderCamund: FieldDecoder[Int] = (name, task) => task.getVariable(name)
  implicit lazy val doubleDecoderCamund: FieldDecoder[Double] = (name, task) => task.getVariable(name)
  implicit lazy val dateDecoderCamund: FieldDecoder[Date] = (name, task) => task.getVariable(name)
  implicit lazy val bytesDecoderCamund: FieldDecoder[Array[Byte]] = (name, task) => task.getVariable(name)
  implicit lazy val boolDecoderCamund: FieldDecoder[Boolean] = (name, task) => task.getVariable(name)


  implicit lazy val uuidDecoderCamund: FieldDecoder[UUID] = FieldDecoder[String].map(UUID.fromString)
  implicit lazy val javaDurationDecoderCamund: FieldDecoder[java.time.Duration] = {
    FieldDecoder[String].map(java.time.Duration.parse)
  }
  implicit lazy val scalaDurationDecoderCamund: FieldDecoder[FiniteDuration] = {
    FieldDecoder[java.time.Duration].map(d => FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS))
  }
  implicit lazy val instantDecoderCamund: FieldDecoder[Instant] = FieldDecoder[Date].map(_.toInstant)
  implicit lazy val offsetDateTimeDecoderCamund: FieldDecoder[OffsetDateTime] = {
    FieldDecoder[Instant].map(instant => OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()))
  }

  implicit def finableEnumDecoderCamund[E <: Enumeration : EnumFinder]: FieldDecoder[E#Value] = {
    FieldDecoder[String].map(EnumFinder[E].find.withName)
  }
}


trait LowPriorityDecoders {

  import io.circe.{Decoder, parser}

  implicit def jsonDecoder[T: Decoder]: FieldDecoder[T] = { (name: String, task: ExternalTask) =>
    val jsonString = task.getVariable[String](name)
    (for {
      json <- parser.parse(jsonString)
      result <- json.as[T]
    } yield result).toTry
  }

  implicit def optionDecoder[T: FieldDecoder]: FieldDecoder[Option[T]] = FieldDecoder[T].map(Option(_)).enableNull
}