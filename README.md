## :link: Ligoj Identity plugin [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ligoj.plugin/plugin-id/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ligoj.plugin/plugin-id) [![Download](https://api.bintray.com/packages/ligoj/maven-repo/plugin-id/images/download.svg) ](https://bintray.com/ligoj/maven-repo/plugin-id/_latestVersion)
API plugin used for compatibility check

[![Build Status](https://app.travis-ci.com/github/ligoj/plugin-id.svg?branch=master)](https://app.travis-ci.com/github/ligoj/plugin-id)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.ligoj.plugin%3Aplugin-id&metric=coverage)](https://sonarcloud.io/dashboard?id=org.ligoj.plugin%3Aplugin-id)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?metric=alert_status&project=org.ligoj.plugin:plugin-id)](https://sonarcloud.io/dashboard/index/org.ligoj.plugin:plugin-id)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/abf810c094e44c0691f71174c707d6ed)](https://www.codacy.com/gh/ligoj/plugin-id?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ligoj/plugin-id&amp;utm_campaign=Badge_Grade)
[![CodeFactor](https://www.codefactor.io/repository/github/ligoj/plugin-id/badge)](https://www.codefactor.io/repository/github/ligoj/plugin-id)
[![License](http://img.shields.io/:license-mit-blue.svg)](http://fabdouglas.mit-license.org/)

[Ligoj](https://github.com/ligoj/ligoj) Identity plugin
This a very complex plugin managing security constraints depending on the authorization of current user and are massively based on RBAC and delegates.
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
