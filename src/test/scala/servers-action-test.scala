package com.rackspace.usage

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.w3c.dom.Document

import scala.xml._

import com.rackspace.com.papi.components.checker.servlet.RequestAttributes._
import com.rackspace.cloud.api.wadl.Converters._
import com.rackspace.com.papi.components.checker.Converters._
import com.rackspace.com.papi.components.checker.servlet.RequestAttributes._

import BaseUsageSuite._

//
//  Tests on servers action transformations
//

@RunWith(classOf[JUnitRunner])
class ServersActionSuite extends BaseUsageSuite {
  register ("csd", "http://docs.rackspace.com/event/servers/slice")

  val sliceAction = <atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:title>Slice Action</atom:title>
      <atom:content type="application/xml">
                <event xmlns="http://docs.rackspace.com/core/event"
                                xmlns:csd="http://docs.rackspace.com/event/servers/slice"
                                version="1" tenantId="555"
                                id="560490c6-6c63-11e1-adfe-27851d5aed13"
                                resourceId="4116"
                                type="INFO" dataCenter="DFW1" region="DFW"
                                eventTime="2012-09-15T11:51:11Z">
                        <csd:product version="1" serviceCode="CloudServers"
                                        resourceType="SLICE" managed="false" imageId="101"
                                        options="5" huddleId="202" serverId="10"
                                        action="RESIZE" imageName="Name" customerId="100"
                                        flavorId="101" status="BUILD" sliceType="CLOUD"
                                        privateIp="1.1.1.1" publicIp="1.1.1.1"
                                        dns1="1.1.1.1" dns2="1.1.1.1"
                                        createdAt="2011-05-15T11:51:11Z">
                                <csd:sliceMetaData key="key1" value="value1"/>
                                <csd:sliceMetaData key="key2" value="value2"/>
                                <csd:additionalPublicAddress ip="1.1.1.1"
                                        dns1="1.1.1.1" dns2="1.1.1.1"/>
                                <csd:additionalPublicAddress ip="1.1.1.2"
                                        dns1="1.1.1.2" dns2="1.1.1.2"/>
                        </csd:product>
                </event>
    </atom:content>
    </atom:entry>

  test("In a slice action event action and status should be added as categories") {
    val req = request("POST", "/servers/events", "application/atom+xml", sliceAction, SERVICE_ADMIN)
    atomValidator.validate(req, response, chain)
    assert(getProcessedXML(req), "count(/atom:entry/atom:category[@term = 'action:RESIZE']) = 1")
    assert(getProcessedXML(req), "count(/atom:entry/atom:category[@term = 'status:BUILD']) = 1")
  }
}
