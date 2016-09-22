package net.petitviolet.ex.persistence.task.model.support

import com.esotericsoftware.kryo
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer
import com.twitter.chill._
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._

//import com.twitter.chill.KryoInjection

import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls

/**
 * should define original serializer for class used at Akka-Persistence.
 */
class RegisterKryoSerializer extends KryoSerializerBase[Register] {
  override def write(kryo: Kryo, output: Output, `object`: Register): Unit = {
    //    super.write(kryo, output, `object`)
    output.writeString(`object`.task.id.value)
    output.writeString(`object`.task.name.value)
    output.writeInt(`object`.task.state.value)
    output.writeBoolean(`object`.flag)
  }

  override def read(kryo: Kryo, input: Input, `type`: Class[Register]): Register = {
    val taskId = TaskId(input.readString)
    val taskTitle = TaskTitle(input.readString)
    val taskState = TaskState.from(input.readInt)
    val flag = input.readBoolean()
    val register = Register(Task(taskId, taskTitle, taskState), flag)
    println(s"recover: $register")
    register
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

class KryoSerializerInitializer {
  def run() = {
    val instantiator = new ScalaKryoInstantiator
    val kryo = instantiator.newKryo()
    customize(kryo)
  }
  def customize(kryo: Kryo) = {
    kryo.setDefaultSerializer(classOf[CompatibleFieldSerializer[Any]])
    kryo.addDefaultSerializer(classOf[Register], classOf[RegisterKryoSerializer])
    println(s"after - ${kryo.getSerializer(classOf[Register])}")
  }
}
