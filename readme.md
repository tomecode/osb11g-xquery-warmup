# osb11g-xquery-warmup
Recompile all XQueries in OSB 11g at runtime

# What is it
  - a simple application (packaged as EAR) deployed in OSB/WLS
  - activated when the OSB starting or XQuery is/was deployed (or if something is/was changed in XQuery and session is/was activated)
  - pre-compile/execute Xquery with dummy arguments - warm-up 

# Install
  - Download package
  - Deploy it as enterprise application: _'Install this deployment as an application'_
  - Deployment Order: 20
  - Restart the OSB.

# Development
  - Build is based on the Maven
  - JVM 1.6
  - in pom.xml change path for following properties:
| Plugin | README |
| fmw11gHome | path to FMW 11g home |
| wls11gHome | path to FMW WLS 11g home |
| osb11gHome | path to FMW OSB 11G home |
  - mvn clean install
