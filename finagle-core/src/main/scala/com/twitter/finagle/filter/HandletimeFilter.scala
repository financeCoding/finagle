package com.twitter.finagle.filter

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{param, Service, ServiceFactory, SimpleFilter, Stack, Stackable}
import com.twitter.util.{Stopwatch, Future}

private[finagle] object HandletimeFilter {
  val role = Stack.Role("HandleTime")

  /**
   * Creates a [[com.twitter.finagle.Stackable]] [[com.twitter.finagle.filter.HandletimeFilter]].
   */
  def module[Req, Rep]: Stackable[ServiceFactory[Req, Rep]] =
    new Stack.Simple[ServiceFactory[Req, Rep]] {
      val role = HandletimeFilter.role
      val description = "Record elapsed execution time of underlying service"
      def make(next: ServiceFactory[Req, Rep])(implicit params: Params) = {
        val param.Stats(statsReceiver) = get[param.Stats]
        new HandletimeFilter(statsReceiver) andThen next
      }
    }
}

/**
 * A [[com.twitter.finagle.Filter]] that records the elapsed execution times of
 * the underlying [[com.twitter.finagle.Service]]. Durations are recorded in
 * microseconds and emitted as a stat labeled "handletime_us" to the argument
 * [[com.twitter.finagle.stats.StatsReceiver]].
 */
class HandletimeFilter[Req, Rep](statsReceiver: StatsReceiver)
  extends SimpleFilter[Req, Rep]
{
  private[this] val stat = statsReceiver.stat("handletime_us")

  def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    val elapsed = Stopwatch.start()
    try
      service(request)
    finally
      stat.add(elapsed().inMicroseconds)
  }
}
