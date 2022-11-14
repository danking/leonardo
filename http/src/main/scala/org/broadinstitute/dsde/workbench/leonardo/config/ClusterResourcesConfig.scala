package org.broadinstitute.dsde.workbench.leonardo
package config

case class ClusterResourcesConfig(initScript: RuntimeResource, cloudInit: Option[RuntimeResource], startupScript: RuntimeResource, shutdownScript: RuntimeResource, jupyterDockerCompose: RuntimeResource, gpuDockerCompose: Option[RuntimeResource], rstudioDockerCompose: RuntimeResource, proxyDockerCompose: RuntimeResource, welderDockerCompose: RuntimeResource, proxySiteConf: RuntimeResource, jupyterNotebookConfigUri: RuntimeResource, jupyterNotebookFrontendConfigUri: RuntimeResource, customEnvVarsConfigUri: RuntimeResource, jupyterLabDockerCompose: RuntimeResource)

object ClusterResourcesConfig {
  val basePath = "init-resources"
}
