package com.ryan.ordertocash;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests, no Spring context. CpiClient is stubbed so nothing
 * talks to SAP.
 */
class OrderControllerTest {

    /** Captures the XML instead of calling CPI. */
    private static class StubCpi extends CpiClient {
        String lastXml;
        @Override public String sendOrder(String orderXml) {
            lastXml = orderXml;
            return "<Fulfillment/>";
        }
    }

    private final StubCpi cpi = new StubCpi();
    private final OrderController controller = new OrderController(cpi);

    private static OrderController.Line line(String sku, int qty, BigDecimal price) {
        return new OrderController.Line(sku, qty, price);
    }

    // ---- toOrderXml ----

    @Test
    void escapesXmlSpecialCharacters() {
        var req = new OrderController.OrderRequest(
                "PO<1>&", "WEST", "A & B <Co>",
                List.of(line("S&K<U>", 1, new BigDecimal("2.50"))));
        String xml = controller.toOrderXml(req);
        assertTrue(xml.contains("<PoNumber>PO&lt;1&gt;&amp;</PoNumber>"), xml);
        assertTrue(xml.contains("<ShipTo>A &amp; B &lt;Co&gt;</ShipTo>"), xml);
        assertTrue(xml.contains("<Sku>S&amp;K&lt;U&gt;</Sku>"), xml);
    }

    @Test
    void nullFieldsBecomeEmptyElements() {
        var req = new OrderController.OrderRequest(null, null, null, null);
        String xml = controller.toOrderXml(req);
        assertEquals("<Order><PoNumber></PoNumber><Region></Region>"
                + "<ShipTo></ShipTo><Lines></Lines></Order>", xml);
    }

    @Test
    void nullPriceBecomesEmptyElement() {
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4", List.of(line("SKU-1", 1, null)));
        String xml = controller.toOrderXml(req);
        assertTrue(xml.contains("<Price></Price>"), xml);
    }

    @Test
    void largePriceStaysInPlainNotation() {
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4",
                List.of(line("SKU-1", 1, new BigDecimal("12345678901234567.89"))));
        String xml = controller.toOrderXml(req);
        assertTrue(xml.contains("<Price>12345678901234567.89</Price>"), xml);
    }

    @Test
    void scientificInputStillEmitsPlainString() {
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4",
                List.of(line("SKU-1", 1, new BigDecimal("1E+9"))));
        String xml = controller.toOrderXml(req);
        assertTrue(xml.contains("<Price>1000000000</Price>"), xml);
    }

    // ---- validation ----

    @Test
    void blankPoNumberIs400() {
        var req = new OrderController.OrderRequest("  ", "WEST", "Dock 4", List.of());
        var resp = controller.submit(req);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void nullPoNumberIs400() {
        var req = new OrderController.OrderRequest(null, "WEST", "Dock 4", List.of());
        var resp = controller.submit(req);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void zeroQtyIs400() {
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4", List.of(line("SKU-1", 0, new BigDecimal("2.50"))));
        var resp = controller.submit(req);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void negativePriceIs400() {
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4", List.of(line("SKU-1", 1, new BigDecimal("-0.01"))));
        var resp = controller.submit(req);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void nullPriceIs400() {
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4", List.of(line("SKU-1", 1, null)));
        var resp = controller.submit(req);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void unknownRegionIs400() {
        var req = new OrderController.OrderRequest(
                "PO-1", "NORTH", "Dock 4", List.of(line("SKU-1", 1, new BigDecimal("2.50"))));
        var resp = controller.submit(req);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void missingRegionIs400() {
        var req = new OrderController.OrderRequest(
                "PO-1", null, "Dock 4", List.of(line("SKU-1", 1, new BigDecimal("2.50"))));
        var resp = controller.submit(req);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void validOrderGoesToCpiAndReturnsFulfillment() {
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4", List.of(line("SKU-1", 2, new BigDecimal("9.99"))));
        var resp = controller.submit(req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("<Fulfillment/>", resp.getBody());
        assertTrue(cpi.lastXml.contains("<PoNumber>PO-1</PoNumber>"), cpi.lastXml);
    }

    @Test
    void cpiErrorStatusAndBodyPassThrough() {
        StubCpi failing = new StubCpi() {
            @Override public String sendOrder(String orderXml) {
                throw new RestClientResponseException(
                        "CPI rejected the order", HttpStatusCode.valueOf(422), "Unprocessable Entity",
                        null, "cpi says no".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            }
        };
        var ctrl = new OrderController(failing);
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4", List.of(line("SKU-1", 1, new BigDecimal("2.50"))));
        var resp = ctrl.submit(req);
        assertEquals(422, resp.getStatusCode().value());
        assertEquals("cpi says no", resp.getBody());
    }

    @Test
    void cpiTimeoutIs502WithCleanBody() {
        StubCpi timingOut = new StubCpi() {
            @Override public String sendOrder(String orderXml) {
                throw new ResourceAccessException("I/O error on POST request: Read timed out");
            }
        };
        var ctrl = new OrderController(timingOut);
        var req = new OrderController.OrderRequest(
                "PO-1", "WEST", "Dock 4", List.of(line("SKU-1", 1, new BigDecimal("2.50"))));
        var resp = ctrl.submit(req);
        assertEquals(502, resp.getStatusCode().value());
        assertTrue(resp.getBody().startsWith("could not reach CPI"), resp.getBody());
    }
}
