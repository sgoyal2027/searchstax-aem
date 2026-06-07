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
| Indexing Report | Last 24 hours of incremental indexing success/failure events |

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

### WKND / site rendering broken after install

Older builds shipped a **global** publish OSGi config (`JcrResourceResolverFactoryImpl.cfg.json` without a `~searchstaxconnector` suffix) that **replaced AEM's default resource resolver mappings**. That breaks WKND pages, templates, and short URLs on **publish** (port 4503).

**Fix on the affected instance (do this before reinstalling):**

1. Open **OSGi Console** on the broken instance (`http://localhost:4503/system/console/configMgr` for publish, or 4502 if publish runmode is co-located).
2. Find **Apache Sling Resource Resolver Factory** (PID: `org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl`).
3. If a config exists with only `/content/searchstaxconnector/` mapping, **delete** that configuration (trash icon).
4. Ensure the default mapping is restored (should include `/content/</`, `/libs/</`, `/etc/`</`, etc.). On a fresh AEM SDK you can compare with an untouched instance.
5. **Restart** that AEM instance.

Also check for an orphan global **Apache Sling Servlet Resolver** config (PID without `~searchstaxconnector` suffix) and delete it if present. Connector configs must use named suffixes such as `~searchstaxconnector` so they do not replace platform defaults.

Current `ui.config` only ships **named** factory configs (`~searchstaxconnector`) and no longer includes the publish resource-resolver override.

Use **Test Configuration** on the API wizard to validate connectivity (`/bin/staxsync/searchstax/test-connection`).

### Incremental indexing (Step 3)

- Listens on **author** for replication `ACTIVATE` / `DEACTIVATE` / `DELETE`
- Gates on Initial Setup (`enableConnector`, root/exclude paths, allowed asset MIME types)
- Builds Solr documents from metadata + language mappings (`title_txt_en`, `language_s`, etc.)
- Batches updates (10s debounce, max 100 paths per job/API call)
- Enforces SearchStax service limits (100 KB/doc, 10 MB payload, URL length) with HTTP 413 batch-splitting, exponential backoff on 429/5xx, and documented error guidance
- Logs each step and stores audit records under `/var/searchstaxconnector/incremental/audit`
- Sends failure email when Email config notifications are enabled
- **Indexing Report** wizard shows the last 24 hours of results (auto-purged)

### Not yet implemented

- Full index **Run** button execution (UI present; service still returns HTTP 503)

## Project status

- Step 1: Maven project setup — complete
- Step 2: Author configuration UI — complete
- Step 3: Incremental indexing — complete
- Step 3b: Full reindex execution — pending
