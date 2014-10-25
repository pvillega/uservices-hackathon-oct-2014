package model

import akka.util.ByteString
import com.rabbitmq.client.{Channel, Connection}
import play.api.Logger

import scala.collection.JavaConversions._

/**
 * Utility trait exposing the logic to initiate new channel and bindings.
 */
trait ChannelInitializer {

  def initChannel(binding: RabbitBinding)(implicit connection: Connection): Channel = {
    val ch = connection.createChannel()
    ch.exchangeDeclare(binding.exchange, "direct", true)
    ch.queueDeclare(binding.queue, true, false, false, Map[String, java.lang.Object]())
    ch.queueBind(binding.queue, binding.exchange, "")
    ch
  }
}

/**
 * Simple representation of RabbitMQ message.
 *
 * Encloses a channel to allow acknowledging or rejecting the message later during processing.
 */
class RabbitMessage(val deliveryTag: Long, val body: ByteString, channel: Channel) {

  /**
   * Ackowledge the message.
   */
  def ack(): Unit = {
    Logger.debug(s"ack $deliveryTag")
    channel.basicAck(deliveryTag, false)
  }

  /**
   * Reject and requeue the message.
   */
  def nack(): Unit = {
    Logger.debug(s"nack $deliveryTag")
    channel.basicNack(deliveryTag, false, true)
  }
}

/**
 * Exchange and queue names.
 */
case class RabbitBinding(exchange: String, queue: String)


