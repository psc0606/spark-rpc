# spark-rpc
spark-rpc is a rpc framework split from [Spark](https://github.com/apache/spark).

## Dependency

# Usage
## 1. How to run?

## 2. start server

## 3. start client

## 4. configuration
| Property Name | Default | Meaning | Since Version |
| ----- | ----- | ----- | ----- |
| spark.port.maxRetries | 16  | Maximum number of retries when binding to a port before giving up. When a port is given a specific value (non 0), each subsequent retry will increment the port used in the previous attempt by 1 before retrying. This essentially allows it to try a range of ports from the start port specified to port + maxRetries. |  |