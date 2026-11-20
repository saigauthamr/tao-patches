# fanfiction-morphe

A set of [Morphe](https://github.com/MorpheApp) patches for the FanFiction.Net Android app
(`com.fictionpress.fanfiction`).

## Goals

- Performance: cut startup time, jank, and memory use.
- Fewer server requests: drop redundant calls, cache more, remove telemetry and ad traffic.
- General quality-of-life improvements.

The patches ship as a bundle that users apply themselves with Morphe Manager or Morphe CLI.

## Target

| App | Package | Version |
|---|---|---|
| FanFiction.Net | `com.fictionpress.fanfiction` | 66.5 (build 918) |

Upstream APKs are not committed (see `.gitignore`). Put the `.apkm` in the repo root to work locally.
