import java.io._
import java.nio.charset.StandardCharsets
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.{StreamResult, StreamSource}
import javax.xml.xpath.XPathFactory

import com.rackspace.usage.BaseUsageSuite
import net.sf.saxon.Controller
import net.sf.saxon.lib.NamespaceConstant
import org.boon.json.JsonParserFactory
import org.junit.runner.RunWith
import org.scalatest.AppendedClues
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner

import scala.language.implicitConversions
import scala.util.parsing.json.JSON

/**
  * Tests for the XSLT that transforms XML events to JSON events
  */
@RunWith(classOf[JUnitRunner])
class Xml2JsonSuite extends BaseUsageSuite with AppendedClues {
  import Xml2JsonSuite._

  // During the "initialize" phase, we copy this file from src/main/xsl to target/
  // directory. This xslt "xsl:includes" another xslt that's generated (nonStringAttrs.xsl)
  // and lives in the target/ directory. The "xsl:include" href attribute can not be
  // parameterized. And since at runtime, the two files live in the same directory
  // (/etc/cloudfeeds/translation), it would be good to just deal with this at
  // build time.
  val xml2JsonXslt = new File("target/xslt-artifacts/xml2json-feeds.xsl")

  val xpathFactory = XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON, "net.sf.saxon.xpath.XPathFactoryImpl", null)
  val templates = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null).newTemplates(new StreamSource(xml2JsonXslt))

  register("atom", "http://www.w3.org/2005/Atom")
  val dedicatedvCloudMixedNonAdjacentCategoryXml = "<entry xmlns=\"http://www.w3.org/2005/Atom\">" +
    "  <title type=\"text\">Dedicated vCloud event</title>" +
    "  <category term=\"dedicatedvcloud.server.create.vm\"/>" +
    "  <id>urn:uuid:1f1b37e2-503b-4f32-bc8b-6d1d054efc21</id>" +
    "  <content type=\"application/json\">" +
    "    {\"tenant\":\"hybrid:2848639\",\"source\":\"qe.virtops.rackspacecloud.com\",\"type\":\"virtops.vm.create\",\"datacenter\":\"IAD3\",\"timestamp\":\"2014-10-29T16:23:13.431+0000\",\"vmProperties\":{\"organization\":\"urn:vcloud:org:0bc35ece-34d8-45bc-a61e-9e223e022165\",\"vcdName\":\"vcd02-2848639.mv.rackspace.com\",\"containerOS\":\"Microsoft Windows Server 2012 (64-bit)\",\"hypervisor\":\"581846\",\"vmName\":\"WindowsServer_2012_R2_Standard_vcloud_core\",\"computerName\":\"WindowsServ-001\",\"vcenterUuid\":\"4219fe9a-fc2d-cd52-d55d-4d6842c4beb2\",\"vcdUrn\":\"urn:vcloud:vm:fc51f8a1-11c5-4f78-89bf-611af92d8b83\",\"memoryMb\":\"4096\",\"cpuInfo\":{\"cpuCount\":\"4\",\"coresPerSocket\":\"1\"},\"networks\":[{\"name\":\"ExNet-Inside-VLAN1470\",\"ipAddress\":\"192.168.100.5\",\"vlan\":\"1470\",\"isPrimary\":\"true\"}]},\"raxData\":{\"platform.DVC_WINDOWS_UNSUPPORTED\":{\"category\":\"platform\",\"name\":\"DVC_WINDOWS_UNSUPPORTED\",\"attributes\":{},\"type\":\"DeviceConfig\"},\"osName.WINDOWS_2012_R2_STD_X64\":{\"category\":\"osName\",\"name\":\"WINDOWS_2012_R2_STD_X64\",\"attributes\":{\"Dedicated vCloud Director OS Type\":\"WINDOWS\"},\"type\":\"DeviceConfig\"}}}" +
    "  </content>" +
    "  <updated>2014-10-29T16:24:02.856Z</updated>" +
    "  <published>2014-10-29T16:24:02.856Z</published>" +
    "  <category term=\"DataCenter:IAD3\"/>" +
    "  <link href=\"https://atom.staging.ord1.us.ci.rackspace.net/dedicatedvcloud/events/entries/urn:uuid:1f1b37e2-503b-4f32-bc8b-6d1d054efc21\" rel=\"self\" />" +
    "</entry>"

  test("should transform /dedicatedvcloud event with mixed-content JSON inside XML and non-adjacent category properly") {
    val jsonResult = transform(new StreamSource(new StringReader(dedicatedvCloudMixedNonAdjacentCategoryXml)))
    //println(jsonResult)

    // check for entry
    val jsonObjects = JSON.parseFull(jsonResult).get.asInstanceOf[Map[String, Any]]
    assert(jsonObjects("entry") != null, "should have an 'entry' root element")
    val entryObject = jsonObjects("entry").asInstanceOf[Map[String, Any]]

    // check for id, updated, title, published
    assert(entryObject("id") != null, "entry should have an id")
    assert(entryObject("updated") != null, "entry should have an updated")
    assert(entryObject("title") != null, "entry should have a title")
    assert(entryObject("published") != null, "entry should have a published")

    // check for content
    assert(entryObject("content") != null, "entry should have a content")
    val contentObject = entryObject("content").asInstanceOf[Map[String, Any]]
    assert(contentObject("type").asInstanceOf[String] == "application/json", "content type must be application/json")
    assert(contentObject("@text") != null, "content must have @text")

    // check for the content payload
    val textObject = contentObject("@text").asInstanceOf[Map[String, Any]]
    assert(textObject("tenant").asInstanceOf[String] == "hybrid:2848639", "text needs to have tenant")

    // check handling of links
    val linkObjects = entryObject("link").asInstanceOf[List[Map[String, Any]]]
    assert(linkObjects.size == 1, "there is only 1 link object")
    assert(linkObjects.exists(link => {
      link("rel") == "self" &&
        link("href") == "https://atom.staging.ord1.us.ci.rackspace.net/dedicatedvcloud/events/entries/urn:uuid:1f1b37e2-503b-4f32-bc8b-6d1d054efc21"
    }),
      "should have correct self link")

    // check handling of categories
    val categoryObjects = entryObject("category").asInstanceOf[List[Map[String, Any]]]
    assert(categoryObjects.size == 2, "there are 2 category objects")
    assert(categoryObjects.exists(category => {
      category("term") == "DataCenter:IAD3" || category("term") == "dedicatedvcloud.server.create.vm"
    }),
      "should have category term=DataCenter:IAD3 or term=dedicatedvcloud.server.create.vm")
  }

  val errorXmls = List(
    ("should transform Abdera error XML properly",
      """<?xml version='1.0' encoding='UTF-8'?>
<error xmlns="http://abdera.apache.org">
  <code>400</code>
  <message>Invalid LDAP Search Parameter</message>
</error>"""),
    ("should transform LBaaS validation error XML properly",
      """<?xml version="1.0" encoding="UTF-8"?>
<error xmlns="http://abdera.apache.org"
       xmlns:db="http://docbook.org/ns/docbook"
       xmlns:xs="http://www.w3.org/2001/XMLSchema"
       xmlns:event="http://docs.rackspace.com/core/event"
       xmlns:dbaas="http://docs.rackspace.com/usage/dbaas"
       xmlns:maas="http://docs.rackspace.com/usage/maas"
       xmlns:lbaas="http://docs.rackspace.com/usage/lbaas"
       xmlns:lbaas-account="http://docs.rackspace.com/usage/lbaas/account"
       xmlns:cbs="http://docs.rackspace.com/usage/cbs"
       xmlns:cbs-snap="http://docs.rackspace.com/usage/cbs/snapshot"
       xmlns:cf-b="http://docs.rackspace.com/usage/cloudfiles/bandwidth"
       xmlns:cf-cdn="http://docs.rackspace.com/usage/cloudfiles/cdnbandwidth"
       xmlns:cf-str="http://docs.rackspace.com/usage/cloudfiles/storage"
       xmlns:rax="http://docs.rackspace.com/api"
       xmlns:atom="http://www.w3.org/2005/Atom">
  <code>400</code>
  <message>Bad Content: The synthesized attribute 'avgConcurrentConnectionsSum' should not be included in the original event.</message>
</error>""")
  )

  errorXmls.foreach(input => {
    val (title, xml) = input
    test(title) {
      val jsonResult = transform(new StreamSource(new StringReader(xml)))
      val jsonObjects = JSON.parseFull(jsonResult).get.asInstanceOf[Map[String, Any]]

      // check for error
      assert(jsonObjects("error") != null, "should have an 'error' root element")
      val errorObject = jsonObjects("error").asInstanceOf[Map[String, Any]]

      // check for code
      assert(errorObject("code") != null, "should have a 'code' element")
      assert(errorObject("code") == "400", "error code should be 400")

      // check for message
      assert(errorObject("message") != null, "should have a 'message' element")
    }
  })

  val emptyFeedXmlList = List(("empty feed",
    """
      |<feed xmlns="http://www.w3.org/2005/Atom">
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/" rel="current" />
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/" rel="self" />
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/?marker=last&amp;limit=25&amp;search=&amp;direction=backward" rel="last" />
      |</feed>
    """.stripMargin), ("empty feed without entries but with other elements",
    """
      |<feed xmlns="http://www.w3.org/2005/Atom"
      |      xmlns:fh="http://purl.org/syndication/history/1.0">
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/" rel="current" />
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/" rel="self" />
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/?marker=last&amp;limit=25&amp;search=&amp;direction=backward" rel="last" />
      |  <fh:archive />
      |  <id>uuid</id>
      |  <title type="text">feed1</title>
      |</feed>
    """.stripMargin), ("empty feed without entries but with other elements in between",
    """
      |<feed xmlns="http://www.w3.org/2005/Atom"
      |      xmlns:fh="http://purl.org/syndication/history/1.0">
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/" rel="current" />
      |  <fh:archive />
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/" rel="self" />
      |  <id>uuid</id>
      |  <link href="https://atom.test.ord1.us.ci.rackspace.net/functest2/events/?marker=last&amp;limit=25&amp;search=&amp;direction=backward" rel="last" />
      |</feed>
    """.stripMargin))

  emptyFeedXmlList.foreach(input => {
    val (title, emptyFeedXml) = input
    test(s"should transform an $title response properly") {
      val jsonResult = transform(new StreamSource(new StringReader(emptyFeedXml)))
      val jsonObjects = JSON.parseFull(jsonResult).get.asInstanceOf[Map[String, Any]]

      // check for feed
      assert(jsonObjects("feed") != null, "should have a 'feed' root element")
      val feedObject = jsonObjects("feed").asInstanceOf[Map[String, Any]]

      // check for links
      assert(feedObject("link") != null, "should have 'link' elements")
      val linkObjects = feedObject("link").asInstanceOf[List[Map[String, Any]]]
      assert(linkObjects.size == 3, "should have 3 link elements")

      // check for entries
      assert(feedObject.get("entry").isEmpty, "should not have entry")
    }
  })


  test("should transform /dedicatedvcloud feed response properly") {
    val jsonResult = transform(new StreamSource(new File("src/test/resources/dedicatedvcloud-feed-response.xml")))
    val jsonObjects = JSON.parseFull(jsonResult).get.asInstanceOf[Map[String, Any]]

    // check for feed
    jsonObjects should contain key "feed" // should have a 'feed' root element
    val feedObject = jsonObjects("feed").asInstanceOf[Map[String, Any]]

    // check for link
    feedObject should contain key "link" // should have 'link' elements
    val linkObjects = feedObject("link").asInstanceOf[List[Map[String, Any]]]
    linkObjects should have size 5 // should have 5 link elements

    // check for entry
    feedObject should contain key "entry" // should have 'entry' elements
    val entryObjects = feedObject("entry").asInstanceOf[List[Map[String, Any]]]
    entryObjects should have size 25 // should have 25 entry elements

    entryObjects.foreach(entry => {
      // check for link inside entry
      entry should contain key "link" // should have link inside entry element
      val linkObjects = entry("link").asInstanceOf[List[Map[String, Any]]]
      linkObjects should have size 1 // should have 1 link inside entry element
      linkObjects.head("rel") should equal ("self") // should have correct self link
      linkObjects.head("href").asInstanceOf[String] should not be empty // should have correct self link

      // check for category
      entry should contain key "category" // should have category inside entry element
      val categoryObjects = entry("category").asInstanceOf[List[Map[String, Any]]]
      categoryObjects.size should be >= 1 // should have at least 1 category element

      assert(categoryObjects.exists(cat => {
        cat.get("term") != null
      }), "should have category term")

      // check for id, updated, published
      entry should contain key "id" // should have an id inside entry element
      entry should contain key "updated" // should have an updated inside entry element
      entry should contain key "published" // should have a published inside entry element

      // check for content
      entry should contain key "content" // should have a content inside entry element
    })
  }

  test("should transform /dbaas event with non-string attributes properly") {
    val dbaasXml =
      """<?xml version="1.0"?>
        |<entry xmlns="http://www.w3.org/2005/Atom">
        |  <title>trove.instance.exists</title>
        |  <category term="DFW2"/>
        |  <category term="6093728"/>
        |  <category term="trove.instance.exists"/>
        |  <content type="application/xml">
        |    <event xmlns="http://docs.rackspace.com/core/event" xmlns:dbaas="http://docs.rackspace.com/usage/dbaas" dataCenter="DFW2"
        |           startTime="2014-08-06T21:11:55Z"
        |           endTime="2014-08-06T22:11:55Z"
        |           environment="QA" id="170d03b4-9582-4e34-80fb-d01911ef6469" region="DFW" resourceId="88939024-bf82-49ec-a177-d7ca55d60c9d" resourceName="ba858553-30f4-4f69-a65d-6addc5eafef5" rootAction="trove.instance.exists"
        |           tenantId="6093728" type="USAGE" version="1">
        |      <dbaas:product dbVersion="1.0" memory="32768" resourceType="MARIADB" serviceCode="CloudDatabase" storage="0" version="2"/>
        |    </event>
        |  </content>
        |</entry>
      """.stripMargin

    // check for entry
    val jsonResult = transform(new StreamSource(new StringReader(dbaasXml)))
    val jsonObjects = JSON.parseFull(jsonResult).get.asInstanceOf[Map[String, Any]]
    jsonObjects should contain key "entry" // should have an 'entry' root element
    val entryObject = jsonObjects("entry").asInstanceOf[Map[String, Any]]

    // check for content
    entryObject should contain key "content" // entry should have a content
    val contentObject = entryObject("content").asInstanceOf[Map[String, Any]]
    assert(contentObject.get("type").isEmpty, "content must not have type attribute")
    assert(contentObject.get("@text").isEmpty, "content must NOT @text")

    // check for event
    contentObject should contain key "event" // content should have event element
    val eventObject = contentObject("event").asInstanceOf[Map[String, Any]]

    // check for product
    eventObject should contain key "product" // event should have product element
    val productObject = eventObject("product").asInstanceOf[Map[String, Any]]
    productObject("memory") shouldBe 32768 // memory should be numeric and 32768
  }

  val jsonStringTests = List(
    ("with quotation mark", s"a${XmlQuote}b", """a"b"""),
    ("with two quotation marks", s"a${XmlQuote}b${XmlQuote}c", """a"b"c"""),
    ("starting with quotation mark", s"${XmlQuote}b", """"b"""),
    ("ending with quotation mark", s"a$XmlQuote", """a""""),
    ("starting and ending with quotation marks", s"${XmlQuote}a$XmlQuote", """"a""""),
    ("with only quotation mark", XmlQuote, "\""),
    ("with only two quotation marks", s"$XmlQuote$XmlQuote", "\"\""),
    ("with only three quotation marks", s"$XmlQuote$XmlQuote$XmlQuote", "\"\"\""),
    ("with only four quotation marks", s"$XmlQuote$XmlQuote$XmlQuote$XmlQuote", "\"\"\"\""),
    ("with backslash", s"a${XmlBackslash}b", """a\b"""),
    ("with two backslashes", s"a${XmlBackslash}b${XmlBackslash}c", """a\b\c"""),
    ("starting with backslash", s"${XmlBackslash}b", """\b"""),
    ("ending with backslash", s"a$XmlBackslash", """a\"""),
    ("starting and ending with backslashes", s"${XmlBackslash}a$XmlBackslash", """\a\"""),
    ("with only backslash", XmlBackslash, """\"""),
    ("with only two backslashes", s"$XmlBackslash$XmlBackslash", """\\"""),
    ("with only three backslashes", s"$XmlBackslash$XmlBackslash$XmlBackslash", """\\\"""),
    ("with only four backslashes", s"$XmlBackslash$XmlBackslash$XmlBackslash$XmlBackslash", """\\\\"""),
    ("with quotation mark then backslash", s"a${XmlQuote}b${XmlBackslash}c", """a"b\c"""),
    ("with backslash then quotation mark", s"a${XmlBackslash}b${XmlQuote}c", """a\b"c"""),
    ("with quotation mark/backslash alt 1", s"$XmlQuote$XmlBackslash$XmlQuote", "\"\\\""),
    ("with quotation mark/backslash alt 2", s"$XmlBackslash$XmlQuote$XmlBackslash", "\\\"\\"))

  jsonStringTests.foreach { case (testCase, xmlElement, expectedJsonValue) =>
    test(s"should transform element $testCase properly") {
      val xml =
        s"""<?xml version="1.0"?>
           |<entry xmlns="http://www.w3.org/2005/Atom">
           |  <title>$xmlElement</title>
           |  <category term="DFW2"/>
           |  <category term="6093728"/>
           |  <category term="trove.instance.exists"/>
           |  <content type="application/xml">
           |    <event xmlns="http://docs.rackspace.com/core/event" xmlns:dbaas="http://docs.rackspace.com/usage/dbaas" dataCenter="DFW2"
           |           startTime="2014-08-06T21:11:55Z"
           |           endTime="2014-08-06T22:11:55Z"
           |           environment="QA" id="170d03b4-9582-4e34-80fb-d01911ef6469" region="DFW" resourceId="88939024-bf82-49ec-a177-d7ca55d60c9d" resourceName="ba858553-30f4-4f69-a65d-6addc5eafef5" rootAction="trove.instance.exists"
           |           tenantId="6093728" type="USAGE" version="1">
           |      <dbaas:product dbVersion="1.0" memory="32768" resourceType="MARIADB" serviceCode="CloudDatabase" storage="0" version="2"/>
           |    </event>
           |  </content>
           |</entry>
          """.stripMargin

      val jsonResult = transform(new StreamSource(new StringReader(xml)))
      jsonResult should not be null withClue s". The XML could not be transformed:\n$xml"

      val jsonParsed = JSON.parseFull(jsonResult)
      jsonParsed shouldBe defined withClue s". The transform generated JSON could not be parsed:\n$jsonResult"
      val jsonObjects = jsonParsed.get.asInstanceOf[Map[String, Any]]

      jsonObjects should contain key "entry"
      val entryObject = jsonObjects("entry").asInstanceOf[Map[String, Any]]

      entryObject should contain key "title"
      entryObject("title") shouldBe expectedJsonValue
    }
  }

  jsonStringTests.foreach { case (testCase, xmlElement, expectedJsonValue) =>
    test(s"should transform text element $testCase properly") {
      val xml =
        s"""<?xml version="1.0"?>
           |<entry xmlns="http://www.w3.org/2005/Atom">
           |  <title type="text">$xmlElement</title>
           |  <category term="DFW2"/>
           |  <category term="6093728"/>
           |  <category term="trove.instance.exists"/>
           |  <content type="application/xml">
           |    <event xmlns="http://docs.rackspace.com/core/event" xmlns:dbaas="http://docs.rackspace.com/usage/dbaas" dataCenter="DFW2"
           |           startTime="2014-08-06T21:11:55Z"
           |           endTime="2014-08-06T22:11:55Z"
           |           environment="QA" id="170d03b4-9582-4e34-80fb-d01911ef6469" region="DFW" resourceId="88939024-bf82-49ec-a177-d7ca55d60c9d" resourceName="ba858553-30f4-4f69-a65d-6addc5eafef5" rootAction="trove.instance.exists"
           |           tenantId="6093728" type="USAGE" version="1">
           |      <dbaas:product dbVersion="1.0" memory="32768" resourceType="MARIADB" serviceCode="CloudDatabase" storage="0" version="2"/>
           |    </event>
           |  </content>
           |</entry>
          """.stripMargin

      val jsonResult = transform(new StreamSource(new StringReader(xml)))
      jsonResult should not be null withClue s". The XML could not be transformed:\n$xml"

      val jsonParsed = JSON.parseFull(jsonResult)
      jsonParsed shouldBe defined withClue s". The transform generated JSON could not be parsed:\n$jsonResult"
      val jsonObjects = jsonParsed.get.asInstanceOf[Map[String, Any]]

      jsonObjects should contain key "entry"
      val entryObject = jsonObjects("entry").asInstanceOf[Map[String, Any]]

      entryObject should contain key "title"
      val titleObject = entryObject("title").asInstanceOf[Map[String, Any]]

      titleObject should contain key "@text"
      titleObject("@text") shouldBe expectedJsonValue
    }
  }

  jsonStringTests.foreach { case (testCase, xmlAttribute, expectedJsonValue) =>
    test(s"should transform category with term attribute $testCase properly") {
      val xml =
        s"""<?xml version="1.0"?>
           |<entry xmlns="http://www.w3.org/2005/Atom">
           |  <title>trove.instance.exists</title>
           |  <category term="DFW2"/>
           |  <category term="$xmlAttribute"/>
           |  <category term="trove.instance.exists"/>
           |  <content type="application/xml">
           |    <event xmlns="http://docs.rackspace.com/core/event" xmlns:dbaas="http://docs.rackspace.com/usage/dbaas" dataCenter="DFW2"
           |           startTime="2014-08-06T21:11:55Z"
           |           endTime="2014-08-06T22:11:55Z"
           |           environment="QA" id="170d03b4-9582-4e34-80fb-d01911ef6469" region="DFW" resourceId="88939024-bf82-49ec-a177-d7ca55d60c9d" resourceName="ba858553-30f4-4f69-a65d-6addc5eafef5" rootAction="trove.instance.exists"
           |           tenantId="6093728" type="USAGE" version="1">
           |      <dbaas:product dbVersion="1.0" memory="32768" resourceType="MARIADB" serviceCode="CloudDatabase" storage="0" version="2"/>
           |    </event>
           |  </content>
           |</entry>
          """.stripMargin

      val jsonResult = transform(new StreamSource(new StringReader(xml)))
      jsonResult should not be null withClue s". The XML could not be transformed:\n$xml"

      val jsonParsed = JSON.parseFull(jsonResult)
      jsonParsed shouldBe defined withClue s". The transform generated JSON could not be parsed:\n$jsonResult"
      val jsonObjects = jsonParsed.get.asInstanceOf[Map[String, Any]]

      jsonObjects should contain key "entry"
      val entryObject = jsonObjects("entry").asInstanceOf[Map[String, Any]]

      entryObject should contain key "category"
      val categoryObjects = entryObject("category").asInstanceOf[List[Map[String, Any]]]
      categoryObjects.size should be >= 3
      categoryObjects should contain (Map("term" -> expectedJsonValue))
    }
  }

  jsonStringTests.foreach { case (testCase, xmlAttribute, expectedJsonValue) =>
    test(s"should transform product attribute $testCase properly") {
      val xml =
        s"""<?xml version="1.0"?>
           |<entry xmlns="http://www.w3.org/2005/Atom">
           |  <title>trove.instance.exists</title>
           |  <category term="DFW2"/>
           |  <category term="6093728"/>
           |  <category term="trove.instance.exists"/>
           |  <content type="application/xml">
           |    <event xmlns="http://docs.rackspace.com/core/event" xmlns:dbaas="http://docs.rackspace.com/usage/dbaas" dataCenter="DFW2"
           |           startTime="2014-08-06T21:11:55Z"
           |           endTime="2014-08-06T22:11:55Z"
           |           environment="QA" id="170d03b4-9582-4e34-80fb-d01911ef6469" region="DFW" resourceId="88939024-bf82-49ec-a177-d7ca55d60c9d" resourceName="ba858553-30f4-4f69-a65d-6addc5eafef5" rootAction="trove.instance.exists"
           |           tenantId="6093728" type="USAGE" version="1">
           |      <dbaas:product dbVersion="1.0" memory="32768" resourceType="$xmlAttribute" serviceCode="CloudDatabase" storage="0" version="2"/>
           |    </event>
           |  </content>
           |</entry>
          """.stripMargin

      val jsonResult = transform(new StreamSource(new StringReader(xml)))
      jsonResult should not be null withClue s". The XML could not be transformed:\n$xml"

      val jsonParsed = JSON.parseFull(jsonResult)
      jsonParsed shouldBe defined withClue s". The transform generated JSON could not be parsed:\n$jsonResult"
      val jsonObjects = jsonParsed.get.asInstanceOf[Map[String, Any]]

      jsonObjects should contain key "entry"
      val entryObject = jsonObjects("entry").asInstanceOf[Map[String, Any]]

      entryObject should contain key "content"
      val contentObject = entryObject("content").asInstanceOf[Map[String, Any]]

      contentObject should contain key "event"
      val eventObject = contentObject("event").asInstanceOf[Map[String, Any]]

      eventObject should contain key "product"
      val productObject = eventObject("product").asInstanceOf[Map[String, Any]]

      productObject should contain key "resourceType"
      productObject("resourceType") shouldBe expectedJsonValue
    }
  }

  test("should transform XML identity user access event into JSON properly") {
    val parser = new JsonParserFactory().createFastParser()

    val transformResult = transform(new StreamSource(new File("message_samples/corexsd/xml/identity-user-access-event.xml")))
    val transformMap = parser.parseMap(new ByteArrayInputStream(transformResult.getBytes(StandardCharsets.UTF_8)))

    // assert auditData/region/text()
    assert(transformMap.get("entry").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("content").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("event").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("attachments").asInstanceOf[java.util.List[Map[String, AnyRef]]]
      .get(0).asInstanceOf[java.util.Map[String, AnyRef]]
      .get("content").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("auditData").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("region") == "DFW")
  }

  test("should transform Feeds Catalog with Prefs Svc workspace to JSON properly") {

    val transformResult = transform(new StreamSource(new File("src/test/resources/feedscatalog.xml")))

    val jsonObj = JSON.parseFull(transformResult).get.asInstanceOf[Map[String, Any]]
    assert(jsonObj != null, "should have a valid json object")

    val serviceObj = jsonObj("service").asInstanceOf[Map[String, Any]]
    assert(serviceObj != null, "should have a service object")

    val workspaces = serviceObj("workspace").asInstanceOf[List[Map[String, AnyRef]]]
    assert(workspaces != null, "workspaces should not be null")
    assert(workspaces.nonEmpty, "should have at least one workspaces")

    val lastWorkspace = workspaces.last
    assert(lastWorkspace != null, "last workspace should not be null")

    val links = lastWorkspace("link").asInstanceOf[List[AnyRef]]
    assert(links != null && links.size == 2, "must have 2 links")
    assert(lastWorkspace("title").asInstanceOf[String] == "Feeds Archiving Preferences Service endpoints")
  }

  test("should transform CADF with multiple attachments into JSON properly") {
    val parser = new JsonParserFactory().createFastParser()

    val transformResult = transform(new StreamSource(new File("src/test/resources/cadf-multiple-attachments.xml")))
    val transformMap = parser.parseMap(new ByteArrayInputStream(transformResult.getBytes(StandardCharsets.UTF_8)))

    // assert auditData/region/text()
    assert(transformMap.get("entry").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("content").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("event").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("attachments").asInstanceOf[java.util.List[Map[String, AnyRef]]].size() == 2)
  }

  test("should transform XML containing event node with random namespace") {
    val parser = new JsonParserFactory().createFastParser()

    val transformResult = transform(new StreamSource(new File("src/test/resources/event-node-random-ns.xml")))
    val transformMap = parser.parseMap(new ByteArrayInputStream(transformResult.getBytes(StandardCharsets.UTF_8)))

    assert(transformMap.get("entry").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("content").asInstanceOf[java.util.Map[String, AnyRef]]
      .get("event").asInstanceOf[java.util.Map[String, AnyRef]] != null)
  }

  def transform(inputXML: StreamSource): String = {
    val trans = templates.newTransformer()
    val writer = new StringWriter()

    trans.setParameter("schemaDirectory", "./../../../sample_product_schemas")
    trans.asInstanceOf[Controller].setInitialTemplate("main")
    trans.transform(inputXML, new StreamResult(writer))

    writer.toString
  }

  val archiveFeedsList = List(
    ("archive node at the beginning should transform feeds archive element into JSON properly",
      """<?xml version="1.0" encoding="UTF-8" ?>
        <feed xmlns="http://www.w3.org/2005/Atom"
              xmlns:fh="http://purl.org/syndication/history/1.0" >
          <fh:archive />
          <link rel="current" href="http://livefeed1/feed1/1234"/>
          <id>uuid1</id>
          <title type="text">feed1</title>
          <updated>TIME</updated>
          <atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary" xmlns:wadl="http://wadl.dev.java.net/2009/02" xmlns:d312e1="http://wadl.dev.java.net/2009/02" xmlns:error="http://docs.rackspace.com/core/error" xmlns:db="http://docbook.org/ns/docbook" xmlns="http://wadl.dev.java.net/2009/02" xmlns:atom="http://www.w3.org/2005/Atom">
            <atom:id>urn:uuid:560490c6-6c63-11e1-adfe-27851d5aed43</atom:id>
            <atom:category term="metaData.key:foo"/>
            <atom:content type="application/xml">
              <event xmlns:widget="http://docs.rackspace.com/usage/widget" xmlns="http://docs.rackspace.com/core/event" version="1" type="UPDATE" tenantId="12334" startTime="2012-03-12T11:51:11Z" resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549" region="DFW" id="560490c6-6c63-11e1-adfe-27851d5aed43" environment="PROD" endTime="2012-03-12T15:51:11Z" dataCenter="DFW1">
                <widget:product widgetOnlyAttribute="bar" version="3" serviceCode="Widget" resourceType="WIDGET" mid="e9a67860-52e6-11e3-a0d1-002500a28a7a" label="test">
                  <widget:metaData key="foo"/>
                  <widget:mixPublicPrivateAttributes myAttribute="here it should be public"/>
                </widget:product>
              </event>
            </atom:content>
            <atom:updated>2014-02-18T21:12:10.997Z</atom:updated>
            <atom:published>2014-02-18T21:12:10.997Z</atom:published>
          </atom:entry>
        
          </feed>"""
    ),
    ("archive node at the end should transform feeds archive element into JSON properly",
      """<?xml version="1.0" encoding="UTF-8" ?>
        <feed xmlns="http://www.w3.org/2005/Atom"
              xmlns:fh="http://purl.org/syndication/history/1.0" >
          <link rel="current" href="http://livefeed1/feed1/1234"/>
          <id>uuid1</id>
          <title type="text">feed1</title>
          <updated>TIME</updated>
          <atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary" xmlns:wadl="http://wadl.dev.java.net/2009/02" xmlns:d312e1="http://wadl.dev.java.net/2009/02" xmlns:error="http://docs.rackspace.com/core/error" xmlns:db="http://docbook.org/ns/docbook" xmlns="http://wadl.dev.java.net/2009/02" xmlns:atom="http://www.w3.org/2005/Atom">
            <atom:id>urn:uuid:560490c6-6c63-11e1-adfe-27851d5aed43</atom:id>
            <atom:category term="metaData.key:foo"/>
            <atom:content type="application/xml">
              <event xmlns:widget="http://docs.rackspace.com/usage/widget" xmlns="http://docs.rackspace.com/core/event" version="1" type="UPDATE" tenantId="12334" startTime="2012-03-12T11:51:11Z" resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549" region="DFW" id="560490c6-6c63-11e1-adfe-27851d5aed43" environment="PROD" endTime="2012-03-12T15:51:11Z" dataCenter="DFW1">
                <widget:product widgetOnlyAttribute="bar" version="3" serviceCode="Widget" resourceType="WIDGET" mid="e9a67860-52e6-11e3-a0d1-002500a28a7a" label="test">
                  <widget:metaData key="foo"/>
                  <widget:mixPublicPrivateAttributes myAttribute="here it should be public"/>
                </widget:product>
              </event>
            </atom:content>
            <atom:updated>2014-02-18T21:12:10.997Z</atom:updated>
            <atom:published>2014-02-18T21:12:10.997Z</atom:published>
          </atom:entry>
          <fh:archive />        
          </feed>"""
    ),
    ("archive node at the middle should transform feeds archive element into JSON properly",
      """<feed xmlns="http://www.w3.org/2005/Atom"
            xmlns:fh="http://purl.org/syndication/history/1.0" >       
        <link rel="current" href="http://livefeed1/feed1/1234"/>
        <id>uuid2</id>
        <title type="text">feed1</title>
        <updated>TIME</updated>
        <fh:archive />
        <atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary" xmlns:wadl="http://wadl.dev.java.net/2009/02" xmlns:d312e1="http://wadl.dev.java.net/2009/02" xmlns:error="http://docs.rackspace.com/core/error" xmlns:db="http://docbook.org/ns/docbook" xmlns="http://wadl.dev.java.net/2009/02" xmlns:atom="http://www.w3.org/2005/Atom">
          <atom:id>urn:uuid:560490c6-6c63-11e1-adfe-27851d5aed43</atom:id>
          <atom:category term="tid:12334"/>
          <atom:content type="application/xml">
            <event xmlns:widget="http://docs.rackspace.com/usage/widget" xmlns="http://docs.rackspace.com/core/event" version="1" type="UPDATE" tenantId="12334" startTime="2012-03-12T11:51:11Z" resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549" region="DFW" id="560490c6-6c63-11e1-adfe-27851d5aed43" environment="PROD" endTime="2012-03-12T15:51:11Z" dataCenter="DFW1">
              <widget:product widgetOnlyAttribute="bar" version="3" serviceCode="Widget" resourceType="WIDGET" mid="e9a67860-52e6-11e3-a0d1-002500a28a7a" label="test">
                <widget:metaData key="foo"/>
                <widget:mixPublicPrivateAttributes myAttribute="here it should be public"/>
              </widget:product>
            </event>
          </atom:content>
          <atom:updated>2014-02-18T21:12:10.997Z</atom:updated>
          <atom:published>2014-02-18T21:12:10.997Z</atom:published>
        </atom:entry>
        </feed>""")
  )

  archiveFeedsList.foreach(input => {
    val (title, xml) = input
    test(title) {
      val parser = new JsonParserFactory().createFastParser()

      val transformResult = transform(new StreamSource(new StringReader(xml)))
      val transformMap = parser.parseMap(new ByteArrayInputStream(transformResult.getBytes(StandardCharsets.UTF_8)))

      // assert auditData/region/text()
      assert(transformMap.get("feed").asInstanceOf[java.util.Map[String, AnyRef]]
        .get("archive").asInstanceOf[java.util.Map[String, AnyRef]]
        .get("@type") == "http://purl.org/syndication/history/1.0")
    }
  })
}

object Xml2JsonSuite {
  val XmlQuote = "&quot;"
  val XmlBackslash = "\\"
}