package service

import java.net.InetSocketAddress

import actors.{RabbitPublisher, RabbitConsumerActor, RabbitConnectionActor}
import akka.actor.ActorSystem
import akka.stream.actor.ActorProducer
import akka.stream.scaladsl.{Flow, Duct}
import akka.stream.{MaterializerSettings, FlowMaterializer}
import akka.util.Timeout
import com.rabbitmq.client.Connection
import scala.concurrent.duration._

class RabbitMQService {

  implicit val timeout = Timeout(2 seconds)

  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

  implicit val executor = actorSystem.dispatcher

  val materializer = FlowMaterializer(MaterializerSettings())

  val connectionActor = actorSystem.actorOf(
    RabbitConnectionActor.props(new InetSocketAddress(RabbitMQ.host, RabbitMQ.port))
  )

  /*
   * Ask for a connection and start processing.
   */
  (connectionActor ? Connect).mapTo[Connection] map { implicit conn =>

    val rabbitConsumer = ActorProducer(actorSystem.actorOf(RabbitConsumerActor.props(IN_BINDING)))

    val domainProcessingDuct = MyDomainProcessing()

    val okPublisherDuct = new RabbitPublisher(OUT_OK_BINDING).flow

    val publisherDuct: String => Duct[String, Unit] = ex => ex match {
      case OUT_OK_EXCHANGE => okPublisherDuct
      case OUT_NOK_EXCHANGE => nokPublisherDuct
    }

    /*
     * connect flows with ducts and consume
     */
    Flow(rabbitConsumer) append domainProcessingDuct map {
      case (exchange, producer) =>

        // start a new flow for each message type
        Flow(producer)

          // extract the message
          .map(_.message)

          // add the outbound publishing duct
          .append(publisherDuct(exchange))

          // and start the flow
          .consume(materializer)

    } consume(materializer)
  }


}

object RabbitMQ {
  val host = "178.62.106.39"
  val port = 5672
  val exchange = "combo"
  val exchangeType = "topic"
  val routingKey = "chat"
}
