# osb11g-xquery-warmup
Recompile/warm-up any XQueries at runtime in the OSB 11g

# What is it
  - a simple application (packaged as EAR) deployed in OSB/WLS
  - activated when the OSB starting or XQuery is/was deployed (or if something is/was changed in XQuery and session is/was activated)
  - pre-compile/execute Xquery with dummy arguments - warm-up 

# Install
  - Download latest version of application...
  - Deploy it as enterprise application: _'Install this deployment as an application'_
  - Deployment Order: 80
  - Restart the OSB.

# Development
  - Build is based on the Maven
  - JVM 1.6
  - in pom.xml change path for following properties:
| Plugin | README |
| wls11gHome | path to FMW WLS 11g home |
| osb11gHome | path to FMW OSB 11G home |
  - mvn clean install
