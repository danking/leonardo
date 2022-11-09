package org.broadinstitute.dsde.workbench.leonardo.dao

import cats.effect.Async
import org.broadinstitute.dsde.workbench.leonardo.RuntimeContainerServiceType.{JupyterLabService, JupyterService, RStudioService, WelderService}
import org.broadinstitute.dsde.workbench.leonardo.{CloudContext, RuntimeContainerServiceType, RuntimeName}

trait ToolDAO[F[_], A] {
  def isProxyAvailable(cloudContext: CloudContext, runtimeName: RuntimeName): F[Boolean]
}

object ToolDAO {
  def clusterToolToToolDao[F[_]](
    jupyterDAO: JupyterDAO[F],
    welderDAO: WelderDAO[F],
    rstudioDAO: RStudioDAO[F]
  )(implicit F: Async[F]): RuntimeContainerServiceType => ToolDAO[F, RuntimeContainerServiceType] =
    clusterTool =>
      clusterTool match {
        case JupyterService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            F.pure(true)
        // todo: can this just use the existing JupyterDAO instead of defining it's own?
        case JupyterLabService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            jupyterDAO.isProxyAvailable(cloudContext, runtimeName)
        case WelderService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            welderDAO.isProxyAvailable(cloudContext, runtimeName)
        case RStudioService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            rstudioDAO.isProxyAvailable(cloudContext, runtimeName)
      }
}
