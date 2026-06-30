# Order-to-Cash App (Spring Boot + static frontend → SAP CPI)

A web form that submits an order to a Spring Boot backend, which calls a deployed
SAP Integration Suite (CPI) iFlow with OAuth and returns the result. The browser
never sees the SAP credentials.

## Structure

```
order-to-cash-app/
├── pom.xml
├── src/main/java/com/ryan/ordertocash/
│   ├── OrderToCashApplication.java   # entry point
│   ├── OrderController.java          # POST /api/orders, builds Order XML
│   └── CpiClient.java                # OAuth token + POST to iFlow
└── src/main/resources/
    ├── application.properties        # <-- fill in your CPI service-key values
    └── static/index.html             # the web form (served at http://localhost:8080)
```

## Run

1. Build/deploy the CPI iFlow first (see `../order_to_cash_project_spec.md`), so you have
   its endpoint URL.
2. Create a **Process Integration Runtime** service key in BTP (plan `integration-flow`,
   role `ESBMessaging.send`). Copy `tokenurl`, `clientid`, `clientsecret`.
3. Fill those four values in `src/main/resources/application.properties`
   (`cpi.token-url`, `cpi.client-id`, `cpi.client-secret`, `cpi.iflow-url`).
4. Run:
   ```
   mvn spring-boot:run
   ```
5. Open http://localhost:8080, fill the form, click **Send to SAP**. The Fulfillment
   XML from CPI appears below the button.

## Notes

- Frontend is a single static HTML page (vanilla JS) so there's no separate build step.
  Swap it for a React app later if you want — the backend API stays the same.
- Same-origin (page served by Spring Boot), so no CORS config needed.
- The OAuth client secret stays server-side in `application.properties`. Do not move
  the SAP call into the browser.
