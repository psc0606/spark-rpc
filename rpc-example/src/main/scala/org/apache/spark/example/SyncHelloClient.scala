package org.apache.spark.example

import org.apache.spark.SparkConf
import org.apache.spark.rpc.{RpcAddress, RpcEndpointRef, RpcEnv}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * @author pengshaocheng
 */
object SyncHelloClient {
  def main(args: Array[String]): Unit = {
    val rpcEnv: RpcEnv = RpcEnv.create("hello-word", "localhost", 3031, new SparkConf(), null, clientMode = true)
    val endPointRef: RpcEndpointRef = rpcEnv.setupEndpointRef(RpcAddress("localhost", 3031), "hello-service")
    val result = endPointRef.askSync[String](SayBye("neo"))
    println(result)
  }
}

object AsyncHelloClient {
  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val rpcEnv: RpcEnv = RpcEnv.create("hello-word", "localhost", 3031, new SparkConf(), null, clientMode = true)
    val endPointRef: RpcEndpointRef = rpcEnv.setupEndpointRef(RpcAddress("localhost", 3031), "hello-service")
    val future: Future[String] = endPointRef.ask[String](SayHi("neo"))
    future.onComplete {
      case scala.util.Success(value) => println(s"Got the result = $value")
      case scala.util.Failure(e) => println(s"Got error: $e")
    }
    Await.result(future, Duration.apply("30s"))
  }
}
