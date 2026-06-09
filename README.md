# SearchStax AEM Connector

Maven multi-module project for push-based indexing of AEM content to SearchStax Solr.

**Primary target:** AEM **6.5.23** on-prem (Java **11+**). Also supports **AMS** and **AEM as a Cloud Service**.

## Supported environments

| Environment | Build command | Deploy | Notes |
|-------------|---------------|--------|-------|
| **AEM 6.5 on-prem** | `mvn clean install` | Package Manager or `-PautoInstallSinglePackage` | AEM JVM must be **Java 11+**. Delete `/apps/cq/core/content/nav` in CRXDE once if a prior install hit OakConstraint0025. |
| **AMS** | `mvn clean install` | Same container package as 6.5 | AMS runs AEM 6.5 — use the **classic** (default) build. |
| **AEM as a Cloud Service** | `mvn clean install -Pcloudservice` | Cloud Manager pipeline | Uses `aem-sdk-api` for compile; same OSGi bundle (`bnd.bnd`) with cross-version imports. |

**One codebase** — the core bundle uses `version=0.0.0` imports, inlined `commons-email`, no JavaActivation contract, and `norequirements` so it resolves on 6.5, AMS, and Cloud.

## Modules

| Module | Purpose |
|--------|---------|
| `core` | OSGi bundle — configuration services, wizard servlets, and APIs |
| `ui.apps` | Author UI wizards, clientlibs, and Tools navigation |
| `ui.config` | OSGi configurations, repoinit, and service user mapping |
| `ui.apps.structure` | Repository structure package for FileVault validation |
| `all` | Container package embedding core, ui.apps, and ui.config |

## Prerequisites

- **JDK 11 or higher** (matches AEM 6.5.23+ on Java 11)
- Apache Maven 3.3.9 or higher
- AEM 6.5.23 author instance (Java 11 JVM)

## Build

**AEM 6.5 on-prem / AMS (default):**

```powershell
mvn clean install
```

**AEM as a Cloud Service:**

```powershell
mvn clean install -Pcloudservice
```

## Deploy to AEM 6.5.23

Install the container package on the **author** instance (adjust port if not 4502):

```powershell
mvn clean install -PautoInstallSinglePackage "-Daem.port=4505"
```

Or upload `all/target/searchstax-aem-connector.all-1.0.0-SNAPSHOT.zip` via Package Manager.

After install, verify the core bundle in `/system/console/bundles`:

- **Symbolic name:** `searchstax-aem-connector.core`
- **Status:** Active
- **JavaSE:** 11 (requires AEM JVM on Java 11+)

**Bundle stuck at Installed / Resolved (not Active):**

1. Confirm AEM author runs on **Java 11+** (System Information in `/system/console/status-systeminfo`). A bundle built for Java 11 will not resolve on a Java 8 JVM.
2. Reinstall the latest package, then click **Start** on `searchstax-aem-connector.core` in `/system/console/bundles`.
3. Open bundle details → **Imports** — any red/unresolved import blocks activation. Common on 6.5: missing `javax.mail` or `javax.activation` platform bundles (install AEM 6.5.23 with full platform packages).
4. Check `error.log` for `searchstax-aem-connector.core` or `SCR` lines after starting the bundle.

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

The connector uses **`merge` only** on `/apps/cq/.../nav` — it **does not replace** existing CQ navigation.

| What | Touched by this package? |
|------|--------------------------|
| `/libs/cq/core/content/nav` (Sites, Assets, default Tools) | **Never** — read-only product nav |
| Other project's `/apps/cq/core/content/nav/*` overlays | **Not removed** — merge adds/updates only Searchstax nodes |
| Searchstax entry | **Added** under `/apps/cq/core/content/nav/tools/searchstax` |

At runtime AEM merges `/apps` over `/libs` (Sling Resource Merger). Sites, Assets, Operations, and any other Tools entries from `/libs` or other `/apps` overlays remain.

Nav nodetypes (matches AEM `/libs` pattern):

| Path | Nodetype |
|------|----------|
| `/apps/cq` | `sling:Folder` |
| `/apps/cq/core` | `sling:Folder` |
| `/apps/cq/core/content` | `sling:Folder` |
| `/apps/cq/core/content/nav` | `nt:unstructured` |
| `/apps/cq/core/content/nav/tools` | `nt:unstructured` |

**OakConstraint0025:** delete `/apps/cq/core/content/nav` in CRXDE Lite before reinstall if a prior install used wrong nodetypes (`nt:folder` instead of `sling:Folder`). Then reinstall the package.

**If Searchstax Connector is missing under Tools after a successful install:**

1. In **CRXDE Lite** (`/crx/de`), delete `/apps/cq/core/content/nav` if a prior install created it as `nt:folder` (causes OakConstraint0025).
2. Reinstall: `mvn clean install -PautoInstallSinglePackage "-Daem.port=4502" "-Dmaven.test.skip=true"`
3. Verify in CRXDE: `/apps/cq/core/content/nav/tools/searchstax` exists with child nodes (`initialsetup`, `apiconfiguration`, etc.).
4. Verify JSON: `http://localhost:4502/mnt/overlay/cq/core/content/nav.infinity.json` — under `tools` look for `searchstax`.
5. Hard-refresh the author UI (Ctrl+F5) — Granite caches nav.

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
