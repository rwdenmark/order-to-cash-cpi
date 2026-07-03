<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="/Order">
    <Fulfillment>
      <Reference><xsl:value-of select="PoNumber"/></Reference>
      <Warehouse><xsl:value-of select="ShipTo"/></Warehouse>
      <Items>
        <xsl:for-each select="Lines/Line">
          <Item>
            <xsl:attribute name="sku"><xsl:value-of select="Sku"/></xsl:attribute>
            <xsl:attribute name="quantity"><xsl:value-of select="Qty"/></xsl:attribute>
          </Item>
        </xsl:for-each>
      </Items>
    </Fulfillment>
  </xsl:template>
</xsl:stylesheet>
