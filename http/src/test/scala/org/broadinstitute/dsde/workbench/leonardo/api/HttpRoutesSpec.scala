package org.broadinstitute.dsde.workbench.leonardo.api

import java.net.URL

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.broadinstitute.dsde.workbench.google2.MachineTypeName
import org.broadinstitute.dsde.workbench.leonardo.CommonTestData.{contentSecurityPolicy, swaggerConfig}
import org.broadinstitute.dsde.workbench.leonardo.JsonCodec._
import org.broadinstitute.dsde.workbench.leonardo._
import org.broadinstitute.dsde.workbench.leonardo.api.HttpRoutesSpec._
import org.broadinstitute.dsde.workbench.leonardo.db.TestComponent
import org.broadinstitute.dsde.workbench.leonardo.http.api.RoutesTestJsonSupport.runtimeConfigRequestEncoder
import org.broadinstitute.dsde.workbench.leonardo.http.api.{
  CreateRuntime2Request,
  HttpRoutes,
  ListRuntimeResponse2,
  TestLeoRoutes,
  UpdateRuntimeConfigRequest,
  UpdateRuntimeRequest
}
import org.broadinstitute.dsde.workbench.leonardo.http.service.{GetRuntimeResponse, RuntimeConfigRequest}
import org.broadinstitute.dsde.workbench.leonardo.service.MockRuntimeServiceInterp
import org.broadinstitute.dsde.workbench.model.WorkbenchEmail
import org.broadinstitute.dsde.workbench.model.google.{GcsBucketName, GcsObjectName, GcsPath, GoogleProject}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class HttpRoutesSpec
    extends FlatSpec
    with ScalatestRouteTest
    with LeonardoTestSuite
    with ScalaFutures
    with Matchers
    with TestComponent
    with TestLeoRoutes {
  val clusterName = "test"
  val googleProject = "dsp-leo-test"

  val routes = new HttpRoutes(
    swaggerConfig,
    statusService,
    proxyService,
    leonardoService,
    MockRuntimeServiceInterp,
    timedUserInfoDirectives,
    contentSecurityPolicy
  )

  "HttpRoutes" should "create runtime" in {
    val request = CreateRuntime2Request(
      Map("lbl1" -> "true"),
      None,
      Some(UserScriptPath.Gcs(GcsPath(GcsBucketName("bucket"), GcsObjectName("script.sh")))),
      None,
      Some(RuntimeConfigRequest.GceConfig(Some(MachineTypeName("n1-standard-4")), Some(DiskSize(100)))),
      None,
      Some(true),
      Some(30.minutes),
      None,
      Some(ContainerImage.DockerHub("myrepo/myimage")),
      Some(ContainerImage.DockerHub("broadinstitute/welder")),
      Set.empty,
      Map.empty
    )
    Post("/api/google/v1/runtimes/googleProject1/runtime1")
      .withEntity(ContentTypes.`application/json`, request.asJson.spaces2) ~> routes.route ~> check {
      status shouldEqual StatusCodes.Accepted
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "create a runtime with default parameters" in {
    Post("/api/google/v1/runtimes/googleProject1/runtime1")
      .withEntity(ContentTypes.`application/json`, "{}") ~> routes.route ~> check {
      status shouldEqual StatusCodes.Accepted
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "get a runtime" in {
    Get("/api/google/v1/runtimes/googleProject/runtime1") ~> routes.route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[GetRuntimeResponse].id shouldBe CommonTestData.testCluster.id
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "list runtimes with a project" in {
    Get("/api/google/v1/runtimes/googleProject") ~> routes.route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[Vector[ListRuntimeResponse2]].map(_.id) shouldBe Vector(CommonTestData.testCluster.id)
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "list runtimes without a project" in {
    Get("/api/google/v1/runtimes") ~> routes.route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[Vector[ListRuntimeResponse2]].map(_.id) shouldBe Vector(CommonTestData.testCluster.id)
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "list runtimes with parameters" in {
    Get("/api/google/v1/runtimes?project=foo&creator=bar") ~> routes.route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[Vector[ListRuntimeResponse2]].map(_.id) shouldBe Vector(CommonTestData.testCluster.id)
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "delete a runtime" in {
    Delete("/api/google/v1/runtimes/googleProject1/runtime1") ~> routes.route ~> check {
      status shouldEqual StatusCodes.Accepted
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "stop a runtime" in {
    Post("/api/google/v1/runtimes/googleProject1/runtime1/stop") ~> routes.route ~> check {
      status shouldEqual StatusCodes.Accepted
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "start a runtime" in {
    Post("/api/google/v1/runtimes/googleProject1/runtime1/start") ~> routes.route ~> check {
      status shouldEqual StatusCodes.Accepted
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "update a runtime" in {
    val request = UpdateRuntimeRequest(
      Some(UpdateRuntimeConfigRequest.GceConfig(Some(MachineTypeName("n1-micro-2")), Some(DiskSize(20)))),
      true,
      Some(true),
      Some(5.minutes)
    )
    Patch("/api/google/v1/runtimes/googleProject1/runtime1")
      .withEntity(ContentTypes.`application/json`, request.asJson.spaces2) ~> routes.route ~> check {
      status shouldEqual StatusCodes.Accepted
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "update a runtime with default parameters" in {
    Patch("/api/google/v1/runtimes/googleProject1/runtime1")
      .withEntity(ContentTypes.`application/json`, "{}") ~> routes.route ~> check {
      status shouldEqual StatusCodes.Accepted
      validateRawCookie(header("Set-Cookie"))
    }
  }

  it should "not handle patch with invalid runtime config" in {
    val negative =
      UpdateRuntimeRequest(Some(UpdateRuntimeConfigRequest.GceConfig(None, Some(DiskSize(-100)))), false, None, None)
    Patch("/api/google/v1/runtimes/googleProject1/runtime1")
      .withEntity(ContentTypes.`application/json`, negative.asJson.spaces2) ~> routes.route ~> check {
      handled shouldBe false
    }
    val oneWorker =
      UpdateRuntimeRequest(Some(UpdateRuntimeConfigRequest.DataprocConfig(None, None, Some(1), None)),
                           false,
                           None,
                           None)
    Patch("/api/google/v1/runtimes/googleProject1/runtime1")
      .withEntity(ContentTypes.`application/json`, oneWorker.asJson.spaces2) ~> routes.route ~> check {
      handled shouldBe false
    }
  }

  it should "not handle unrecognized routes" in {
    Post("/api/google/v1/runtime/googleProject1/runtime1/unhandled") ~> routes.route ~> check {
      handled shouldBe false
    }
    Get("/api/google/v1/badruntime/googleProject1/runtime1") ~> routes.route ~> check {
      handled shouldBe false
    }
    Get("/api/google/v2/runtime/googleProject1/runtime1") ~> routes.route ~> check {
      handled shouldBe false
    }
    Post("/api/google/v1/runtime/googleProject1/runtime1") ~> routes.route ~> check {
      handled shouldBe false
    }
  }
}

object HttpRoutesSpec {
  implicit val createRuntime2RequestEncoder: Encoder[CreateRuntime2Request] = Encoder.forProduct13(
    "labels",
    "jupyterExtensionUri",
    "jupyterUserScriptUri",
    "jupyterStartUserScriptUri",
    "runtimeConfig",
    "userJupyterExtensionConfig",
    "autopause",
    "autopauseThreshold",
    "defaultClientId",
    "toolDockerImage",
    "welderDockerImage",
    "scopes",
    "customEnvironmentVariables"
  )(x =>
    (
      x.labels,
      x.jupyterExtensionUri,
      x.jupyterUserScriptUri,
      x.jupyterStartUserScriptUri,
      x.runtimeConfig,
      x.userJupyterExtensionConfig,
      x.autopause,
      x.autopauseThreshold.map(_.toMinutes),
      x.defaultClientId,
      x.toolDockerImage,
      x.welderDockerImage,
      x.scopes,
      x.customEnvironmentVariables
    )
  )

  implicit val updateGceConfigRequestEncoder: Encoder[UpdateRuntimeConfigRequest.GceConfig] = Encoder.forProduct3(
    "cloudService",
    "machineType",
    "diskSize"
  )(x => (x.cloudService, x.updatedMachineType, x.updatedDiskSize))

  implicit val updateDataprocConfigRequestEncoder: Encoder[UpdateRuntimeConfigRequest.DataprocConfig] =
    Encoder.forProduct5(
      "cloudService",
      "masterMachineType",
      "masterDiskSize",
      "numberOfWorkers",
      "numberOfPreemptibleWorkers"
    )(x =>
      (x.cloudService,
       x.updatedMasterMachineType,
       x.updatedMasterDiskSize,
       x.updatedNumberOfWorkers,
       x.updatedNumberOfPreemptibleWorkers)
    )

  implicit val updateRuntimeConfigRequestEncoder: Encoder[UpdateRuntimeConfigRequest] = Encoder.instance { x =>
    x match {
      case x: UpdateRuntimeConfigRequest.DataprocConfig => x.asJson
      case x: UpdateRuntimeConfigRequest.GceConfig      => x.asJson
    }
  }

  implicit val updateRuntimeRequestEncoder: Encoder[UpdateRuntimeRequest] = Encoder.forProduct4(
    "runtimeConfig",
    "allowStop",
    "autopause",
    "autopauseThreshold"
  )(x =>
    (
      x.updatedRuntimeConfig,
      x.allowStop,
      x.updateAutopauseEnabled,
      x.updateAutopauseThreshold.map(_.toMinutes)
    )
  )

  implicit val getClusterResponseDecoder: Decoder[GetRuntimeResponse] = Decoder.instance { x =>
    for {
      id <- x.downField("id").as[Long]
      clusterName <- x.downField("runtimeName").as[RuntimeName]
      googleProject <- x.downField("googleProject").as[GoogleProject]
      serviceAccount <- x.downField("serviceAccount").as[WorkbenchEmail]
      asyncRuntimeFields <- x.downField("asyncRuntimeFields").as[Option[AsyncRuntimeFields]]
      auditInfo <- x.downField("auditInfo").as[AuditInfo]
      runtimeConfig <- x.downField("runtimeConfig").as[RuntimeConfig]
      clusterUrl <- x.downField("proxyUrl").as[URL]
      status <- x.downField("status").as[RuntimeStatus]
      labels <- x.downField("labels").as[LabelMap]
      jupyterExtensionUri <- x.downField("jupyterExtensionUri").as[Option[GcsPath]]
      jupyterUserScriptUri <- x.downField("jupyterUserScriptUri").as[Option[UserScriptPath]]
      jupyterStartUserScriptUri <- x.downField("jupyterStartUserScriptUri").as[Option[UserScriptPath]]
      errors <- x.downField("errors").as[List[RuntimeError]]
      userJupyterExtensionConfig <- x.downField("userJupyterExtensionConfig").as[Option[UserJupyterExtensionConfig]]
      autopauseThreshold <- x.downField("autopauseThreshold").as[Int]
      defaultClientId <- x.downField("defaultClientId").as[Option[String]]
      clusterImages <- x.downField("runtimeImages").as[Set[RuntimeImage]]
      scopes <- x.downField("scopes").as[Set[String]]
    } yield GetRuntimeResponse(
      id,
      RuntimeInternalId(""),
      clusterName,
      googleProject,
      ServiceAccountInfo(Some(serviceAccount), None),
      asyncRuntimeFields,
      auditInfo,
      runtimeConfig,
      clusterUrl,
      status,
      labels,
      jupyterExtensionUri,
      jupyterUserScriptUri,
      jupyterStartUserScriptUri,
      errors,
      Set.empty, // Dataproc instances
      userJupyterExtensionConfig,
      autopauseThreshold,
      defaultClientId,
      false,
      clusterImages,
      scopes,
      true,
      false,
      Map.empty
    )
  }

  implicit val listClusterResponseDecoder: Decoder[ListRuntimeResponse2] = Decoder.instance { x =>
    for {
      id <- x.downField("id").as[Long]
      clusterName <- x.downField("runtimeName").as[RuntimeName]
      googleProject <- x.downField("googleProject").as[GoogleProject]
      auditInfo <- x.downField("auditInfo").as[AuditInfo]
      machineConfig <- x.downField("runtimeConfig").as[RuntimeConfig]
      clusterUrl <- x.downField("proxyUrl").as[URL]
      status <- x.downField("status").as[RuntimeStatus]
      labels <- x.downField("labels").as[LabelMap]
      patchInProgress <- x.downField("patchInProgress").as[Boolean]
    } yield ListRuntimeResponse2(
      id,
      RuntimeInternalId("fakeId"),
      clusterName,
      googleProject,
      auditInfo,
      machineConfig,
      clusterUrl,
      status,
      labels,
      patchInProgress
    )
  }
}
