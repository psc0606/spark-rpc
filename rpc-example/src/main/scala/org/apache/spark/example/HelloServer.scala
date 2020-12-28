package org.apache.spark.example

import org.apache.spark.SparkConf
import org.apache.spark.rpc.{RpcAddress, RpcCallContext, RpcEndpoint, RpcEnv}

/**
 * @author pengshaocheng
 */
class HelloEndpoint(override val rpcEnv: RpcEnv) extends RpcEndpoint {
  override def onStart(): Unit = {
    println("start hello endpoint!")
  }

  // receiveAndReply just return a PartialFunction.
  // it will be called by inbox
  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case SayHi(msg) =>
      println(s"receive $msg")
      context.reply(s"hi, $msg")
    case SayBye(msg) =>
      println(s"receive $msg")
      context.reply(s"bye, $msg")
  }

  override def onConnected(remoteAddress: RpcAddress): Unit = {
    println("start connecting with client.")
  }

  override def onDisconnected(remoteAddress: RpcAddress): Unit = {
    println("end connecting with client.")
  }

  override def onError(cause: Throwable): Unit = {
    cause.printStackTrace()
  }

  override def onStop(): Unit = {
    println("stop server...")
  }
}

case class SayHi(msg: String)
case class SayBye(msg: String)

object HelloServer {
  def main(args: Array[String]): Unit = {
    val rpcEnv: RpcEnv = RpcEnv.create("hello-word", "localhost", 3031, new SparkConf(), null)
    val helloEndpoint: RpcEndpoint = new HelloEndpoint(rpcEnv)
    rpcEnv.setupEndpoint("hello-service", helloEndpoint)
    rpcEnv.awaitTermination()
  }
}
