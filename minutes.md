Topic 1: CCE On-Call Knowledge Base & Roster

Objective

Set up a clear CCE on-call process, routing guide, troubleshooting knowledge base, logbook process, and roster model.

Key Discussion Points

* CCE will act as Level 2 support for EKS/container platform issues.
* CCOE Tier 1 should perform initial triage before escalating to CCE.
* Misrouted incidents should be documented and used as feedback to improve routing.
* A Bitbucket static site will be used as the main knowledge base.
* Documentation will be written in Markdown and built using Docusaurus.
* The site will include:
    * On-call escalation guide
    * Cluster inventory
    * Platform vs application issue guide
    * Troubleshooting runbooks
    * Incident logbooks
    * Standard triage questions
    * Contact/escalation details
    * Onboarding material

On-Call Roster

* Three rosters were discussed:
    * India business-hours roster
    * North America business-hours roster
    * Off-hours paid standby/on-call roster
* Off-hours rotation runs Tuesday to Tuesday.
* Weekend coverage is 24x7 under the same off-hours roster.
* P1/P2/P3 incidents are in scope for off-hours support.
* If called off-hours, minimum callout is expected to be 2 hours.
* Timesheets should be submitted on time, typically by Friday.

Documentation / Logbook Process

* Every incident or troubleshooting activity should be documented.
* Even non-CCE or misrouted incidents should be captured with validation steps.
* Engineers can use the custom on-call docs/Copilot agent to convert notes into Markdown logbooks.
* PRs should be raised into develop.
* Incidents should be reviewed in release/review meetings.

Routing Guidance

Tier 1 should confirm:

* Is this an EKS cluster?
* Is it managed by CCE?
* Is it platform or application related?
* Is Asia Container Squad required?
* Is this a production P1/P2/P3 issue?
* Has the right team already been engaged?

Action Items

Owner

Action

Sandeep

Convert Confluence troubleshooting docs to Markdown

Sandeep

Fix image/assets issue during Markdown migration

Sandeep

Maintain/update cluster inventory automation

Pratik

Create standard incident questions / first-response checklist

Dan/Shraman

Clean up routing guide and escalation wording

Team

Add historical incidents and troubleshooting notes

Team

Complete/review incident management training

Team

Add quick links to front page



opic 2: EKS Namespace Request from Nova Portal / Backstage

Objective

Discuss how EKS namespace requests should be handled through Nova portal/Backstage and how to make the workflow safer, structured, and less manual.

Key Discussion Points

* Namespace request flow should be handled through Nova/Backstage instead of manual coordination.
* This should reduce risk by enforcing a standard workflow and validation.
* The team discussed this as a POC with a target around June 5.
* The flow may require integration with existing GitOps/Bitbucket processes.
* Backstage/Nova should collect required request fields and generate/update the required configuration.
* Permissions and cluster access need to be reviewed carefully.
* Similar work or patterns may already exist in OpenShift or related platform teams.
* Netskope / security permission concerns were mentioned, especially because some tooling may need broad in-cluster permissions.
* Additional test clusters may be required for validation.

Expected Namespace Request Information

The Nova/Backstage form should likely capture:

* Application/team name
* Requested namespace name
* Target EKS cluster
* Environment
* AWS account / region
* Owner AD group
* Support group
* Cost center or billing metadata
* Required resource quota
* Network/access requirements
* Any required platform add-ons or policies
* Change/request reference if required

Technical Considerations

* Namespace creation should avoid direct manual cluster changes where possible.
* Preferred model should be Git-driven:
    * Nova/Backstage captures request
    * Generates a PR or config update
    * Team reviews/approves
    * Pipeline applies changes
* Need to decide whether namespace creation is:
    * Fully self-service
    * Approval-based self-service
    * Platform-team assisted POC first
* RBAC, quotas, labels, annotations, and ownership metadata should be standardized.
* The workflow should prevent inconsistent namespace setup across clusters.

Action Items

Owner

Action

Team

Define required fields for Nova namespace request form

Team

Review permission/security model for namespace automation

Sandeep

Support POC/testing and documentation

Platform/CCE

Decide GitOps vs direct API execution model

Team

Validate whether extra test cluster is required

Team

Compare with OpenShift namespace request process

Team

Track blockers and provide updates in daily stand-ups
