package net.petitviolet.ex.persistence.task.model.support

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import akka.serialization._
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io._
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

//class TaskSerializer extends BaseSerializer[Task]
//class TaskStateSerializer extends BaseSerializer[TaskState]

class RegisterSerializer extends BaseSerializer[Register]
//class CompleteSerializer extends BaseSerializer[Complete]
//class UndoSerializer extends BaseSerializer[Undo]
//class ArchiveSerializer extends BaseSerializer[Archive]

abstract class BaseSerializer[T <: AnyRef](implicit classTag: ClassTag[T]) extends SerializerWithStringManifest {
  private val TASK_MANIFEST = classTag.runtimeClass.getName
  private val klass = classTag.runtimeClass.getClass
  println(s"klass: $classTag, manifest: $TASK_MANIFEST")

  /**
   * Completely unique value to identify this implementation of Serializer, used to optimize network traffic.
   * Values from 0 to 16 are reserved for Akka internal usage.
   */
  override def identifier: Int = 1000

  /**
   * Return the manifest (type hint) that will be provided in the fromBinary method.
   * Use `""` if manifest is not needed.
   */
  override def manifest(o: AnyRef): String = o.getClass.getName

  /**
   * Produces an object from an array of bytes, with an optional type-hint;
   * the class should be loaded using ActorSystem.dynamicAccess.
   */
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case TASK_MANIFEST => MyKryoSerializer.deserialize(bytes, klass)
      case _             => sys.error(s"unknown: $manifest, prepared => $TASK_MANIFEST")
    }
  }

  /**
   * Serializes the given object into an Array of Byte
   */
  override def toBinary(o: AnyRef): Array[Byte] = MyKryoSerializer.serialize(o)
}

object MyKryoSerializer {
  private def kryo = new Kryo()

  def serialize[A](target: A): Array[Byte] = {
    using(new ByteArrayOutputStream()) { baos =>
      //      using(new Output(baos)) { output =>
      val output = new Output(baos)
      println(s"target: $target")
      kryo.writeObject(output, target)
      output.flush()
      baos.toByteArray
      //      }
    }
  }

  def deserialize[A <: AnyRef](bytes: Array[Byte], klass: Class[A]): A = {
    using(new ByteArrayInputStream(bytes)) { baos =>
      using(new Input(baos)) { input =>
        val result = kryo.readObject(input, klass)
        println(s"result: $result")
        result
      }
    }
  }

  private def using[A, R <: { def close() }](r: R)(f: R => A): A =
    try {
      f(r)
    } finally {
      r.close()
    }
}
