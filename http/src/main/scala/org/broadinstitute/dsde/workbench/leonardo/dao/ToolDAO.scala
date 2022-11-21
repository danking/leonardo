package org.broadinstitute.dsde.workbench.leonardo.dao

import org.broadinstitute.dsde.workbench.leonardo.RuntimeContainerServiceType.{
  JupyterLabService,
  JupyterService,
  RStudioService,
  WelderService
}
import org.broadinstitute.dsde.workbench.leonardo.{CloudContext, RuntimeContainerServiceType, RuntimeName}

trait ToolDAO[F[_], A] {
  def isProxyAvailable(cloudContext: CloudContext, runtimeName: RuntimeName): F[Boolean]
}

object ToolDAO {
  def clusterToolToToolDao[F[_]](
    jupyterDAO: JupyterDAO[F],
    welderDAO: WelderDAO[F],
    rstudioDAO: RStudioDAO[F]
  ): RuntimeContainerServiceType => ToolDAO[F, RuntimeContainerServiceType] =
    clusterTool =>
      clusterTool match {
        case JupyterService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            jupyterDAO.isProxyAvailable(cloudContext, runtimeName)
        case JupyterLabService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            // For the endpoints that we care about, JupyterLab has the same underlying API as
            // Jupyter, so there's no need to duplicate code by implementing a new JupyterLabDAO.
            // Hence, we call JupyterDAO here.
            jupyterDAO.isProxyAvailable(cloudContext, runtimeName)
        case WelderService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            welderDAO.isProxyAvailable(cloudContext, runtimeName)
        case RStudioService =>
          (cloudContext: CloudContext, runtimeName: RuntimeName) =>
            rstudioDAO.isProxyAvailable(cloudContext, runtimeName)
      }
}
