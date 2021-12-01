package org.broadinstitute.dsde.workbench.leonardo

import cats.implicits._

final case class TenantId(value: String) extends AnyVal
final case class SubscriptionId(value: String) extends AnyVal
final case class ManagedResourceGroupName(value: String) extends AnyVal

final case class AzureCloudContext(tenantId: TenantId,
                                   subscriptionId: SubscriptionId,
                                   managedResourceGroupName: ManagedResourceGroupName) {
  val asString = s"${tenantId.value}/${subscriptionId.value}/${managedResourceGroupName.value}"
}

object AzureCloudContext {
  def fromString(s: String): Either[String, AzureCloudContext] = {
    val res = for {
      splitted <- Either.catchNonFatal(s.split("/"))
      tenantId <- Either.catchNonFatal(splitted(0)).map(TenantId)
      subscriptionId <- Either.catchNonFatal(splitted(1)).map(SubscriptionId)
      mrgName <- Either.catchNonFatal(splitted(2)).map(ManagedResourceGroupName)
    } yield AzureCloudContext(tenantId, subscriptionId, mrgName)
    res.leftMap(t => s"Fail to decode $s as Azure Cloud Context due to ${t.getMessage}")
  }
}