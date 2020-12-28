package org.apache.spark.example

import org.apache.spark.SparkConf
import org.apache.spark.rpc.{RpcCallContext, RpcEndpoint, RpcEnv}

/**
 * @author pengshaocheng
 */
class HelloEndpoint(override val rpcEnv: RpcEnv) extends RpcEndpoint {
  override def onStart(): Unit = {
    println("start hello endpoint!")
  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case SayHi(msg) =>
      println(s"receive $msg")
      context.reply(s"hi, $msg")
    case SayBye(msg) => {
      println(s"receive $msg")
      context.reply(s"bye, $msg")
    }
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
