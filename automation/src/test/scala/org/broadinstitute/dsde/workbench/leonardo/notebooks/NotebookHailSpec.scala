package org.broadinstitute.dsde.workbench.leonardo.notebooks

import cats.effect.unsafe.implicits.global
import org.broadinstitute.dsde.workbench.ResourceFile
import org.broadinstitute.dsde.workbench.auth.AuthToken
import org.broadinstitute.dsde.workbench.dao.Google.googleStorageDAO
import org.broadinstitute.dsde.workbench.leonardo.{CloudService, LeonardoConfig, RuntimeFixtureSpec}
import org.broadinstitute.dsde.workbench.model.google.{EmailGcsEntity, GcsEntityTypes, GcsObjectName, GcsRoles}
import org.broadinstitute.dsde.workbench.service.Sam
import org.scalatest.DoNotDiscover

import scala.concurrent.duration._

/**
 * This spec verifies Hail and Spark functionality.
 */
@DoNotDiscover
class NotebookHailSpec extends RuntimeFixtureSpec with NotebookTestUtils {
  implicit def ronToken: AuthToken = ronAuthToken.unsafeRunSync()

  // Should match the HAILHASH env var in the Jupyter Dockerfile
  val hailTutorialUploadFile = ResourceFile(s"diff-tests/hail-tutorial.ipynb")
  override val toolDockerImage: Option[String] = Some(LeonardoConfig.Leonardo.hailImageUrl)
  override val cloudService: Option[CloudService] = Some(CloudService.Dataproc)

  "NotebookHailSpec" - {
    // This test is not a no-op, the fixture it mixes in creates a runtime by default with the `toolDockerImage` we override
    "should create a hail runtime" in { _ => }
  }
}
