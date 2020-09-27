package a14e.commons.camundadsl

import java.time.Instant
import java.util.{Date, UUID}

import org.camunda.bpm.client.task.ExternalTask
import org.camunda.bpm.client.variable.impl.value.JsonValueImpl
import org.camunda.bpm.engine.variable.VariableMap
import org.camunda.bpm.engine.variable.impl.VariableMapImpl
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl._
import org.camunda.bpm.engine.variable.value.TypedValue
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness, labelled}

trait RootEncoder[T] {
  def encode(x: T): VariableMap
}

object RootEncoder {
  def apply[T: RootEncoder]: RootEncoder[T] = implicitly[RootEncoder[T]]

  implicit def unitRootEncoder: RootEncoder[Unit] = _ => new VariableMapImpl()
}

// todo валидация на null
trait RootDecoder[T] {
  self =>
  def decode(task: ExternalTask): T
}

object RootDecoder {
  implicit def indentityRootDecoder: RootDecoder[ExternalTask] = x => x

  implicit def unitRootDecoder: RootDecoder[Unit] = _ => ()

  def apply[T: RootDecoder]: RootDecoder[T] = implicitly[RootDecoder[T]]


  implicit def nilRootDecoder: RootDecoder[HNil] = _ => HNil

  // формат камунды не поддерживает рекурсию, поэтому тут отдельный тип
  implicit def hlistRootDecoder[Key <: Symbol, Head, Tail <: HList](implicit
                                                                    classFieldKey: Witness.Aux[Key],
                                                                    headDecoder: Lazy[FieldDecoder[Head]],
                                                                    tailDecoder: Lazy[RootDecoder[Tail]]): RootDecoder[FieldType[Key, Head] :: Tail] = {

    val key: String = classFieldKey.value.name
    (task: ExternalTask) => {

      val head = headDecoder.value.decode(key, task)
      val tail = tailDecoder.value.decode(task)
      labelled.field[Key][Head](head) :: tail
    }
  }
}

object Encodings {

  object auto extends AutoDecoders with AutoEncoders {

  }

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
    (task: ExternalTask) => lgen.from(reprWrites.value.decode(task))

}

trait AutoEncoders extends {

  import shapeless.{LabelledGeneric, Witness, _}
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
  implicit lazy val instantEncoderCamud: FieldEncoder[Instant] =
    FieldEncoder[Date].contramap(x => new Date(x.toEpochMilli))
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
        case None => map.putValue(name, null)
        case Some(x) => FieldEncoder[T].encode(name, x, map)
      }
    }
}


trait FieldDecoder[T] {
  self =>

  def map[B](f: T => B): FieldDecoder[B] =
    (name: String, task: ExternalTask) => f(self.decode(name, task))


  def decode(name: String,
             task: ExternalTask): T = {
    val res = docodeImpl(name, task)
    if (res == null && !nullEnabled)
      throw new RuntimeException("null value is not supported here")
    res
  }

  protected def docodeImpl(name: String,
                           task: ExternalTask): T

  protected def nullEnabled = false

  def enableNull: FieldDecoder[T] = new FieldDecoder[T] {
    override def docodeImpl(name: String,
                            task: ExternalTask): T = self.docodeImpl(name, task)

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
  implicit lazy val instantDecoderCamund: FieldDecoder[Instant] = FieldDecoder[Date].map(_.toInstant)
}


trait LowPriorityDecoders {

  import io.circe.{Decoder, parser}

  implicit def jsonDecoder[T: Decoder]: FieldDecoder[T] = { (name: String, task: ExternalTask) =>
    val jsonString = task.getVariable[String](name)
    val resultEither = for {
      json <- parser.parse(jsonString)
      result <- json.as[T]
    } yield result
    resultEither.toTry.get
  }

  implicit def optionDecoder[T: FieldDecoder]: FieldDecoder[Option[T]] = FieldDecoder[T].map(Option(_)).enableNull
}