

**Document Purpose:** This document provides detailed explanations for each decision and discussion point from the ECR Enablement MVP planning sessions. Use this as a reference when discussing with the team.

---

## Table of Contents

1. [Strategic Context](#1-strategic-context)
2. [Architecture Decisions](#2-architecture-decisions)
3. [Credential & Security Model](#3-credential--security-model)
4. [Feature Flag Strategy](#4-feature-flag-strategy)
5. [Ownership Model](#5-ownership-model)
6. [Build Workflow Design](#6-build-workflow-design)
7. [Service Catalog Integration](#7-service-catalog-integration)
8. [Scope Boundaries](#8-scope-boundaries)

---

## 1. Strategic Context

### 1.1 "Nova is the single self-serve console"

**What this means:**
Nova is SunLife's internal developer platform (built on Backstage) that serves as the ONE place where developers go to manage their services, infrastructure, and workflows.

**Why this matters for ECR:**
- Instead of developers logging into AWS Console to view ECR repositories, they should see this information in Nova
- This reduces context switching and improves developer productivity
- Centralizes access control and audit logging

**Business Driver:**
- Reduce AWS console dependency
- Improve security posture (fewer people need direct AWS access)
- Better developer experience (everything in one place)

---

### 1.2 "AWS console should trend to read-only"

**What this means:**
Over time, developers should primarily use Nova for actions (triggering builds, viewing status) while AWS Console becomes a read-only view for troubleshooting or advanced use cases.

**Why this decision was made:**
- Direct AWS Console access is harder to audit
- Permissions in AWS are complex; Nova can simplify with ownership-based access
- Reduces risk of accidental changes in AWS

**Practical Impact:**
- ECR MVP focuses on BUILD actions via Nova (not repository creation)
- Viewing ECR images will be in Nova/Backstage
- Repository creation stays in Service Catalog (controlled provisioning)

---

## 2. Architecture Decisions

### 2.1 "ECR fits primarily in Backstage via Component/System views"

**What this means:**
In Backstage, every microservice/application is registered as a "Component" in the catalog. Components belong to "Systems" (logical groupings). ECR repositories will be linked to these Components.

**Example:**
```
System: payments-platform
├── Component: payment-service      → ECR: payment-service-repo
├── Component: payment-gateway      → ECR: payment-gateway-repo
└── Component: payment-notifications → ECR: payment-notifications-repo
```

**Why this approach:**
- Leverages existing Backstage catalog structure
- Ownership is already defined at Component level
- Natural place for developers to find their ECR info

**How it works:**
- Each Component's `catalog-info.yaml` will have an annotation pointing to its ECR repo
- The ECR plugin will read this annotation and display ECR data on that Component's page

---

### 2.2 "Image build/push is a separate lane: Nova UI → Jenkins/WHS pipeline"

**What this means:**
Building Docker images and pushing to ECR is NOT done by Nova directly. Instead:

```
┌─────────────────────────────────────────────────────────────────────┐
│  User clicks "Build Image" in Nova                                  │
│         ↓                                                           │
│  Nova Backend API receives request                                  │
│         ↓                                                           │
│  Nova calls Jenkins/WHS (Webhook Service) API to trigger pipeline   │
│         ↓                                                           │
│  Jenkins runs the actual docker build & push to ECR                 │
│         ↓                                                           │
│  Nova polls Jenkins for status and displays to user                 │
└─────────────────────────────────────────────────────────────────────┘
```

**Why not build directly in Nova:**
- Jenkins pipelines already exist and are battle-tested
- Build infrastructure is complex (workers, caching, security)
- Reusing Jenkins avoids duplicating infrastructure
- Teams already have their Jenkinsfiles configured

**What Nova provides:**
- Nice UI to trigger builds (instead of Jenkins UI)
- Ownership-based access control
- Unified view of build history
- No need to navigate to Jenkins

---

### 2.3 "ECR repo creation is via AWS Service Catalog + Terraform (Felix)"

**What this means:**
When a team needs a NEW ECR repository, they don't create it manually or via Nova. They use AWS Service Catalog, which runs Terraform (via the "Felix" engine) to provision it with proper configuration.

**Why this approach:**
- Service Catalog ensures standardized repository configuration
- Proper tagging, lifecycle policies, encryption settings
- Terraform provides infrastructure-as-code benefits
- Audit trail of who provisioned what

**What Nova does NOT do in MVP:**
- ❌ Create ECR repositories
- ❌ Delete ECR repositories
- ❌ Modify repository settings

**What Nova DOES do:**
- ✅ Display repository info (from Service Catalog + ECR)
- ✅ Link to Service Catalog for repo creation/modification
- ✅ Trigger image builds for existing repos

---

## 3. Credential & Security Model

### 3.1 "Avoid global super-user with access to all repos"

**The Problem We're Avoiding:**
A common (bad) pattern is having ONE set of credentials that can access ALL git repositories and ALL ECR repositories. This is dangerous because:
- If compromised, attacker has access to everything
- No way to audit which team accessed what
- Violates principle of least privilege

**The Solution:**
```
┌──────────────────────────────────────────────────────────────────────┐
│  WRONG (What we're avoiding):                                        │
│  ┌─────────────────────────┐                                         │
│  │  Global Service Account │──→ Access to ALL repos                  │
│  └─────────────────────────┘                                         │
├──────────────────────────────────────────────────────────────────────┤
│  RIGHT (What we're implementing):                                    │
│  ┌─────────────────────────┐                                         │
│  │  Team A Credential      │──→ Access to Team A's repos only        │
│  └─────────────────────────┘                                         │
│  ┌─────────────────────────┐                                         │
│  │  Team B Credential      │──→ Access to Team B's repos only        │
│  └─────────────────────────┘                                         │
│  ┌─────────────────────────┐                                         │
│  │  Team C Credential      │──→ Access to Team C's repos only        │
│  └─────────────────────────┘                                         │
└──────────────────────────────────────────────────────────────────────┘
```

---

### 3.2 "Team-provided read-only SSH key/token (project/repo scoped)"

**What this means:**
Each team provides their OWN git credentials (SSH key or personal access token) that:
- Is READ-ONLY (can clone, cannot push to git)
- Is scoped to only their repositories (max 5 repos per credential)
- Is owned and managed by the team (they can rotate it)

**Why read-only:**
- Build process only needs to CLONE the repo
- Push to ECR uses AWS IAM, not git credentials
- Reduces blast radius if credential is exposed

**Why team-provided:**
- Teams control their own credentials
- Teams can rotate without involving platform team
- Clear ownership and accountability

---

### 3.3 "Stored securely; never log secrets"

**Storage Requirements:**
```
┌─────────────────────────────────────────────────────────────────────┐
│  Credentials stored in:                                             │
│  • HashiCorp Vault (preferred)                                      │
│  • AWS Secrets Manager (alternative)                                │
│                                                                     │
│  NOT stored in:                                                     │
│  • Environment variables                                            │
│  • Config files                                                     │
│  • Database                                                         │
│  • Git repository                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

**"Never log secrets" explained:**
When Nova retrieves a credential to pass to Jenkins, the actual credential value must NEVER appear in:
- Application logs
- API responses
- Error messages
- Debug output
- Monitoring dashboards

**Example of WRONG logging:**
```
❌ log.info("Retrieved credential: ghp_abc123xyz789...")
❌ log.error("Failed to authenticate with token: ghp_abc123xyz789...")
```

**Example of CORRECT logging:**
```
✅ log.info("Retrieved credential for repo: my-service, team: payments")
✅ log.error("Failed to authenticate for repo: my-service. Request ID: abc-123")
```

---

## 4. Feature Flag Strategy

### 4.1 "Backstage ECR plugin is read-only; integrate behind feature flag"

**What is the ECR Plugin:**
A Backstage plugin that displays ECR repository information (images, tags, scan results) on a Component page. It only READS from ECR, doesn't write.

**Why behind a feature flag:**
```
┌─────────────────────────────────────────────────────────────────────┐
│  Without Feature Flag:                                              │
│  • Deploy plugin → Everyone sees it immediately                     │
│  • Bug found → Must rollback deployment (risky, slow)               │
│  • Can't do gradual rollout                                         │
├─────────────────────────────────────────────────────────────────────┤
│  With Feature Flag:                                                 │
│  • Deploy plugin → Nobody sees it (flag is OFF)                     │
│  • Turn ON for admin group → Admins test it                         │
│  • Bug found → Turn OFF flag (instant, no deployment)               │
│  • Gradually enable for more users                                  │
└─────────────────────────────────────────────────────────────────────┘
```

**Rollout Plan:**
1. **Phase 1:** Flag OFF for everyone (deployed but invisible)
2. **Phase 2:** Flag ON for Platform/Admin team only
3. **Phase 3:** Flag ON for early adopter teams (25%)
4. **Phase 4:** Flag ON for everyone (GA)

---

### 4.2 "Does not block release trains"

**What this means:**
Nova has regular release schedules ("release trains"). The ECR feature should not prevent other features from being released.

**How feature flags help:**
- ECR code can be merged and deployed even if not ready for users
- Flag keeps it hidden until ready
- Other teams' features can ship on schedule
- No "big bang" release with lots of risk

---

## 5. Ownership Model

### 5.1 "Ownership is the core model"

**What is ownership in Backstage:**
Every Component in the Backstage catalog has an "owner" field that specifies which team owns it.

```yaml
# catalog-info.yaml
apiVersion: backstage.io/v1alpha1
kind: Component
metadata:
  name: payment-service
spec:
  owner: team-payments  # ← This team owns this component
  system: payments-platform
```

**Why ownership matters for ECR:**
- Only owners can trigger builds for their components
- Only owners can view sensitive information
- Audit logs track who (from which team) did what
- No need for separate permission management

**Access Control Based on Ownership:**
```
┌─────────────────────────────────────────────────────────────────────┐
│  User: alice@sunlife.com                                            │
│  Groups: [team-payments, all-engineers]                             │
│                                                                     │
│  Component: payment-service                                         │
│  Owner: team-payments                                               │
│                                                                     │
│  → Alice IS in team-payments                                        │
│  → Alice CAN trigger builds for payment-service ✅                  │
├─────────────────────────────────────────────────────────────────────┤
│  User: bob@sunlife.com                                              │
│  Groups: [team-inventory, all-engineers]                            │
│                                                                     │
│  Component: payment-service                                         │
│  Owner: team-payments                                               │
│                                                                     │
│  → Bob is NOT in team-payments                                      │
│  → Bob CANNOT trigger builds for payment-service ❌                 │
│  → Bob CAN view basic info (read-only)                              │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 5.2 "Tagging/Annotations Contract"

**Two-way linking between AWS and Backstage:**

**AWS Side (ECR Resource Tags):**
```
ECR Repository: payment-service-repo
Tags:
  backstage.io/component: payment-service
  backstage.io/system: payments-platform
  backstage.io/owner: team-payments
  cost-center: CC-12345
```

**Backstage Side (Catalog Annotations):**
```yaml
# catalog-info.yaml
metadata:
  annotations:
    amazon.com/ecr-repository-arn: "arn:aws:ecr:us-east-1:123456789:repository/payment-service-repo"
    amazon.com/ecr-repository-name: "payment-service-repo"
    nova.sunlife.com/build-pipeline: "jenkins/payment-service-build"
```

**Why both:**
- AWS tags let us query "which ECR repos belong to this Backstage component"
- Backstage annotations let the UI know "where is this component's ECR repo"
- Enables two-way navigation and validation

---

## 6. Build Workflow Design

### 6.1 "Explicit Dockerfile selection (path/name)"

**What this means:**
When triggering a build, the user MUST specify which Dockerfile to use. The system will NOT try to auto-detect it.

**Example - User must provide:**
```
┌─────────────────────────────────────────────────────────────────────┐
│  Build Image Modal                                                  │
│  ─────────────────                                                  │
│  Git Repository: git@github.com:sunlife/payment-service (readonly)  │
│  Git Branch:     [main_____________]                                │
│  Dockerfile:     [./docker/Dockerfile.prod____] ← User must specify │
│  Image Tag:      [v1.2.3___________]                                │
│                                                                     │
│  [Cancel]                                    [Build]                │
└─────────────────────────────────────────────────────────────────────┘
```

**Why NOT auto-detect:**
A repo might have multiple Dockerfiles:
```
my-service/
├── Dockerfile                 # Development
├── Dockerfile.prod            # Production
├── docker/
│   ├── Dockerfile.api         # API service
│   └── Dockerfile.worker      # Background worker
```

Auto-detecting which one to use is complex and error-prone. For MVP, we keep it simple: user tells us exactly which file.

---

### 6.2 "Avoid smart git diff auto-detect in MVP"

**What we're NOT building:**
Some systems automatically detect "what changed" in git and only build affected Dockerfiles. This requires:
- Parsing git history
- Understanding which files affect which Dockerfile
- Handling monorepo complexities

**Why skip for MVP:**
- Adds significant complexity
- Edge cases are hard to handle
- Can be added post-MVP if needed
- Explicit selection is clear and predictable

**Post-MVP possibility:**
```
"Smart mode" could suggest: "Detected changes in ./src/api, would you like to build ./docker/Dockerfile.api?"
```

---

## 7. Service Catalog Integration

### 7.1 "Nova may integrate later, but MVP needs alignment"

**Current State:**
- ECR repos are created via Service Catalog (Terraform/Felix)
- Nova doesn't create repos; it uses existing ones

**MVP Alignment Needed:**
- Understand Service Catalog API for ECR products
- Ensure tags applied by Service Catalog match our ownership model
- Display Service Catalog provisioning info in Nova

**Future Possibility (Post-MVP):**
```
Nova could potentially:
• Trigger Service Catalog to create new ECR reposa
• Display "Create New Repository" button that launches Service Catalog workflow
```

---

### 7.2 "Repositories without Service Catalog origin"

**The Problem:**
Some ECR repos were created manually or via other tools before Service Catalog existed. These "legacy" repos won't have Service Catalog metadata.

**How Nova Handles This:**
```
┌─────────────────────────────────────────────────────────────────────┐
│  Repository: legacy-service-repo                                    │
│  ─────────────────────────────────────────────────────────────────  │
│  Provisioning Status: "Provisioned externally"                      │
│  Terraform Workspace: N/A                                           │
│  Last Deployment: N/A                                               │
│                                                                     │
│  ℹ️ This repository was not created via Service Catalog.            │
│     [Request Migration to Service Catalog]                          │
│                                                                     │
│  ─────────────────────────────────────────────────────────────────  │
│  Images: (still displayed normally from ECR API)                    │
│  • latest - pushed 2 hours ago                                      │
│  • v1.2.3 - pushed 3 days ago                                       │
└─────────────────────────────────────────────────────────────────────┘
```

**Key Point:**
Even without Service Catalog data, we still show ECR data. Graceful degradation, not failure.

---

## 8. Scope Boundaries

### 8.1 What's IN Scope for MVP

| Feature | Description | Why Included |
|---------|-------------|--------------|
| ECR Plugin (read-only) | View images, tags, scan results | Core visibility requirement |
| Build Image trigger | POST API to start Jenkins build | Core action requirement |
| Build status tracking | GET API to check build progress | Users need feedback |
| Feature flags | Control rollout | Risk mitigation |
| Ownership-based access | Only owners can build | Security requirement |
| Audit logging | Track all actions | Compliance requirement |
| Team-scoped credentials | Per-team git access | Security requirement |

---

### 8.2 What's OUT of Scope for MVP

| Feature | Why Excluded | When to Consider |
|---------|--------------|------------------|
| ECR repo creation | Complex, Service Catalog handles it | Post-MVP if needed |
| Smart Dockerfile detection | Adds complexity, edge cases | Post-MVP enhancement |
| Global credentials | Security risk | Never |
| ECR write operations via plugin | Plugin is read-only by design | Evaluate need |
| Automated credential rotation | Nice-to-have, not critical | Post-MVP |
| Advanced analytics | Not core functionality | Post-MVP |

---

### 8.3 Why These Boundaries Matter

**Risk Reduction:**
MVP scope is deliberately minimal to reduce:
- Development time
- Testing complexity
- Security surface area
- Things that can go wrong

**Iterative Approach:**
```
MVP (8 weeks)
    ↓
Feedback & Metrics
    ↓
MVP+1 (add features based on feedback)
    ↓
MVP+2 (continue iteration)
```

**Scope Creep Warning:**
The #1 risk identified is "scope creep" - stakeholders asking for features beyond MVP. The Product Owner must enforce boundaries.

**Common Scope Creep Requests to Push Back On:**
- "Can we also auto-detect which Dockerfile changed?"
- "Can we create ECR repos from Nova?"
- "Can we add support for other registries (DockerHub, GCR)?"
- "Can we add image scanning/vulnerability management?"

**Correct Response:**
"Great idea! Let's add it to the backlog for post-MVP consideration after we validate the core workflow."

---

## Summary: Key Points to Remember

### For the Team Discussion:

1. **Nova = Single Console** - Developers should use Nova, not AWS Console
2. **Read First, Action Second** - ECR plugin is read-only; builds go through Jenkins
3. **Ownership = Access** - Your team owns it? You can build it. Otherwise, read-only.
4. **No Super-User Credentials** - Each team manages their own scoped credentials
5. **Feature Flags = Safety** - We can turn things off instantly if problems arise
6. **Explicit > Magic** - Users specify Dockerfile path; no auto-detection in MVP
7. **Service Catalog for Provisioning** - Nova doesn't create repos; it uses existing ones
8. **Graceful Degradation** - Legacy repos still work, just with less metadata
9. **MVP = Minimal** - Resist scope creep; iterate after launch
10. **Security First** - Never log credentials; audit everything

---

## Questions for Team Discussion

Use these to drive your next meeting:

1. **Credential Onboarding:** How do teams register their git credentials? (UI? ServiceNow? CLI?)

2. **Default Values:** What should be the default Dockerfile path? (`./Dockerfile` or require explicit entry?)

3. **Error Messages:** How verbose should error messages be? Include request ID for support?

4. **Legacy Repos:** What percentage of ECR repos are legacy vs. Service Catalog provisioned?

5. **Jenkins API:** Do we have documentation for the Jenkins/WHS API we're wrapping?

6. **Feature Flag Service:** Which service are we using? (LaunchDarkly? ConfigCat? Custom?)

7. **Timeline Feasibility:** Is 8 weeks realistic given dependencies on other teams?

---

*Document created: December 17, 2025*
*For: ECR MVP Team Discussion*
