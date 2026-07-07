# Order-to-Cash App (Spring Boot + static frontend → SAP CPI)

A web form that submits an order to a Spring Boot backend, which calls a deployed
SAP Integration Suite (CPI) iFlow with OAuth and returns the result. The browser
never sees the SAP credentials.

## Structure

```
order-to-cash-app/
├── pom.xml
├── schemas/                          # Order/Fulfillment XSDs + sample order
├── src/main/java/com/ryan/ordertocash/
│   ├── OrderToCashApplication.java   # entry point
│   ├── OrderController.java          # POST /api/orders, builds Order XML
│   └── CpiClient.java                # OAuth token + POST to iFlow
├── src/main/resources/
│   ├── application.properties.example   # copy to application.properties (gitignored)
│   └── static/index.html             # the web form (served at http://localhost:8080)
└── src/test/java/com/ryan/ordertocash/
    ├── OrderControllerTest.java
    └── CpiClientTest.java
```

`schemas/` mirrors the XSDs in `cpi-package`. The `cpi-package` copy is canonical.

## Run

1. Build/deploy the CPI iFlow first, so you have its endpoint URL.
2. Create a **Process Integration Runtime** service key in BTP (plan `integration-flow`,
   role `ESBMessaging.send`). Copy `tokenurl`, `clientid`, `clientsecret`.
3. Copy `src/main/resources/application.properties.example` to
   `src/main/resources/application.properties`, then fill in the three service-key
   values plus the iFlow endpoint URL from step 1
   (`cpi.token-url`, `cpi.client-id`, `cpi.client-secret`, `cpi.iflow-url`).
4. Run:
   ```
   mvn spring-boot:run
   ```
5. Open http://localhost:8080, fill the form, click **Send to SAP**. The Fulfillment
   XML from CPI appears below the button.

## Notes

- Frontend is a single static HTML page (vanilla JS) so there's no separate build step.
- Same-origin (page served by Spring Boot), so no CORS config needed.
- The OAuth client secret stays server-side in `application.properties`. Do not move
  the SAP call into the browser.
