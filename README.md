# SearchStax AEM Connector

Maven multi-module project for push-based indexing of AEM content to SearchStax Solr. Supports AEM 6.5 on-prem, AMS, and AEM as a Cloud Service.

## Modules

| Module | Purpose |
|--------|---------|
| `core` | OSGi bundle — configuration services, wizard servlets, and APIs |
| `ui.apps` | Author UI wizards, clientlibs, and Tools navigation |
| `ui.config` | OSGi configurations, repoinit, and service user mapping |
| `ui.apps.structure` | Repository structure package for FileVault validation |
| `all` | Container package embedding core, ui.apps, and ui.config |

## Prerequisites

- JDK 11 or higher
- Apache Maven 3.3.9 or higher

## Build

**AEM as a Cloud Service (default):**

```powershell
mvn clean install
```

**AEM 6.5 on-prem / AMS:**

```powershell
mvn clean install -Pclassic
```

## Deploy

Install the container package on the **author** instance:

```powershell
mvn clean install -PautoInstallSinglePackage
```

Or upload `all/target/searchstax-aem-connector.all-1.0.0-SNAPSHOT.zip` via Package Manager.

## Author configuration UI

After install, open **Tools → SearchStax Connector** on the author instance:

| Wizard | Purpose |
|--------|---------|
| Initial Setup | Enable connector, root paths, exclude paths, allowed file types |
| API Configuration | SearchStax Solr update endpoint, credentials, and related settings |
| Metadata Field Mapping | Map AEM properties to Solr index fields |
| Language Mapping | Map AEM language codes to SearchStax language codes |
| Full Index Configuration | Path includes/excludes for bulk reindex scope |
| Email Configuration | SMTP settings and recipients for indexing failure notifications |

Configuration is stored under `/conf/searchstaxconnector/settings/*`:

| Wizard | JCR path |
|--------|----------|
| Initial Setup | `/conf/searchstaxconnector/settings/initialsetupconfig` |
| API Configuration | `/conf/searchstaxconnector/settings/apiconfig` |
| Email Configuration | `/conf/searchstaxconnector/settings/emailconfig` |
| Metadata Mapping | `/conf/searchstaxconnector/settings/metadatafieldmapping` |
| Language Mapping | `/conf/searchstaxconnector/settings/languagemapping` |
| Full Index paths | `/conf/searchstaxconnector/settings/fullindexsetupconfig` |

API tokens are encrypted in JCR via AEM `CryptoSupport`. OSGi configs in `ui.config` are limited to infrastructure (repoinit, service users, servlet paths, logging).

**Servlet 404 troubleshooting:** Install the full container package (`searchstax-aem-connector.all`), not `ui.apps` alone. Verify `searchstax-aem-connector.core` is **Active** in `/system/console/bundles`. After install, these URLs should return JSON on author (not 404):

- `/bin/searchstaxconnector/wizard/initial-setup-load`
- `/bin/searchstaxconnector/wizard/initial-setup-config` (POST)

If a previous install replaced the global Sling Servlet Resolver config, remove the orphan `org.apache.sling.servlets.resolver.internal.SlingServletResolver` config from OSGi Console and reinstall `ui.config`.

Use **Test Configuration** on the API wizard to validate connectivity (`/bin/staxsync/searchstax/test-connection`).

### Not yet implemented (Step 3)

- Full index **Run** button (UI present; execution returns HTTP 503 until indexing logic is added)
- Incremental indexing on activation/deactivation

## Project status

- Step 1: Maven project setup — complete
- Step 2: Author configuration UI — complete
- Step 3: Incremental and full reindex logic — pending
