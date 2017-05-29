## :link: Ligoj Identity plugin [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ligoj.plugin/plugin-id/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ligoj.plugin/plugin-id) [![Download](https://api.bintray.com/packages/ligoj/maven-repo/plugin-id/images/download.svg) ](https://bintray.com/ligoj/maven-repo/plugin-id/_latestVersion)
API plugin used for compatibility check

[![Build Status](https://travis-ci.org/ligoj/plugin-id.svg?branch=master)](https://travis-ci.org/ligoj/plugin-id)
[![Build Status](https://circleci.com/gh/ligoj/plugin-id.svg?style=svg)](https://circleci.com/gh/ligoj/plugin-id)
[![Build Status](https://codeship.com/projects/59d0b6a0-ef12-0134-dc5d-06835e321a69/status?branch=master)](https://codeship.com/projects/208765)
[![Build Status](https://semaphoreci.com/api/v1/ligoj/plugin-id/branches/master/shields_badge.svg)](https://semaphoreci.com/ligoj/plugin-id)
[![Build Status](https://ci.appveyor.com/api/projects/status/5926fmf0p5qp9j16/branch/master?svg=true)](https://ci.appveyor.com/project/ligoj/plugin-id/branch/master)
[![Coverage Status](https://coveralls.io/repos/github/ligoj/plugin-id/badge.svg?branch=master)](https://coveralls.io/github/ligoj/plugin-id?branch=master)
[![Dependency Status](https://www.versioneye.com/user/projects/58caeda8dcaf9e0041b5b978/badge.svg?style=flat)](https://www.versioneye.com/user/projects/58caeda8dcaf9e0041b5b978)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=org.ligoj.plugin:plugin-id)](https://sonarqube.com/dashboard/index/org.ligoj.plugin:plugin-id)
[![Sourcegraph Badge](https://sourcegraph.com/github.com/ligoj/plugin-id/-/badge.svg)](https://sourcegraph.com/github.com/ligoj/plugin-id?badge)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/abf810c094e44c0691f71174c707d6ed)](https://www.codacy.com/app/ligoj/plugin-id?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ligoj/plugin-id&amp;utm_campaign=Badge_Grade)
[![CodeFactor](https://www.codefactor.io/repository/github/ligoj/plugin-id/badge)](https://www.codefactor.io/repository/github/ligoj/plugin-id)
[![License](http://img.shields.io/:license-mit-blue.svg)](http://gus.mit-license.org/)

[Ligoj](https://github.com/ligoj/ligoj) Identity plugin
This a very complex plugin managing a lot security constraints depending on the authorization of current user and are massively based on RBAC and delegates.
Provides the following features :
- User, group and company management
- Scope of container (group and company) to name macro set of containers
- Delegates to user/group/company of a subset of current user depending on propagation constraints
- Activity export for a group

Dashboard features :
- Amount of users in the linked group