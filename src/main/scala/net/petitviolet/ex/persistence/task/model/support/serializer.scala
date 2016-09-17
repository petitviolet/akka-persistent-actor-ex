package net.petitviolet.ex.persistence.task.model.support

import java.io.{ InputStream, ByteArrayOutputStream }

import com.esotericsoftware.kryo
import com.esotericsoftware.kryo.{ io, Kryo }
import com.esotericsoftware.kryo.io._
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer
import com.twitter.chill.KryoInjection
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._

//import com.twitter.chill.KryoInjection

import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls

class RegisterKryoSerializer extends KryoSerializerBase[Register] {
  //  override def write(kryo: Kryo, output: Output, `object`: Register): Unit = kryo.writeObject(output, `object`) //defaultWrite(kryo, output, `object`)

  def read(kryo: Kryo, input: Input, `type`: Class[Register]): Register = {
    val bytes = input.readBytes(4096)
    //    val bytes = readFromBytes(kryo, input)
    println(s"input => " + new String(bytes))
    Register(Task(TaskId("hogehoge"), TaskTitle("yeeeee")))
    //    defaultRead(kryo, input, `type`)
    //    val result = KryoInjection.invert(input.getBuffer) //.readObjectOrNull(input, `type`)
    //    println(s"result: $result")
    //    result
  }

}

abstract class KryoSerializerBase[T] extends kryo.Serializer[T](false, false) {

  def write(kryo: Kryo, output: Output, `object`: T): Unit = kryo.writeObject(output, `object`)
  def defaultRead(kryo: Kryo, input: Input, `type`: Class[T]): T = kryo.readObject(input, `type`)

  protected def readFromBytes(kryo: Kryo, input: Input): Array[Byte] = {
    val r = input.readBytes(9999)
    input.setInputStream(new ByteBufferInputStream())
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
  def customize(kryo: Kryo) = {
    kryo.setDefaultSerializer(classOf[CompatibleFieldSerializer[Any]])
    kryo.addDefaultSerializer(classOf[Register], classOf[RegisterKryoSerializer])
    println(s"after - ${kryo.getSerializer(classOf[Register])}")
  }
}
