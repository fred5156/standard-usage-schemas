<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:html="http://www.w3.org/1999/xhtml"
    xmlns:schema="http://docs.rackspace.com/core/usage/schema"
    xmlns:usage="http://docs.rackspace.com/core/usage"
    xmlns:xerces="http://xerces.apache.org"
    xmlns:saxon="http://saxon.sf.net/"
    xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
    xmlns="http://www.w3.org/2001/XMLSchema"
    xmlns:sum="http://docs.rackspace.com/core/usage/schema/summary"
    exclude-result-prefixes="schema"
    version="2.0">

    <xsl:import href="productSchema-summary-util.xsl"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="MAX_STRING">255</xsl:variable>
    <xsl:variable name="MAX_OCCURS_VALUE">5000</xsl:variable>

    <xsl:template match="/">
        <xsl:call-template name="genXSD">
            <xsl:with-param name="schemas" select="//schema:productSchema"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="genXSD">
        <xsl:param name="schemas" as="item()*"/>
        <xsl:variable name="isUsage" as="xsd:boolean"
                      select="not(empty(for $s in tokenize(string-join($schemas/@type,' '),' ') return if ($s = ('USAGE', 'USAGE_SNAPSHOT')) then true() else ()))"/>
        <xsl:comment>THIS SCHEMA IS AUTOGENERATED DO NOT EDIT</xsl:comment>
        <xsl:text>&#x0a;</xsl:text>
        <xsd:schema
            elementFormDefault="qualified"
            attributeFormDefault="unqualified"
            xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:html="http://www.w3.org/1999/xhtml"
            xmlns:xerces="http://xerces.apache.org"
            xmlns:saxon="http://saxon.sf.net/"
            xmlns="http://www.w3.org/2001/XMLSchema">
            <xsl:namespace name="p" select="$schemas[1]/@namespace"/>
            <xsl:attribute name="targetNamespace">
                <xsl:value-of select="$schemas[1]/@namespace"/>
            </xsl:attribute>

            <xsl:choose>
                <xsl:when test="(count($schemas) = 1) and not($isUsage)">
                    <xsl:call-template name="setupSingleProduct">
                        <xsl:with-param name="schemas" select="$schemas"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="setupMultiProduct">
                        <xsl:with-param name="schemas" select="$schemas"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>

            <xsl:if test="$schemas//schema:attribute[@type=('UUID', 'UUID*')]">
                <simpleType name="UUID">
                    <restriction base="xsd:string">
                        <length value="36" fixed="true"/>
                        <pattern>
                            <xsl:attribute name="value">[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}</xsl:attribute>
                        </pattern>
                    </restriction>
                </simpleType>
            </xsl:if>
            <xsl:if test="$schemas//schema:attribute[@type=('utcDateTime', 'utcDateTime*')]">
                <simpleType name="UTCDateTime">
                    <restriction base="xsd:dateTime" vc:minVersion="1.0" vc:maxVersion="1.1"/>
                    <restriction base="xsd:dateTimeStamp" vc:minVersion="1.1">
                        <assertion
                            test="ends-with(string($value),'Z') or ends-with(string($value),'+00:00') or ends-with(string($value),'-00:00')"
                            xerces:message="The dateTime should be in the UTC timezone, it is expect to end in +00:00 or Z."
                            saxon:message="The dateTime should be in the UTC timezone, it is expect to end in +00:00 or Z."/>
                    </restriction>
                </simpleType>
            </xsl:if>
            <xsl:if test="$schemas//schema:attribute[@type=('utcTime', 'utcTime*')]">
                <simpleType name="UTCTime">
                    <restriction base="xsd:time">
                        <assertion
                            vc:minVersion="1.1"
                            test="ends-with(string($value),'Z') or ends-with(string($value),'+00:00') or ends-with(string($value),'-00:00')"
                            xerces:message="The time should be in the UTC timezone, it is expect to end in +00:00 or Z."
                            saxon:message="The time should be in the UTC timezone, it is expect to end in +00:00 or Z."/>
                    </restriction>
                </simpleType>
            </xsl:if>
            <xsl:for-each-group select="$schemas//schema:attribute[@type=('string', 'string*') and @maxLength and not(@allowedValues)]" group-by="concat(concat(@minLength, '_'), @maxLength)">
                <xsl:variable name="maxLengthValue" as="xsd:integer" select="./@maxLength" />
                <xsl:variable name="minLengthValue">
                    <xsl:choose>
                        <xsl:when test="./@minLength"><xsl:value-of select="./@minLength"/></xsl:when>
                        <xsl:otherwise>0</xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <simpleType name="string{$minLengthValue}_{$maxLengthValue}">
                    <restriction base="xsd:string">
                        <minLength value="{$minLengthValue}"/>
                        <maxLength value="{$maxLengthValue}"/>
                    </restriction>
                </simpleType>
            </xsl:for-each-group>
            <xsl:if test="$schemas//schema:attribute[@type=('string', 'string*') and not(@maxLength) and not(@allowedValues)]">
                <xsl:variable name="minLengthValue">
                    <xsl:choose>
                        <xsl:when test="./@minLength"><xsl:value-of select="./@minLength"/></xsl:when>
                        <xsl:otherwise>0</xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <simpleType name="string">
                    <restriction base="xsd:string">
                        <minLength value="{$minLengthValue}"/>
                        <maxLength value="{$MAX_STRING}"/>
                    </restriction>
                </simpleType>
            </xsl:if>
            <xsl:if test="$schemas//schema:attribute[@type=('Name','Name*') and not(@allowedValues)]">
                <simpleType name="Name">
                    <restriction base="xsd:Name">
                        <maxLength value="{$MAX_STRING}"/>
                    </restriction>
                </simpleType>
            </xsl:if>
        </xsd:schema>
    </xsl:template>

    <xsl:template name="setupSingleProduct">
        <xsl:param name="schemas" as="item()*"/>
        <element name="product">
            <xsl:attribute name="type">
                <xsl:value-of select="concat('p:',$schemas[1]/@serviceCode,'Type')"/>
            </xsl:attribute>
        </element>
        <xsl:apply-templates select="$schemas">
            <xsl:with-param name="use-version" tunnel="yes" select="false()"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template name="setupMultiProduct">
        <xsl:param name="schemas" as="item()*"/>
        <xsl:variable name="baseType" as="xsd:string" select="usage:baseTypeName($schemas[1])"/>
        <xsl:variable name="errorType" as="xsd:string" select="concat($baseType,'Error')"/>
        <xsl:variable name="types" as="xsd:string*" select="for $s in $schemas return concat('p:',$s/@serviceCode,$s/@version,'Type')"/>
        <xsl:variable name="versions" as="xsd:string*" select="for $s in $schemas return $s/@version"/>
        <xsl:variable name="usageSchemas" as="item()*" select="for $s in $schemas return if (tokenize($s/@type,' ') = ('USAGE','USAGE_SNAPSHOT')) then $s else ()"
                      use-when="not(system-property('xsl:is-schema-aware'))"/>
        <xsl:variable name="usageSchemas" as="item()*" select="for $s in $schemas return if ($s/@type = ('USAGE','USAGE_SNAPSHOT')) then $s else ()"
                      use-when="system-property('xsl:is-schema-aware')"/>
        <xsl:variable name="usageSummaryTypes" as="xsd:string*" select="for $s in $usageSchemas return concat('p:',$s/@serviceCode,$s/@version,'SummaryType')"/>
        <xsl:variable name="usageSummaryVersions" as="xsd:string*" select="for $s in $usageSchemas return $s/@version"/>
        <element name="product" type="{$types[position() = last()]}" vc:minVersion="1.0" vc:maxVersion="1.1">
            <annotation>
                <documentation>
                    <html:p>
                        Alternates are not supported by XSD 1.0.  So
                        in XSD 1.0 we are only supporting a single
                        version (in this case version <xsl:value-of
                        select="$versions[position() = last()]"/>).
                    </html:p>
                    <html:p>
                        To support other versions you may redefine the
                        product element to any of the following types:
                    </html:p>
                    <html:ol>
                        <xsl:for-each select="($types, $usageSummaryTypes)">
                            <html:li><xsl:value-of select="."/></html:li>
                        </xsl:for-each>
                    </html:ol>
                </documentation>
            </annotation>
        </element>
        <element name="product" vc:minVersion="1.1">
            <xsl:attribute name="type">
                <xsl:value-of select="concat('p:',$baseType)"/>
            </xsl:attribute>
            <xsl:for-each select="$types">
                <xsl:variable name="pos" select="position()"/>
                <alternative test="(@version eq '{$versions[$pos]}') and not(@summary)" type="{.}"/>
            </xsl:for-each>
            <xsl:for-each select="$usageSummaryTypes">
                <xsl:variable name="pos" select="position()"/>
                <alternative test="(@version eq '{$usageSummaryVersions[$pos]}') and @summary" type="{.}"/>
            </xsl:for-each>
            <alternative type="p:{$errorType}"/>
        </element>
        <complexType name="{$baseType}">
            <attribute name="version" type="xsd:string" use="required"/>
            <attribute name="serviceCode" use="required" type="xsd:Name" fixed="{$schemas[1]/@serviceCode}"/>
        </complexType>
        <complexType name="{$errorType}" vc:minVersion="1.1">
            <xsl:variable name="errorMsg" as="xsd:string*">
                <xsl:text>For this type of message, only  versions (</xsl:text>
                <xsl:value-of select="$versions" separator=", "/>
                <xsl:text>) are supported.</xsl:text>
            </xsl:variable>
            <complexContent>
                <extension base="p:{$baseType}">
                    <openContent mode="interleave">
                        <any processContents="skip"/>
                    </openContent>
                    <anyAttribute processContents="skip"/>
                    <assert test="false()" xerces:message="{$errorMsg}" saxon:message="{$errorMsg}"/>
                </extension>
            </complexContent>
        </complexType>
        <xsl:apply-templates select="$schemas">
            <xsl:with-param name="use-version" tunnel="yes" select="true()"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="$usageSchemas">
            <xsl:with-param name="use-version"   tunnel="yes" select="true()"/>
            <xsl:with-param name="usage-summary" tunnel="yes" select="true()"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="schema:productSchema">
        <xsl:param name="use-version" as="xsd:boolean" tunnel="yes" select="false()"/>
        <xsl:param name="usage-summary" as="xsd:boolean" tunnel="yes" select="false()"/>
        <xsl:variable name="resourceTypes" as="xsd:string*" select="tokenize(@resourceTypes, ' ')" use-when="not(system-property('xsl:is-schema-aware'))"/>
        <xsl:variable name="resourceTypes" as="xsd:string*" select="@resourceTypes" use-when="system-property('xsl:is-schema-aware')"/>
        <complexType>
            <xsl:attribute name="name">
                <xsl:choose>
                    <xsl:when test="$usage-summary">
                        <xsl:value-of select="concat(@serviceCode,@version,'SummaryType')"/>
                    </xsl:when>
                    <xsl:when test="$use-version">
                        <xsl:value-of select="concat(@serviceCode,@version,'Type')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat(@serviceCode,'Type')"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <annotation>
                <documentation>
                    <html:p>
                        <xsl:if test="$usage-summary">This is an auto generated usage summary message. </xsl:if>
                        <xsl:value-of select="normalize-space(schema:description)"/>
                    </html:p>
                </documentation>
                <appinfo>
                    <usage:core>
                        <xsl:if test="@groupByResource">
                            <xsl:attribute name="groupByResource" select="@groupByResource"/>
                        </xsl:if>
                        <xsl:if test="@ranEnrichmentStrategy">
                            <xsl:attribute name="ranEnrichmentStrategy" select="@ranEnrichmentStrategy"/>
                        </xsl:if>
                        <xsl:choose>
                            <xsl:when test="$usage-summary">
                                <xsl:attribute name="type" select="'USAGE_SUMMARY'"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:if test="@type">
                                    <xsl:attribute name="type" select="@type"/>
                                </xsl:if>
                            </xsl:otherwise>
                        </xsl:choose>
                    </usage:core>
                    <xsl:apply-templates mode="AddAggregationPeriods"/>
                </appinfo>
            </annotation>
            <xsl:choose>
                <xsl:when test="$use-version">
                    <complexContent>
                        <extension base="p:{usage:baseTypeName(.)}">
                            <xsl:call-template name="buildSchemaComplexType"/>
                        </extension>
                    </complexContent>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="buildSchemaComplexType"/>
                </xsl:otherwise>
            </xsl:choose>
        </complexType>
        <xsl:if test="@resourceTypes and not($usage-summary)">
            <simpleType name="{usage:resourceTypeName(., $use-version, @version)}">
                <annotation>
                    <documentation>
                        <html:p>Resource Types for this product.</html:p>
                    </documentation>
                </annotation>
                <restriction base="xsd:token">
                    <xsl:for-each select="$resourceTypes">
                        <enumeration>
                            <xsl:attribute name="value">
                                <xsl:value-of select="."/>
                            </xsl:attribute>
                        </enumeration>
                    </xsl:for-each>
                </restriction>
            </simpleType>
        </xsl:if>
        <xsl:apply-templates mode="AddTypes">
            <xsl:with-param name="version" tunnel="yes" select="@version"/>
            <xsl:with-param name="usage-summary" tunnel="yes" select="$usage-summary"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template name="buildSchemaComplexType">
        <xsl:param name="use-version" as="xsd:boolean" tunnel="yes" select="false()"/>
        <xsl:param name="usage-summary" as="xsd:boolean" tunnel="yes" select="false()"/>
        <xsl:call-template name="addAttributeGroups">
            <xsl:with-param name="version" tunnel="yes" select="@version"/>
        </xsl:call-template>
        <xsl:if test="not($use-version)">
            <attribute name="version" type="xsd:string" use="required">
                <xsl:attribute name="fixed" select="@version"/>
            </attribute>
            <attribute name="serviceCode" use="required" type="xsd:Name" fixed="{@serviceCode}"/>
        </xsl:if>
        <xsl:if test="@resourceTypes">
            <attribute name="resourceType"  use="required" type="p:{usage:resourceTypeName(., $use-version, @version)}"/>
        </xsl:if>
        <xsl:if test="$usage-summary">
            <attribute name="summary" use="required" type="xsd:boolean" fixed="true"/>
        </xsl:if>
        <xsl:apply-templates>
            <xsl:with-param name="version" tunnel="yes" select="@version"/>
        </xsl:apply-templates>
        <xsl:apply-templates mode="assertions"/>
    </xsl:template>
    <xsl:template match="schema:attributeGroup">
        <!-- Ignore attributes in attribute group in this mode -->
    </xsl:template>
    <xsl:template match="schema:attributeGroup" mode="AddAttributeGroups">
        <element>
            <xsl:copy-of select="(@name, @minOccurs)"/>
            <xsl:choose>
                <xsl:when test="string(@maxOccurs) = 'unbounded'">
                    <xsl:attribute name="maxOccurs" select="$MAX_OCCURS_VALUE"/>
                </xsl:when>
                <xsl:when test="@maxOccurs">
                    <xsl:copy-of select="@maxOccurs"/>
                </xsl:when>
            </xsl:choose>
            <complexType>
                <xsl:apply-templates mode="AddAttributeGroups"/>
            </complexType>
        </element>
    </xsl:template>
    <xsl:template match="schema:aggregationPeriods">
        <!-- Ignore aggregationPeriods in this mode -->
    </xsl:template>
    <xsl:template match="schema:aggregationPeriods" mode="AddAggregationPeriods">
        <usage:aggregationPeriods>
            <xsl:apply-templates mode="AddAggregationPeriod"/>
        </usage:aggregationPeriods>
    </xsl:template>
    <xsl:template match="schema:aggregationPeriod">
        <!-- Ignore aggregationPeriod in this mode -->
    </xsl:template>
    <xsl:template match="schema:aggregationPeriod" mode="AddAggregationPeriod">
        <usage:aggregationPeriod><xsl:value-of select="."/></usage:aggregationPeriod>
    </xsl:template>
    <xsl:function name="schema:getSchemaType" as="xsd:string">
        <xsl:param name="type" as="xsd:string" />
        <xsl:param name="attribute" as="node()"/>
        <xsl:param name="use-version" as="xsd:boolean"/>
        <xsl:param name="usage-summary" as="xsd:boolean"/>
        <xsl:param name="version" as="xsd:string"/>

        <xsl:variable name="vname" select="usage:versionName($attribute/@name, $use-version, $version)"/>
        <xsl:variable name="minLengthValue">
            <xsl:choose>
                <xsl:when test="$attribute/@minLength"><xsl:value-of select="$attribute/@minLength"/></xsl:when>
                <xsl:otherwise>0</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="ends-with($type, '*')">
                <xsl:value-of select="usage:listNameType($vname,true())"/>
            </xsl:when>
            <xsl:when test="$attribute/@allowedValues">
                <xsl:value-of select="usage:enumNameType($vname, true())"/>
            </xsl:when>
            <xsl:when test="$type='UUID'">
                <xsl:value-of select="'p:UUID'"/>
            </xsl:when>
            <xsl:when test="$type='utcDateTime'">
                <xsl:value-of select="'p:UTCDateTime'"/>
            </xsl:when>
            <xsl:when test="$type='utcTime'">
                <xsl:value-of select="'p:UTCTime'"/>
            </xsl:when>
            <xsl:when test="$type='string' and $attribute/@maxLength">
                <xsl:value-of select="concat('p:',$type,$minLengthValue,'_',$attribute/@maxLength)"/>
            </xsl:when>
            <xsl:when test="$type='string' and not($attribute/@maxLength)">
                <xsl:value-of select="concat('p:',$type)"/>
            </xsl:when>
            <xsl:when test="$type='Name'">
                <xsl:value-of select="'p:Name'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <!--
                       The following test is XSD specific in regards to defining specialized types which accomadate
                       min and max values.  Since this doesn't impact the base type, it doesn't impact documenation
                       and isn't included in productSchema-summary-util.xsl
                     -->
                    <xsl:when test="($attribute/@min or $attribute/@max) and not($usage-summary and $attribute/@aggregateFunction = ('SUM','DAILY_WEIGHTED_SUM') and $type = ( 'double', 'int', 'long', 'short', 'byte', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'unsignedByte'))">
                        <xsl:value-of select="usage:minMaxType($vname,true(),$usage-summary and $attribute/@aggregateFunction=('SUM','DAILY_WEIGHTED_SUM','AVG','WEIGHTED_AVG'))"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="sum:getTypeXSD( $type, $usage-summary, $attribute/@aggregateFunction )"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:template match="schema:attribute" mode="#default AddAttributeGroups">
        <xsl:param name="use-version" as="xsd:boolean" tunnel="yes" select="false()"/>
        <xsl:param name="usage-summary" as="xsd:boolean" tunnel="yes" select="false()"/>
        <xsl:param name="version" as="xsd:string" tunnel="yes"/>

        <xsl:variable name="types" as="xsd:string*" select="tokenize(@type, ' ')" use-when="not(system-property('xsl:is-schema-aware'))"/>
        <xsl:variable name="types" as="xsd:string*" select="@type" use-when="system-property('xsl:is-schema-aware')"/>
        <xsl:variable name="rtypes" as="xsd:string*" select="for $rt in $types return schema:getSchemaType($rt, ., $use-version, $usage-summary, $version)"/>
        <attribute>
            <xsl:attribute name="name" select="@name"/>
            <xsl:if test="@use">
                <xsl:choose>
                    <xsl:when test="@use='synthesized' and not($usage-summary)">
                        <xsl:attribute name="use">optional</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="@use='synthesized' and $usage-summary">
                        <xsl:attribute name="use">required</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="@use='required' and (@withEventType or @withResource)">
                       <xsl:attribute name="use">optional</xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="use" select="sum:getOptionality(@use, $usage-summary, @aggregateFunction, @groupBy)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="@fixed">
                <xsl:attribute name="fixed" select="@fixed"/>
            </xsl:if>
            <xsl:if test="@default">
                <xsl:attribute name="default" select="@default"/>
            </xsl:if>
            <xsl:if test="xsd:boolean(@public)">
                <xsl:attribute name="public" select="@public"/>
            </xsl:if>
            <xsl:if test="count($types) = 1">
                <xsl:attribute name="type" select="$rtypes[1]" />
            </xsl:if>
            <annotation>
                <documentation>
                    <html:p>
                        <xsl:value-of select="normalize-space(.)"/>
                    </html:p>
                </documentation>
                <appinfo>
                    <usage:attributes>
                        <xsl:if test="@displayName">
                            <xsl:attribute name="displayName" select="@displayName"/>
                        </xsl:if>
                        <xsl:if test="@aggregateFunction">
                            <xsl:attribute name="aggregateFunction" select="@aggregateFunction"/>
                        </xsl:if>
                        <xsl:if test="@unitOfMeasure">
                            <xsl:attribute name="unitOfMeasure" select="@unitOfMeasure"/>
                        </xsl:if>
                        <xsl:if test="@groupBy">
                            <xsl:attribute name="groupBy" select="@groupBy"/>
                        </xsl:if>
                        <xsl:if test="xsd:boolean(@searchable)">
                            <xsl:attribute name="searchable" select="@searchable"/>
                        </xsl:if>
                        <xsl:if test="@withEventType">
                            <xsl:attribute name="withEventType" select="@withEventType"/>
                        </xsl:if>
                        <xsl:if test="@withResource">
                            <xsl:attribute name="withResource" select="@withResource"/>
                        </xsl:if>
                    </usage:attributes>
                </appinfo>
            </annotation>
            <xsl:if test="count($types) &gt; 1">
                <simpleType>
                    <union>
                        <xsl:attribute name="memberTypes">
                            <xsl:value-of select="$rtypes" separator=" "/>
                        </xsl:attribute>
                    </union>
                </simpleType>
            </xsl:if>
        </attribute>
    </xsl:template>
    <xsl:template match="schema:attribute" mode="AddTypes">
        <xsl:param name="usage-summary" tunnel="yes" as="xsd:boolean"/>
        <xsl:variable name="attrib" as="node()" select="."/>
        <xsl:choose>
            <xsl:when test="$usage-summary">
                <xsl:if test="@aggregateFunction=('AVG','WEIGHTED_AVG', 'SUM','DAILY_WEIGHTED_SUM') and (@min or @max)">
                    <xsl:call-template name="addMinMaxType"/>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="types" as="xsd:string*" select="tokenize(@type, ' ')" use-when="not(system-property('xsl:is-schema-aware'))"/>
                <xsl:variable name="types" as="xsd:string*" select="@type" use-when="system-property('xsl:is-schema-aware')"/>
                <xsl:for-each select="$types">
                    <xsl:if test="ends-with(., '*')">
                        <xsl:call-template name="addListType">
                            <xsl:with-param name="type" select="."/>
                            <xsl:with-param name="attrib" select="$attrib"/>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:for-each>
                <xsl:choose>
                    <xsl:when test="(count($types) = 1) and (@allowedValues)">
                        <xsl:call-template name="addEnumType">
                            <xsl:with-param name="type" select="$types[1]"/>
                            <xsl:with-param name="attrib" select="$attrib"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="@min or @max">
                        <xsl:call-template name="addMinMaxType"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="schema:xpathAssertion[not(@scope) or @scope='product']" mode="assertions">
        <xsl:variable name="message" select="normalize-space(.)"/>
        <assert vc:minVersion="1.1" test="{normalize-space(@test)}"
                xerces:message="{$message}" saxon:message="{$message}">
            <annotation>
                <documentation>
                    <html:p>Assertion: <xsl:value-of select="$message"/></html:p>
                </documentation>
            </annotation>
        </assert>
    </xsl:template>
    <xsl:template name="addAttributeGroups">
        <xsl:if test="schema:attributeGroup">
            <all vc:minVersion="1.1">
                <xsl:apply-templates select="schema:attributeGroup" mode="AddAttributeGroups"/>
            </all>
            <xsl:comment>Order matters in XSD 1.0 :-(</xsl:comment>
            <sequence vc:minVersion="1.0" vc:maxVersion="1.1">
                <xsl:apply-templates select="schema:attributeGroup" mode="AddAttributeGroups"/>
            </sequence>
        </xsl:if>
    </xsl:template>
    <xsl:template name="addEnumType">
        <xsl:param name="type" as="xsd:string" />
        <xsl:param name="attrib" as="node()"/>
        <xsl:param name="use-version" as="xsd:boolean" tunnel="yes"/>
        <xsl:param name="version" as="xsd:string" tunnel="yes"/>
        <xsl:variable name="enumValues" as="xsd:string*" select="tokenize(@allowedValues, ' ')" use-when="not(system-property('xsl:is-schema-aware'))"/>
        <xsl:variable name="enumValues" as="xsd:string*" select="@allowedValues" use-when="system-property('xsl:is-schema-aware')"/>
        <simpleType>
            <xsl:attribute name="name" select="usage:enumNameType(usage:versionName($attrib/@name, $use-version, $version), false())"/>
            <restriction>
                <xsl:attribute name="base" select="usage:enumBaseType($type)"/>
                <xsl:for-each select="$enumValues">
                    <enumeration>
                        <xsl:attribute name="value" select="normalize-space(.)"/>
                    </enumeration>
                </xsl:for-each>
            </restriction>
        </simpleType>
    </xsl:template>
    <xsl:template name="addListType">
        <xsl:param name="type" as="xsd:string"/>
        <xsl:param name="attrib" as="node()"/>
        <xsl:param name="use-version" as="xsd:boolean" tunnel="yes"/>
        <xsl:param name="version" as="xsd:string" tunnel="yes"/>
        <simpleType>
            <xsl:attribute name="name" select="usage:listNameType(usage:versionName($attrib/@name, $use-version, $version),false())"/>
            <list>
                <xsl:attribute name="itemType">
                    <xsl:value-of select="usage:listItemType($type, $attrib, $use-version, $version)"/>
                </xsl:attribute>
            </list>
        </simpleType>
    </xsl:template>
    <xsl:template name="addMinMaxType">
        <xsl:param name="use-version" as="xsd:boolean" tunnel="yes"/>
        <xsl:param name="version" as="xsd:string" tunnel="yes"/>
        <xsl:param name="usage-summary" as="xsd:boolean" tunnel="yes"/>
        <xsl:variable name="do-usage-summary-minmax" as="xsd:boolean"
                      select="if ($usage-summary) then @aggregateFunction=('AVG','WEIGHTED_AVG') else false()"/>
        <xsl:variable name="usage-summary-no-minmax" as="xsd:boolean"
                      select="if ($usage-summary) then @aggregateFunction=('SUM','DAILY_WEIGHTED_SUM') else false()"/>
        <simpleType>
            <xsl:attribute name="name" select="usage:minMaxType(usage:versionName(@name, $use-version, $version), false(),
                                               ($do-usage-summary-minmax or $usage-summary-no-minmax))"/>
            <restriction>
                <xsl:choose>
                    <xsl:when test="$do-usage-summary-minmax">
                        <xsl:attribute name="base" select="'xsd:double'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="base" select="concat('xsd:',@type)"/>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="@min and not($usage-summary-no-minmax)">
                    <minInclusive>
                        <xsl:attribute name="value" select="string(@min)"/>
                    </minInclusive>
                </xsl:if>
                <xsl:if test="@max and not($usage-summary-no-minmax)">
                    <maxInclusive>
                        <xsl:attribute name="value" select="string(@max)"/>
                    </maxInclusive>
                </xsl:if>
            </restriction>
        </simpleType>
    </xsl:template>
    <xsl:function name="usage:minMaxType" as="xsd:string">
        <xsl:param name="name" as="xsd:string"/>
        <xsl:param name="qualified" as="xsd:boolean"/>
        <xsl:param name="usage-summary" as="xsd:boolean"/>
        <xsl:variable name="n" as="xsd:string">
            <xsl:choose>
                <xsl:when test="$usage-summary">
                    <xsl:value-of select="concat($name,'UsageSummary')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$name"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$qualified">
                <xsl:value-of select="concat('p:',$n,'Type')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($n,'Type')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="usage:enumNameType">
        <xsl:param name="name" as="xsd:string"/>
        <xsl:param name="qualified" as="xsd:boolean"/>
        <xsl:choose>
            <xsl:when test="$qualified">
                <xsl:value-of select="concat('p:',$name, 'Enum')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($name, 'Enum')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="usage:enumBaseType">
        <xsl:param name="itype" as="xsd:string"/>
        <xsl:variable name="type" as="xsd:string">
            <xsl:choose>
                <xsl:when test="contains($itype,'*')">
                   <xsl:value-of select="substring-before($itype,'*')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$itype"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$type='UUID'">
                <xsl:value-of select="'p:UUID'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat('xsd:',$type)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="usage:listNameType">
        <xsl:param name="name" as="xsd:string"/>
        <xsl:param name="qualified" as="xsd:boolean"/>
        <xsl:choose>
            <xsl:when test="$qualified">
                <xsl:value-of select="concat('p:',$name, 'List')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($name, 'List')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="usage:listItemType">
        <xsl:param name="itype" as="xsd:string" />
        <xsl:param name="attrib" as="node()"/>
        <xsl:param name="use-version" as="xsd:boolean"/>
        <xsl:param name="version" as="xsd:string"/>
        <xsl:variable name="type" select="substring-before($itype,'*')" as="xsd:string"/>
        <xsl:variable name="minLengthValue">
            <xsl:choose>
                <xsl:when test="$attrib/@minLength"><xsl:value-of select="$attrib/@minLength"/></xsl:when>
                <xsl:otherwise>0</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$type='UUID'">
                <xsl:value-of select="concat('p:',$type)"/>
            </xsl:when>
            <xsl:when test="$attrib/@allowedValues">
                <xsl:value-of select="usage:enumNameType(usage:versionName($attrib/@name, $use-version, $version),true())"/>
            </xsl:when>
            <xsl:when test="$type='string' and $attrib/@maxLength">
                <xsl:value-of select="concat('p:',$type,$minLengthValue,'_',$attrib/@maxLength)"/>
            </xsl:when>
            <xsl:when test="$type='string' and not($attrib/@maxLength)">
                <xsl:value-of select="concat('p:',$type)"/>
            </xsl:when>
            <xsl:when test="$type='Name'">
                <xsl:value-of select="concat('p:',$type)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat('xsd:',$type)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="usage:quote" as="xsd:string">
        <xsl:param name="in" as="xsd:string"/>
        <xsl:variable name="out">
            <xsl:text>'</xsl:text><xsl:value-of select="$in"/><xsl:text>'</xsl:text>
        </xsl:variable>
        <xsl:value-of select="$out"/>
    </xsl:function>
    <xsl:function name="usage:versionName" as="xsd:string">
        <xsl:param name="name" as="xsd:string"/>
        <xsl:param name="use-version" as="xsd:boolean"/>
        <xsl:param name="version" as="xsd:string"/>
        <xsl:choose>
            <xsl:when test="$use-version">
                <xsl:value-of select="concat($name,$version)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$name"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="usage:resourceTypeName" as="xsd:string">
        <xsl:param name="schema" as="element()"/>
        <xsl:param name="use-version" as="xsd:boolean"/>
        <xsl:param name="version" as="xsd:string"/>
        <xsl:choose>
            <xsl:when test="not($use-version)">
                <xsl:value-of select="'ResourceTypes'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat('ResourceTypes',$version)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="usage:baseTypeName" as="xsd:string">
        <xsl:param name="schema" as="element()"/>
        <xsl:value-of select="concat('Base',$schema/@serviceCode,'Type')"/>
    </xsl:function>
    <xsl:template mode="#all" match="text()"/>
</xsl:stylesheet>
