package net.petitviolet.ex.persistence.task.model.support

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import akka.serialization.Serializer
import com.esotericsoftware.kryo
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer
import com.twitter.chill._
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._

//import com.twitter.chill.KryoInjection

import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls

class RegisterKryoSerializer extends Serializer {
  private val CLAZZ = classOf[Register]

  override def identifier: Int = 1000

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = {
    KryoInjection.apply(o)
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    manifest match {
      case Some(CLAZZ) =>
        KryoInjection.invert(bytes).get
      case _ => sys.error(s"unknown manifest: $manifest")
    }
  }
}

/**
 * should define original serializer for class used at Akka-Persistence.
 */
class iRegisterKryoSerializer extends KryoSerializerBase[Register] {
  override def write(kryo: Kryo, output: Output, `object`: Register): Unit = {
    //    super.write(kryo, output, `object`)
    output.writeString(`object`.task.id.value)
    output.writeString(`object`.task.name.value)
    output.writeInt(`object`.task.state.value)
  }

  override def read(kryo: Kryo, input: Input, `type`: Class[Register]): Register = {
    val taskId = TaskId(input.readString)
    val taskTitle = TaskTitle(input.readString)
    val taskState = TaskState.from(input.readInt)
    Register(Task(taskId, taskTitle, taskState))
  }

}

abstract class KryoSerializerBase[T] extends kryo.Serializer[T](false, false) {

  def write(kryo: Kryo, output: Output, `object`: T): Unit = kryo.writeObject(output, `object`)
  def read(kryo: Kryo, input: Input, `type`: Class[T]): T = kryo.readObject(input, `type`)

  protected def readFromBytes(kryo: Kryo, input: Input): Array[Byte] = {
    val is = input.getInputStream
    val buf = ListBuffer[Byte]()
    var b = is.read()
    while (b != -1) {
      buf.append(b.byteValue)
      b = is.read()
    }
    buf.toArray
  }
}

class CustomKryoSerializerInitializer {
  def customize(kryo: Kryo) = {
    kryo.setDefaultSerializer(classOf[CompatibleFieldSerializer[Any]])
    //    kryo.addDefaultSerializer(classOf[Register], classOf[RegisterKryoSerializer])
    println(s"after - ${kryo.getSerializer(classOf[Register])}")
  }
}
