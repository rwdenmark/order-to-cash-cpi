package com.ryan.ordertocash;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Receives an order from the web form as JSON, builds the canonical Order XML,
 * and hands it to CPI via CpiClient. Returns whatever CPI sends back (the
 * Fulfillment XML).
 *
 * The browser only ever talks to THIS backend. The SAP OAuth client secret
 * lives in application.properties and never leaves the server.
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    private final CpiClient cpi;

    public OrderController(CpiClient cpi) {
        this.cpi = cpi;
    }

    public record Line(String sku, int qty, BigDecimal price) {}

    public record OrderRequest(String poNumber, String region, String shipTo, List<Line> lines) {}

    @PostMapping(value = "/orders", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> submit(@RequestBody OrderRequest req) {
        if (req.poNumber() == null || req.poNumber().isBlank()) {
            return ResponseEntity.badRequest().body("poNumber is required");
        }
        // the iFlow router falls back to EAST for anything it doesn't recognize,
        // so reject unknown regions here instead of shipping them silently
        if (!"EAST".equals(req.region()) && !"WEST".equals(req.region())) {
            return ResponseEntity.badRequest().body("region must be EAST or WEST");
        }
        if (req.lines() != null) {
            for (Line l : req.lines()) {
                if (l.qty() <= 0) {
                    return ResponseEntity.badRequest().body("line qty must be greater than zero");
                }
                if (l.price() == null) {
                    // an empty <Price/> breaks the xs:decimal mapping downstream
                    return ResponseEntity.badRequest().body("line price is required");
                }
                if (l.price().signum() < 0) {
                    return ResponseEntity.badRequest().body("line price cannot be negative");
                }
            }
        }

        String orderXml = toOrderXml(req);
        try {
            return ResponseEntity.ok(cpi.sendOrder(orderXml));
        } catch (RestClientResponseException e) {
            // pass the CPI error through so the browser sees what went wrong
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // connect or read timeout, CPI never answered
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("could not reach CPI, " + e.getMessage());
        }
    }

    // package-private so the unit tests can hit it directly
    String toOrderXml(OrderRequest r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Order>");
        sb.append("<PoNumber>").append(esc(r.poNumber())).append("</PoNumber>");
        sb.append("<Region>").append(esc(r.region())).append("</Region>");
        sb.append("<ShipTo>").append(esc(r.shipTo())).append("</ShipTo>");
        sb.append("<Lines>");
        int n = 1;
        if (r.lines() != null) {
            for (Line l : r.lines()) {
                sb.append("<Line>")
                  .append("<Num>").append(n++).append("</Num>")
                  .append("<Sku>").append(esc(l.sku())).append("</Sku>")
                  .append("<Qty>").append(l.qty()).append("</Qty>")
                  .append("<Price>").append(l.price() == null ? "" : l.price().toPlainString()).append("</Price>")
                  .append("</Line>");
            }
        }
        sb.append("</Lines>");
        sb.append("</Order>");
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
