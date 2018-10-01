# KBase Groups design

## Document Purpose

Provide a design for groups (e.g. organizations, projects, teams, etc) within KBase.

## Design

### Group Properties

  * A permanent unique ID generated at creation
    * UUID or, less preferably, an autoincrementing integer
  * An owner
    * With the possibility for system administrators to reassign the owner
    * Only the owner may delete a group, which is permanent
  * A mutable, informational-only type (Organization, Project, Team, ...)
  * A mutable unique name
  * A mutable description
  * A mutable list of members
    * Needs an invite & accept mechanism (feeds)
  * A mutable list of workspaces / narratives that are part of the group
    * List mutation:
      * To add a workspace to a group, a group administrator must approve. 
      * Should a workspace administrator also approve? Adding a workspace to a group
        implies giving the group administrators some administrative ability for the
        workspace.
        * If so, this implies a 2 step addition process where a group administrator
          requests access to the workspace and a workspace administrator approves.
        * Alternatively, require that the user be an administrator of both.
      * At minimum, a group administrator must have read access to the workspace.
    * If a workspace is deleted, it is automatically removed from the group.
    * Workspace administrators can always remove their workspace from the group.
  * Stretch properties:
    * A list of group administrators that is mutable by the owner
      * Mutable properties other than this list are mutable by administrators unless
        otherwise noted
      * Needs an invite & accept mechanism (feeds)
    * Mutable, typed informational-only relationships to other groups (RelatedTo,
      ParentOf, ...)
      * To relate groups, the user must be an administrator of both groups
        * Otherwise prepare for all your groups to be RelatedTo the ICR
        * To remove a relationship, the user must be an administrator of one of
          the groups
    * A mutable list of apps
      * Admins and app owners can requests addition
      * Who approves? Admin and app owner?
    * A mutable icon

All properties are public except the list of workspaces and list of members. The list of
administrators and owner are public. System administrators can view all properties when they
request to be viewed as system administrators and can reassign owners.

### Interactions with ACLs
There are many potential models for interaction with system ACLs. Here we present 3 options,
from most to least complication and effort.

#### 1. System-wide understanding of groups
In this design, the system components are group-aware and will allow a user to access data if
a group grants them access, regardless of their direct access via system ACLs. There are a
number of services that would need updates, including 3rd-party services (e.g. Shock),
and it’s not clear that only data-owning services (e.g. the workspace, njs, catalog,
user profile, metrics, auth, etc.) would need updates, although that seems probable. 

This approach would likely take several months, if not quarters, of work including an
extensive deployment effort. Given the required timeline this approach is not feasible.

#### 2. Automatic ACL updates
In this design, as users and workspaces are added to a group, the service that maintains groups
sets permissions (via a workspace administration account) for said users on said workspaces.
In this case, per workspace group permissions (either read or write) could be set in the group
such that some group workspaces are read only.

The drawback to this approach is that there is no knowledge of whether the workspace
administrators also granted explicit permissions on a per user to users in the group, and so
if a user or workspace is removed from the group, it is not safe to remove the
relevant permissions. This seems confusing and aggravating for users.

If group-based write access to workspaces is allowed and a 2-step workspace addition process is
not used (see above), the group administrators should have at least write permissions to
the workspace.

It must be made very clear to users that granting access to a user may result in granting access
to other users in groups the user administrates as well. This also seems confusing and unexpected.

#### 3. No direct ACL interactions
In this design, the list of workspaces is informational only, and workspace administrators must
set workspace ACLs explicitly for any group members that require access. This process could be
mediated by the Feeds project - for example, a user may activate a control that sends a
notification to the administrator(s) of a workspace(s) that the user wishes read (or write)
access to a workspace(s) in a group. The workspace administrator(s) could then determine whether
to add the user, and their decision communicated to the user via the Feeds system.

This design is probably the least confusing for workspace administrators, as they are explicitly
approving all ACL changes and allowing a user access to their workspace does not mean that user
will be able to add other users via the groups mechanism. However, it means more work as they
are explicitly approving all ACL changes (and users must request all ACL changes). It also
reduces clutter for group members, as only the workspaces for which they have explicitly
requested access appear in their dashboard, data lists, and search, as opposed to all the
workspaces in all their groups.

**Decision: Option 3.**

## Design implications

* The service that manages groups will need at least a read-only workspace administration
  account to display information about the workspaces with which it is associated.
* ~~If the service changes workspace permissions, it will need a full administration account.~~
* It is recommended, but not required, that we finish the workspace read only accounts and
  move account details to the auth service.
* The workspace will need a bulk method for retrieving workspace information.
* The implementation will need to account for group and workspace deletions carefully due to
  group to group and group to workspace relationships.
* We should expect no more than on the order of 10000 groups, most likely, so database space
  is probably not a concern.
* Users will want to find groups based on a text search of name or description and via the
  users list for groups where they’re members. 
* This could be implemented directly in the service or
  * Delegated to search2 (yikes)
    * This would require implementing the EventHandler interface for search2 and extensive testing

## Dependencies
* The workspace service changes as noted above
* Potentially integration with the Feeds system

