package com.rackspace.usage

import com.rackspace.com.papi.components.checker.Converters.doc2NodeSeq
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.xml.NodeSeq

@RunWith(classOf[JUnitRunner])
class DataCenterRegionValidationSuite extends BaseUsageSuite {

  import com.rackspace.usage.BaseUsageSuite._

  val validatedFeedUrl = "/usagetest1/events"
  val usageSummaryFeedUrl = "/usagesummary/sites/events"
  val usageSnapshotFeedUrl = "/sites/events"

  def cbsEntry(dataCenter: String, region: String): NodeSeq = <atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:title>CBS Type as Generic UsageType</atom:title>
    <atom:content type="application/xml">
      <event xmlns="http://docs.rackspace.com/core/event"
             xmlns:cbs="http://docs.rackspace.com/usage/cbs"
             version="1" tenantId="12334" resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549" resourceName="MyVolume"
             id="560490c6-6c63-11e1-adfe-27851d5aed13" type="USAGE" dataCenter={dataCenter} region={region}
             startTime="2012-03-12T11:51:11Z" endTime="2012-03-12T15:51:11Z">
        <cbs:product version="1" serviceCode="CloudBlockStorage" resourceType="VOLUME" type="SATA" provisioned="120"/>
      </event>
    </atom:content>
  </atom:entry>

  def usageSummaryEntry(dataCenter: String, region: String): NodeSeq = <atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:title>CloudSites Summary</atom:title>
    <atom:content type="application/xml">
      <event xmlns="http://docs.rackspace.com/core/event"
             xmlns:sample="http://docs.rackspace.com/usage/sites/subscription" id="e53d007a-fc23-11e1-975c-cfa6b29bb814"
             version="1" resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549" tenantId="1234"
             rackspaceAccountNumber="020-1234" startTime="2013-03-15T11:51:11Z" endTime="2013-03-16T11:51:11Z"
             duration="PT10M" type="USAGE_SUMMARY" dataCenter={dataCenter} region={region}>
        <sample:product serviceCode="CloudSites" version="1" resourceType="SITES_SUBSCRIPTION"/>
      </event>
    </atom:content>
  </atom:entry>

  def usageSnapshotEntry(dataCenter: String, region: String): NodeSeq = <atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:title type="text">Sites</atom:title>
    <atom:content type="application/xml">
      <event xmlns="http://docs.rackspace.com/core/event"
             xmlns:sites="http://docs.rackspace.com/usage/sites/subscription"
             version="1" tenantId="12334" resourceId="256638" resourceName="Mosso Standard Offering"
             clientUsername="a1user" agentUsername="racker1" id="560490c6-6c63-11e1-adfe-27851d5aed13"
             type="USAGE_SNAPSHOT" dataCenter={dataCenter} region={region} eventTime="2012-03-12T11:51:11Z">
        <sites:product version="1" serviceCode="CloudSites" resourceType="SITES_SUBSCRIPTION" action="UPDATE"
                       isNewAccount="false"/>
      </event>
    </atom:content>
  </atom:entry>

  List(
    ("USAGE", validatedFeedUrl, cbsEntry _),
    ("USAGE_SUMMARY", usageSummaryFeedUrl, usageSummaryEntry _),
    ("USAGE_SNAPSHOT", usageSnapshotFeedUrl, usageSnapshotEntry _)
  ) foreach { case (usageType, url, entry) =>
    List(
      ("DFW1", "DFW"),
      ("DFW2", "DFW"),
      ("DFW3", "DFW"),
      ("FRA1", "FRA"),
      ("IAD2", "IAD"),
      ("AWS-AP-SOUTHEAST-1", "AWS-AP"),
      ("AWS-SA-EAST-1", "AWS-SA"),
      ("GLOBAL", "GLOBAL")
    ) foreach { case (dataCenter, region) =>
      test(s"should allow creating a new event with usage type '$usageType', data center '$dataCenter' and region '$region'") {
        val req = request("POST", url, "application/atom+xml", entry(dataCenter, region), SERVICE_ADMIN)
        atomValidator.validate(req, response, chain)
        assert(getProcessedXML(req), s"/atom:entry/atom:category/@term = 'dc:$dataCenter'")
      }
    }
  }

  List(
    ("DFW1", "FRA"),
    ("IAD2", "DFW"),
    ("AWS-AP-SOUTHEAST-2", "LON"),
    ("GLOBAL", "DFW")
  ) foreach { case (dataCenter, region) =>
    test(s"should not allow creating a new event with the usage type 'USAGE', data center '$dataCenter' and region '$region' (data center and region must match)") {
      val req = request("POST", validatedFeedUrl, "application/atom+xml", cbsEntry(dataCenter, region), SERVICE_ADMIN)
      assertResultFailed(atomValidator.validate(req, response, chain))
    }
  }

  test("should not allow creating a new event with the usage type 'USAGE_SUMMARY', data center 'GLOBAL' and region 'HKG' (region must be GLOBAL due to type)") {
    val req = request("POST", usageSummaryFeedUrl, "application/atom+xml", usageSummaryEntry("GLOBAL", "HKG"), SERVICE_ADMIN)
    assertResultFailed(atomValidator.validate(req, response, chain))
  }

  test("should not allow creating a new event with the usage type 'USAGE_SNAPSHOT', data center 'GLOBAL' and region 'DFW' (region must be GLOBAL due to type)") {
    val req = request("POST", usageSnapshotFeedUrl, "application/atom+xml", usageSnapshotEntry("GLOBAL", "DFW"), SERVICE_ADMIN)
    assertResultFailed(atomValidator.validate(req, response, chain))
  }
}
