<?xml version="1.0" encoding="UTF-8"?>
<!-- Corrected Phase 4 mapping. Matches Fulfillment.xsd, child elements on Item
     plus the required TotalValue. CPI's XSLT step runs Saxon, so 2.0 is fine. -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="/Order">
    <Fulfillment>
      <Reference><xsl:value-of select="PoNumber"/></Reference>
      <Warehouse><xsl:value-of select="ShipTo"/></Warehouse>
      <!-- xs:decimal keeps the math exact, no float drift in the total -->
      <TotalValue><xsl:value-of select="sum(Lines/Line/(xs:decimal(Qty) * xs:decimal(Price)))"/></TotalValue>
      <Items>
        <xsl:for-each select="Lines/Line">
          <Item>
            <Sku><xsl:value-of select="Sku"/></Sku>
            <Quantity><xsl:value-of select="Qty"/></Quantity>
          </Item>
        </xsl:for-each>
      </Items>
    </Fulfillment>
  </xsl:template>
</xsl:stylesheet>
