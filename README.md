# Order to Cash on SAP Integration Suite (CPI)

An end-to-end order-to-cash integration demo built on **SAP Integration Suite (Cloud Integration / CPI)**. A web form submits an order; a Spring Boot backend hands it to a deployed CPI iFlow over OAuth; the iFlow transforms and **content-routes** the order to a self-hosted receiver based on its region.

Built as a hands-on way to exercise the core skills of an enterprise integration role (iFlows, adapters, message mapping, scripting, and integration patterns) in a supply-chain domain rather than a toy example.

## Architecture

```
[Web form] --> [Spring Boot backend] --OAuth--> [SAP CPI iFlow]
                (holds SAP secrets)              |  HTTPS sender
                                                 |  Content Modifier (capture Region)
                                                 |  Message Mapping (Order -> Fulfillment)
                                                 |  Router (content-based, by Region)
                                                 |    WEST --> HTTP --> /warehouse/west
                                                 |    EAST --> HTTP --> /warehouse/east
                                                 v
                                       [warehouse-receiver]
                                       TypeScript/Express on a self-hosted
                                       box, exposed via Tailscale Funnel
```

The browser only ever talks to the Spring Boot backend; the SAP OAuth client secret stays server-side.

## Components

| Folder | What it is |
|---|---|
| [`order-to-cash-app/`](order-to-cash-app/) | Spring Boot backend + static dark-mode web form. Builds the Order XML and calls the CPI iFlow with OAuth client-credentials. See its [README](order-to-cash-app/README.md). |
| [`warehouse-receiver/`](warehouse-receiver/) | Small TypeScript/Express service that stands in for downstream "warehouse" systems. Self-hosted and exposed publicly via Tailscale Funnel so CPI can reach it. |

The CPI iFlow itself (the `OrderToCash` integration flow and `OrderToFulfillment` mapping) lives in SAP Integration Suite; the package is exported alongside this repo.

## What it demonstrates (SAP CPI)

- **iFlows and the HTTPS sender adapter** with OAuth-secured invocation
- **Message mapping**, both graphical and the context model (`removeContexts` + `sum` to total line items), plus a Groovy variant
- **Content-based routing** via a Router with Non-XML conditions on a captured property
- **HTTP receiver adapters** calling an external, TLS-fronted endpoint
- **Monitoring and error handling** in the CPI message-processing log

## Tech stack

Java, Spring Boot, SAP Integration Suite (CPI), TypeScript, Express, Tailscale Funnel.

## Notes

This is a personal learning and portfolio project, not a production system. Credentials live in gitignored config (`order-to-cash-app/src/main/resources/application.properties`); see the `.example` file for the shape.
