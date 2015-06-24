package akka.rtcweb

import akka.testkit.TestKitBase

/**
 * A test-bench that sets up two or more independent actor-systems that should establish a common DataChannel.
 * It provides a signalling channel that all participants can use to exchange session initiation messages.
 */
trait IntegrationTestbench extends TestKitBase {

}
