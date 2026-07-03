import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.MarkupBuilder

def Message processData(Message message) {
    def order = new XmlSlurper().parse(message.getBody(java.io.Reader.class))

    def total = 0.0
    order.Lines.Line.each {
        total += it.Qty.text().toBigDecimal() * it.Price.text().toBigDecimal()
    }

    def writer = new StringWriter()
    new MarkupBuilder(writer).Fulfillment {
        Reference(order.PoNumber.text())
        Warehouse(order.ShipTo.text())
        TotalValue(total.toString())
        Items {
            order.Lines.Line.each { line ->
                Item(sku: line.Sku.text(), quantity: line.Qty.text())
            }
        }
    }
    message.setBody(writer.toString())
    message.setProperty("Region", order.Region.text())
    return message
}