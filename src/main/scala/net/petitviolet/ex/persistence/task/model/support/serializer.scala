package net.petitviolet.ex.persistence.task.model.support

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{ Input, Output }
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer
import com.twitter.chill.KryoInjection
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._

import scala.language.reflectiveCalls

/**
 * serializer for [[Register]] using twitter/chill
 */
class _RegisterKryoSerializer extends akka.serialization.Serializer {
  private val CLAZZ = classOf[Register]

  override def identifier: Int = 1000

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = {
    KryoInjection.apply(o)
    // throw `buffer underflow` exception...
    //    val kryo = new Kryo()
    //    val baos = new ByteArrayOutputStream()
    //    kryo.writeObject(new Output(baos), o)
    //
    //    baos.toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    manifest match {
      case Some(CLAZZ) =>
        KryoInjection.invert(bytes).get
      //        val kryo = new Kryo()
      //        val input = new Input(new ByteArrayInputStream(bytes))
      //        kryo.readObject(input, CLAZZ)
      case _ => sys.error(s"unknown manifest: $manifest")
    }
  }
}

/**
 * should define original serializer for class used at Akka-Persistence.
 */
class RegisterKryoSerializer extends KryoSerializerBase[Register] {
  override def write(kryo: Kryo, output: Output, `object`: Register): Unit = {
    output.writeString(`object`.task.id.value)
    output.writeString(`object`.task.title.value)
    output.writeInt(`object`.task.state.value)
  }

  override def read(kryo: Kryo, input: Input, `type`: Class[Register]): Register = {
    val taskId = TaskId(input.readString)
    val taskTitle = TaskTitle(input.readString)
    val taskState = TaskState.from(input.readInt)
    Register(Task(taskId, taskTitle, taskState))
  }

}

/**
 * proxy constructor for initializing `accesptsNull` and `immutable`
 *
 * @tparam T: type to be persisted
 */
abstract class KryoSerializerBase[T] extends com.esotericsoftware.kryo.Serializer[T](false, true)

class CustomKryoSerializerInitializer {
  def customize(kryo: Kryo) = {
    kryo.setDefaultSerializer(classOf[CompatibleFieldSerializer[Any]])
    kryo.addDefaultSerializer(classOf[Register], classOf[RegisterKryoSerializer])
    println(s"after - ${kryo.getSerializer(classOf[Register])}")
  }
}
