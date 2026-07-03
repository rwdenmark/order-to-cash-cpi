# cpi-package

Wipe insurance for the CPI trial tenant.

## LabPractice_package_export.zip

Full SAP Integration Suite package export of the LabPractice package, taken 7/1/2026. It contains the OrderToCash and Simple_Echo iFlows. This zip is what the tenant re-imports after a wipe, so it stays intact and unmodified. Do not edit it, do not re-zip it. After each CPI session, Save as version in the tenant, export again, and replace this file.

## OrderToCash/

The OrderToCash iFlow's inner artifact, extracted from the export so the source is browsable in the repo. The iFlow definition, the Groovy script, both XSDs, the graphical mapping, and the deployed XSLT. This tree documents what the tenant is currently running. The `OrderToFulfillment.xsl` here is the deployed copy, which has a known shape bug. The fix lives in `../OrderToCash_corrected/`.
