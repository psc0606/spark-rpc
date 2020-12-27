package org.apache.spark.example

import org.apache.spark.SparkConf
import org.apache.spark.rpc.{RpcEndpoint, RpcEnv}

/**
 * @author pengshaocheng
 */
object HelloServer {
  def main(args: Array[String]): Unit = {
    val rpcEnv: RpcEnv = RpcEnv.create("hello-word", "localhost", 3031, new SparkConf(), null)
    val helloEndpoint: RpcEndpoint = new HelloEndpoint(rpcEnv)
    rpcEnv.setupEndpoint("hello-service", helloEndpoint)
    rpcEnv.awaitTermination()
  }
}
