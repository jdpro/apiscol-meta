<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lom="http://ltsc.ieee.org/xsd/LOM"
	xmlns:lomfr="http://www.lom-fr.fr/xsd/LOMFR" xmlns:scolomfr="http://www.lom-fr.fr/xsd/SCOLOMFR">
	<xsl:output method="html" omit-xml-declaration="yes"
		encoding="UTF-8" indent="yes" />
	<xsl:variable name="user.language" select="'fr'" />
	<xsl:template match="/lom:lom">
		<add>
			<doc>
				<xsl:apply-templates />
			</doc>
		</add>
	</xsl:template>
	<xsl:template match="lom:general/lom:identifier">

	</xsl:template>
	<xsl:template match="lom:general/lom:title">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of select="'general.title'"></xsl:value-of></xsl:attribute>
			<xsl:call-template name="find-best-string-for-user-language" />
		</xsl:element>
	</xsl:template>
	<xsl:template name="find-best-string-for-user-language">
		<xsl:choose>
			<xsl:when test="lom:string[@language=$user.language]">
				<xsl:value-of select="lom:string[@language=$user.language]" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="lom:string" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="lom:general/lom:language">
		<xsl:element name="field">
			<xsl:attribute name="language"><xsl:value-of select="'general.language'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="." />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:general/lom:description">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="'general.description'"></xsl:value-of></xsl:attribute>
			<xsl:call-template name="find-best-string-for-user-language" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:general/lom:keyword">
		<xsl:call-template name="splitkeywords">
			<xsl:with-param name="string">
				<xsl:call-template name="find-best-string-for-user-language" />
			</xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	<xsl:template match="lom:lifeCycle/lom:contribute">
		<xsl:variable name="role" select="lom:role/lom:value" />
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="concat('lifecycle.',$role)"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:entity" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:lifeCycle/lom:status">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of select="'lifecycle.status'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:value" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:metaMetadata/lom:contribute">
		<xsl:variable name="role" select="lom:role/lom:value" />
		<xsl:apply-templates select="lom:entity">
			<xsl:with-param name="role">
				<xsl:value-of select="$role"></xsl:value-of>
			</xsl:with-param>
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="lom:entity">
		<xsl:param name="role" />
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="concat('metameta.',$role)"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="."></xsl:value-of>
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:metaMetadata/lom:identifier">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of select="'id'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:entry" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:technical/lom:location">
	</xsl:template>
	<xsl:template match="lom:classification">
		<xsl:apply-templates select="lom:taxonPath">
			<xsl:with-param name="purpose">
				<xsl:value-of select="lom:purpose/lom:value" />
			</xsl:with-param>
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="lom:relation">
		<xsl:variable name="kind" select="lom:kind/lom:value" />
		<xsl:variable name="catalog"
			select="lom:resource/lom:identifier/lom:catalog" />
		<xsl:apply-templates select="lom:resource/lom:description">
			<xsl:with-param name="kind" select="$kind" />
			<xsl:with-param name="catalog" select="$catalog" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="lom:resource/lom:description">
		<xsl:param name="kind" />
		<xsl:param name="catalog" />
		<xsl:variable name="description">
			<xsl:call-template name="find-best-string-for-user-language" />
		</xsl:variable>
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of select="'relation'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="concat($kind,'!_!',$catalog,'(__',$description,'__)')" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:taxonPath">
		<xsl:param name="purpose" />
		<xsl:variable name="fieldname" select="concat($purpose, '-taxon')" />
		<xsl:variable name="source" select="lom:source/lom:string" />
		<xsl:variable name="taxonid" select="lom:taxon/lom:id" />
		<xsl:apply-templates select="lom:taxon/lom:entry">
			<xsl:with-param name="taxonid" select="$taxonid" />
			<xsl:with-param name="purpose" select="$purpose" />
			<xsl:with-param name="fieldname" select="$fieldname" />
			<xsl:with-param name="source" select="$source" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="lom:taxon/lom:entry">
		<xsl:param name="purpose" />
		<xsl:param name="taxonid" />
		<xsl:param name="fieldname" />
		<xsl:param name="source" />
		<xsl:variable name="label">
			<xsl:call-template name="find-best-string-for-user-language" />
		</xsl:variable>
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of select="$fieldname"></xsl:value-of></xsl:attribute>
			<!-- TODO c'est quoi ce coucou -->
			<xsl:value-of select="concat($source,'!_!',$taxonid,'(__',' coucou ','__)')" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:educational/lom:learningResourceType">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="'educational.learningResourceType'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:value" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:educational/lom:intendedEndUserRole">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="'educational.intendedEndUserRole'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:value" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:educational/lom:context">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="'educational.context'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:value" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:metadataSchema" />
	<xsl:template match="lom:metaMetadata/lom:language" />
	<xsl:template match="lom:educational/lom:description">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="'educational.description'"></xsl:value-of></xsl:attribute>
			<xsl:call-template name="find-best-string-for-user-language" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:educational/lom:language">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="'educational.language'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="." />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:rights/lom:cost">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of select="'rights.costs'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:value" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="lom:rights/lom:copyrightAndOtherRestrictions">
		<xsl:element name="field">
			<xsl:attribute name="name"><xsl:value-of
				select="'rights.copyrightAndOtherRestrictions'"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="lom:value" />
		</xsl:element>
	</xsl:template>

	<xsl:template name="splitkeywords">
		<xsl:param name="string" />
		<xsl:choose>
			<xsl:when test="contains($string,',')">
				<field name="general.keyword">
					<xsl:value-of select="normalize-space(substring-before($string,','))" />
				</field>
				<xsl:call-template name="splitkeywords">
					<xsl:with-param name="string">
						<xsl:value-of select="substring-after($string,',')" />
					</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="normalize-space($string) != ''">
					<field name="general.keyword">
						<xsl:value-of select="normalize-space($string)" />
					</field>
				</xsl:if>


			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
