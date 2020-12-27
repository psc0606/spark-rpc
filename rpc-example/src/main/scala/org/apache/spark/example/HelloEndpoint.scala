package org.apache.spark.example

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
