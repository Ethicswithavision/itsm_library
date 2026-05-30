KS Buddy is a VS Code extension to troubleshoot and operate EKS clusters using plain English instead of repeatedly running kubectl commands.

It supports:

* Cluster health checks
* Pod logs and pod diagnostics
* Deployment health
* Cluster events
* Argo CD sync/drift status
* Gatekeeper/OPA policy violations
* CPU and memory usage
* Multi-cluster/account comparison
* Cost optimization suggestions

Key Value

The tool reduces manual troubleshooting effort by avoiding:

* Context switching across AWS accounts
* Repeated kubectl commands
* Manual log/event analysis
* Dashboard hopping

Expected impact discussed:

* 30–40% reduction in troubleshooting/operational time
* Incident triage from around 30 minutes to 5 minutes
* Faster root cause analysis

Setup Flow

One-time setup:

1. Install VS Code extension.
2. Installer sets dependencies, Python environment, MCP configuration.
3. User adds AWS account and IAM role.
4. Authentication happens through AWS SSO/OIDC.
5. Tool auto-discovers EKS clusters across regions.
6. Clusters are grouped by account and environment tags.

Important Design Principle

Everything should be read-only for now.

The tool should help users understand issues, not make production changes automatically.

Testing / Feedback Needed

You were asked to focus on functionality testing:

* Check what works
* Check what is broken
* Check what needs to be added
* Test AWS profile / SSO flow
* Test cluster discovery
* Test namespace and pod filters
* Test logs, metrics, Argo, Gatekeeper, deployment health
* Test session/token handling
* Test UI refresh behavior
* Test branch/project selection and push flow

Future Ideas Discussed

Potential extensions:

* Networking diagnostics
* Ingress path validation
* DNS resolution checks
* Network policy checks
* Token optimization
* AI usage/cost dashboard
* Reusable scripts library
* CI/CD automation helpers
* Bitbucket PR validator
* Git project analyzer
* Developer task automation
* Internal “skills” or reusable workflows

Key Takeaway

This is not just an EKS tool. The bigger direction is:

Convert repetitive developer/platform tasks into reusable scripts/tools, then expose them through a simple AI-assisted interface.

That means less Copilot prompting every time, less manual work, and more repeatable automation.
