# Contributing to Query Service is a stateless microservice based on the Spring Boot Chassis. This service exposes data from a PostgreSQL database via OData (V4) queries.

This document describes how to contribute to the Query Service, which is a stateless microservice based on the Spring Boot Chassis. This service exposes data from a PostgreSQL database via OData (V4) queries.

## How to submit a feature request
Contact the PO mentioned in the [README.md]  
Please provide a feature description and why this should be added to this service. Also, please describe the definition of done (DoD) criteria.

## Gerrit Project Details  
Artifacts are stored in the Gerrit project: [OSS/com.ericsson.oss.air/eric-oss-pm-stats-query-service](https://gerrit-gamma.gic.ericsson.se/#/admin/projects/OSS/com.ericsson.oss.air/eric-oss-pm-stats-query-service)
  
### Documents

The documentation for this service is located in the [doc folder](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/OSS/com.ericsson.oss.air/eric-oss-pm-stats-query-service/+/master/doc).

To update documents that are not in the referenced folder, contact the service guardians mentioned in the [README.md](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/OSS/com.ericsson.oss.air/eric-oss-pm-stats-query-service/+/master/README.md).

## Contact Information
Contact the PO mentioned in the [README.md]


## Contribution Workflow
1. The **contributor** updates the artifact in the local repository.
2. The **contributor** pushes the update to Gerrit for review, including a reference to the JIRA.
3. The **contributor** invites the **service guardians** (mandatory) and **other relevant parties** (optional) to the Gerrit review, and makes no further changes to the document until it is reviewed.
4. The **service guardians** review the document and give a code-review score.
The code-review scores and corresponding workflow activities are as follows:
    - Score is +1
        A **reviewer** is happy with the changes but approval is required from another reviewer.
    - Score is +2
        The **service guardian** accepts the change and merges the code to master branch. The Publish pipeline is initiated to make the change available to consumers.
    - Score is -1 or -2
        The **contributor** follows-up on reviewer comments, until changes approved by service guardian.
    - The **service guardian** and the **contributor** align to determine when and how the change is published.

   [README.md] (https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/OSS/com.ericsson.oss.air/eric-oss-pm-stats-query-service/+/master/README.md)