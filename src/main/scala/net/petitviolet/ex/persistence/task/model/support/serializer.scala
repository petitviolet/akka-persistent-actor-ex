package net.petitviolet.ex.persistence.task.model.support

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import com.esotericsoftware.kryo
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io._
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model.{ TaskTitle, TaskId, Task }

import scala.language.reflectiveCalls

class RegisterKryoSerializer extends kryo.Serializer[Register] {
  override def write(kryo: Kryo, output: Output, `object`: Register): Unit = kryo.writeObject(output, `object`)

  override def read(kryo: Kryo, input: Input, `type`: Class[Register]): Register = {
    // kryo.readObject(input, `type`)
    Register(Task(TaskId("hogehoge"), TaskTitle("yeeeee")))
  }
}

class KryoSerializerInitializer {
  def customize(kryo: Kryo) = {
    //    println(s"before - ${kryo.getSerializer(classOf[Register])}")
    kryo.addDefaultSerializer(classOf[Register], new RegisterKryoSerializer)
    println(s"after - ${kryo.getSerializer(classOf[Register])}")
  }

  //  def serialize[A](target: A): Array[Byte] = {
  //    using(new ByteArrayOutputStream()) { baos =>
  //      using(new Output(baos)) { output =>
  //        val output = new Output(baos)
  //        println(s"target: $target")
  //        kryo.writeObject(output, target)
  //        output.flush()
  //      }
  //      baos.toByteArray
  //    }
  //  }
  //
  //  def deserialize[A <: AnyRef](bytes: Array[Byte], klass: Class[A]): A = {
  //    using(new ByteArrayInputStream(bytes)) { baos =>
  //      using(new Input(baos)) { input =>
  //        val result = kryo.readObject(input, klass)
  //        println(s"result: $result")
  //        result
  //      }
  //    }
  //  }
  //
  //  private def using[A, R <: { def close() }](r: R)(f: R => A): A =
  //    try {
  //      f(r)
  //    } finally {
  //      r.close()
  //    }
}
