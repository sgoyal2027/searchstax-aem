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

### WKND / AEM UI broken after install

Older connector builds could damage AEM in two ways:

#### Publish — resource resolver override

A **global** publish OSGi config (`JcrResourceResolverFactoryImpl.cfg.json` without a `~searchstaxconnector` suffix) **replaced AEM's default resource resolver mappings**. That breaks WKND pages, templates, and short URLs on **publish** (port 4503).

**Fix on publish (before reinstalling):**

1. Open **OSGi Console** (`http://localhost:4503/system/console/configMgr`).
2. Find **Apache Sling Resource Resolver Factory** (PID: `org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl`).
3. If a config exists with only `/content/searchstaxconnector/` mapping, **delete** it.
4. Ensure the default mapping is restored (`/:/`, `/libs/</`, etc.).
5. **Restart** publish.

Also delete any orphan global **Apache Sling Servlet Resolver** config (PID without `~searchstaxconnector` suffix).

#### Author — Tools / Sites navigation overlay

Older `ui.apps` packages used a **replace** filter on `/apps/cq/core/content/nav`, which removed AEM's author navigation overlay (Sites, Assets, etc.). Only SearchStax tools remained under `/apps/cq/core/content/nav`.

**Fix on author (4502):**

1. In **CRXDE Lite** (`/crx/de`), delete the node `/apps/cq/core/content/nav` (or run: `curl -u admin:admin -X POST http://localhost:4502/apps/cq/core/content/nav -F :operation=delete`).
2. Reinstall the current connector package: `mvn clean install -PautoInstallSinglePackage`.
3. Confirm `/mnt/overlay/cq/core/content/nav.infinity.json` lists **Sites**, **Assets**, and **Searchstax** under **Tools**.

Current `ui.apps` only merges `/apps/cq/core/content/nav/tools` (SearchStax entries) and does not touch the parent nav node.

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

### Search UI (SearchStudio UX)

The connector ships a publish-ready **SearchStax Search** AEM component powered by the vanilla JS SearchStudio UX toolkit ([searchstudio-ux-samples/pages/js](https://github.com/searchstax/searchstudio-ux-samples/tree/master/pages/js)). Vanilla JS is used instead of React/Vue/Angular because it fits AEM clientlibs and HTL components without a separate SPA build.

**Configuration source:** search endpoints and keys are read from the connector wizards in JCR (`/conf/searchstaxconnector/settings/apiconfig` and language mappings), not from a static `config.js` file.

| Setting | Wizard field |
|---------|----------------|
| Search URL | API Configuration → Select endpoint (fallback: Endpoint URL) |
| Search auth | API Configuration → Select token (fallback: API token) |
| Autosuggest URL | API Configuration → Autosuggest API |
| Related searches | API Configuration → Related searches endpoint + Discovery API key |
| Analytics | API Configuration → Analytics tracking URL + key |
| Language | Current page locale + Language Mapping wizard (optional component override) |

**Public config endpoint (publish):** `GET /bin/searchstaxconnector/search/config`

**Author component:** drag **SearchStax Search** (`searchstaxconnector/components/search`) onto a page. Configure pagination vs infinite scroll and faceting in the component dialog.

**Build note:** `ui.apps` runs `npm install` during Maven build to vendor `@searchstax-inc/searchstudio-ux-js` into `clientlib-search-ux-vendor`.

### Not yet implemented

- Full index **Run** button execution (UI present; service still returns HTTP 503)

## Project status

- Step 1: Maven project setup — complete
- Step 2: Author configuration UI — complete
- Step 3: Incremental indexing — complete
- Step 3b: Full reindex execution — pending
