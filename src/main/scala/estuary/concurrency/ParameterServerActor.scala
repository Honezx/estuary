package estuary.concurrency

import akka.actor.{Actor, ActorLogging, ActorRef}
import estuary.concurrency.ParameterServerActor._

class ParameterServerActor[B](initParams: B) extends Actor with ActorLogging{
  private[this] var parameters: B = initParams
  private[this] var miniBatchTime: Int = 0

  override def receive: Actor.Receive = {
    case GetCurrentParams => sender ! CurrentParams(parameters)

    case GetCurrentParamsForUpdate => sender ! CurrentParamsForUpdate(parameters, miniBatchTime)

    case UpdateParams(newParams: B) =>
      parameters = newParams
      miniBatchTime += 1
  }
}

object ParameterServerActor {
  sealed trait ParameterServerActorMsg extends Serializable with MyMessage

  final case object GetCurrentParams extends ParameterServerActorMsg
  final case object GetCurrentParamsForUpdate extends ParameterServerActorMsg
  final case object GetTrainedParams extends ParameterServerActorMsg
  final case object GetCostHistory extends ParameterServerActorMsg

  final case class UpdateParams[B](newParams: B) extends ParameterServerActorMsg

  final case class SetWorkActorsRef(workActors: Seq[ActorRef]) extends ParameterServerActorMsg

  final case class CurrentParams[B](params: B) extends ParameterServerActorMsg
  final case class CurrentParamsForUpdate[B](params: B, miniBatchTime: Int) extends ParameterServerActorMsg

  final case class CostHistory(cost: Double) extends ParameterServerActorMsg
}
