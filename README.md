## :link: Ligoj Identity plugin ![Maven Central](https://img.shields.io/maven-central/v/org.ligoj.plugin/plugin-id)

API plugin used for compatibility check

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.ligoj.plugin%3Aplugin-id&metric=coverage)](https://sonarcloud.io/dashboard?id=org.ligoj.plugin%3Aplugin-id)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?metric=alert_status&project=org.ligoj.plugin:plugin-id)](https://sonarcloud.io/dashboard/index/org.ligoj.plugin:plugin-id)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/abf810c094e44c0691f71174c707d6ed)](https://www.codacy.com/gh/ligoj/plugin-id?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ligoj/plugin-id&amp;utm_campaign=Badge_Grade)
[![CodeFactor](https://www.codefactor.io/repository/github/ligoj/plugin-id/badge)](https://www.codefactor.io/repository/github/ligoj/plugin-id)
[![License](http://img.shields.io/:license-mit-blue.svg)](http://fabdouglas.mit-license.org/)

[Ligoj](https://github.com/ligoj/ligoj) Identity plugin
This a very complex plugin managing security constraints depending on the authorization of current user and are
massively based on RBAC and delegates.
Provides the following features :

- User, group and company management
- Scope of container (group and company) to name macro set of containers
- Delegates to user/group/company of a subset of current user depending on propagation constraints
- Activity export for a group

Dashboard features :

- Amount of users in the linked group

Related plugins:

- [plugin-id-sql](https://github.com/ligoj/plugin-id-sql)
- [plugin-id-cognito](https://github.com/ligoj/plugin-id-cognito)
- [plugin-id-ldap](https://github.com/ligoj/plugin-id-ldap)
- [plugin-id-ldap-embedded](https://github.com/ligoj/plugin-id-ldap-embedded)

# Plugin parameters

| Parameter               | Scope              | Default | Note                                                          |                     
|-------------------------|--------------------|---------|---------------------------------------------------------------|
| service:id:user-display | Global             | `id`    | Displayed username mode: `id` (login), `mail` (first attached mail, fallback `id`), `mail-short` (`mail` without the domain part), any user attribute name (`firstName`, `lastName`, `company`, ... — also resolved in the `customAttributes` map, fallback `id`), or an expression combining `${token}` placeholders and literal text, e.g. `${firstName} ${lastName}` (each token is one of the previous modes). Whenever the mode or a token cannot be resolved for a user (missing attribute, no mail), the displayed name is this user's visual identifier (`service:id:visual-id-name`, `id` by default). The `${...}` placeholders are for the UI only: this value is forwarded raw, never resolved by Spring. |
| service:id:visual-id-name | Global | `id` | Since Ligoj 5.0 (plugin-id 5.0.2). Attribute displayed as the user's identifier in the UI (table first column, implicit sort): `id`, `firstName`, `lastName`, `mail` (first one), or `customAttributes.<property>` (looked up in `SimpleUser#customAttributes`). The `visual-id` sort key of `UserOrgResource#findAll` maps to this attribute; the LDAP repository sorts custom attributes with `id` fallback. The custom attribute must be named exactly as declared by the identity provider (case-sensitive), otherwise the login is used and a warning is logged. |
| service:id:visual-id-label | Global | - | Static (non localizable) label displayed for the visual identifier column. When undefined, the UI localizes the `visual-id-name` value. |
| service:id:uid-pattern  | Node, Subscription | `.*`    | Pattern determining the login is valid for an authentication. |
| service:id:people-custom-attributes | Node | - | Custom user attribute names, comma or space separated, loaded by the identity tool (LDAP…) and edited in the user dialog. Replaces the tool-level `service:id:ldap:people-custom-attributes`. |
| service:id:read-only-attributes | Node | - | User attributes that cannot be updated after creation, comma or space separated: `firstName`, `lastName`, `company`, `department`, `localId`, `mail` or `customAttributes.<name>`. The API refuses such an update and the user dialog shows the fields read-only. |
| service:id:ou           | Node, Subscription | `null`  | Parent OU.                                                    |
| service:id:group        | Node, Subscription | `null`  | Normalized Group name (CN).                                   |
| service:id:parent-group | Node, Subscription | `null`  | Normalized parent Group name (CN).                            |

> Node parameters are read once per node and kept in the `id-configuration` cache; the directory data (users, groups, companies) is kept by the tool plugin (for LDAP, the `id-ldap-data` cache). After changing the parameters of an identity node (custom attributes, base DN, filters…), invalidate these caches from the cache administration page or with `ligoj cache invalidate`, or restart the API. Changes of `service:id:user-display`, `service:id:visual-id-name` and `service:id:visual-id-label` apply at the next login.
