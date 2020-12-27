/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark

import java.net.{Authenticator, PasswordAuthentication}
import java.nio.charset.StandardCharsets.UTF_8

import org.apache.spark.internal.Logging
import org.apache.spark.network.sasl.SecretKeyHolder
import org.apache.spark.util.Utils

/**
 * Spark class responsible for security.
 *
 * In general this class should be instantiated by the SparkEnv and most components
 * should access it from that. There are some cases where the SparkEnv hasn't been
 * initialized yet and this class must be instantiated directly.
 *
 * This class implements all of the configuration related to security features described
 * in the "Security" document. Please refer to that document for specific features implemented
 * here.
 */
private[spark] class SecurityManager()
  extends Logging with SecretKeyHolder {
  override def getSaslUser(s: String): String = {
    null
  }

  override def getSecretKey(s: String): String = {
    null
  }
}
