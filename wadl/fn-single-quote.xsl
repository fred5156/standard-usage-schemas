<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">

    <xsl:output method="xml" encoding="UTF-8"/>

    <xsl:template match="node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select='translate(.,"‘’","&apos;&apos;")'/></xsl:attribute>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:value-of select='translate(.,"‘’","&apos;&apos;")'/>
    </xsl:template>
</xsl:stylesheet>
