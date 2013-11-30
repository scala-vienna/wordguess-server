import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.clashcode.web.controllers.Application
import play.api.Application
import play.api.{Play, GlobalSettings, Logger, Application}
import scala.Some
import actors.GameServerActor

object Global extends GlobalSettings {

  var maybeCluster = Option.empty[ActorSystem]

  override def onStart(app: Application) {
    super.onStart(app)

    // start second system (for clustering example)
    Play.configuration(app).getConfig("cluster").foreach(clusterConfig => {

      // create cluster system
      val system = ActorSystem("cluster", clusterConfig.underlying)
      maybeCluster = Some(system)

      // start tournament hoster
      val hostingActor = system.actorOf(Props[GameServerActor], "main")
      Application.maybeHostingActor = Some(hostingActor)

      // hosting actor listens to cluster events
      Cluster(system).subscribe(hostingActor, classOf[ClusterDomainEvent])
    })
  }

  override def onStop(app: Application) {
    super.onStop(app)
    maybeCluster.foreach(_.shutdown())
  }

}


