# Group interactions

This document describes client interactions with the Groups service based on the design in
`design.md` in this folder.

The interactions for the stretch design goals are not included - they will be added if and when
it's clear there is time available for implementation.

## Data Structures

### `Group`:
* Unique immutable ID (UUID) - `GID`
* Owner username - `GOwner`
* Unique mutable name - `GName`
* Mutable group type - `GType`
* Mutable description - `GDesc`
* Mutable list of member users - `GUsers`
* Mutable list of member workspaces / narratives ( May be decorated with
  Narrative / workspace names when appropriate) - `GWS`

`PF` - private fields; `GUsers` and `GWS`

### `Invitation`:
* Unique immutable ID (UUID) - `IID`
* `GID`
* username (optional, included when a specific user is involved) - `IUser`
* Invitation type
* Workspace ID (optional, included when a workspace is involved. May be decorated with
  Narrative / workspace names when appropriate) - `WSID`
* Status - one of `Open`, `Accept`, `Deny`, `Cancel`

## Interactions

### Group creation

* Client sends message to service with
  * Requested `GName`
  * `GType`
  * `GDesc`
* Service returns `Group` with empty `PF`

### Group deletion

* Client sends message to service with `GID`
* Service
  * Deletes `Group`
  * Returns 204
  * Requirement: user must be `GID` owner

### Group view

* Client sends message to service with `GID`
* Service returns `Group`
  * `PF` is empty if user is not a member

### List groups

* Client sends a service message
* Service returns list of `Group`
  * `PF` are not included in the list
  * Assume that this list is always small enough that returning all groups at once is not an issue

* Client sends a service message with `WSID`
* Service returns list of `Group` where:
  * `PF` are not included in the list
  * If user is a `WSID` administrator:
    * `GWS` includes `WSID`
  * Else:
    * `GWS` includes `WSID` and `GUsers` includes user


### Mutate group name, type and description

* Client sends a service message with
  * `GID`
  * `GName` or `null`
  * `GType` or `null`
  * `GDesc` or `null`
  * `null` implies no change
* Service returns 204
  * Requirement: user must be `GOwner`

### View invitation

* Client sends service message with `IID`
* Service returns `Invitation`
  * Requirements: user must be `IUser` or owner of `GID`

### List invitations

* ONE OF:
  * Standard user
    * Client sends a service message
    * Service returns list of `Invitation` for that user
  * Group owner
     * Client sends a service message with `GID`
     * Service returns list of `Invitation` for that `GID`
     * Requirement: user must be `GOwner`

### Add user

* Client sends a service message with
  * `GID`
  * username
* Service
  * Stores `Invitation` with `AddUserToGroup` type
  * Sends invitation to user with `IID` to Feeds service
  * Requirement: user must be `GOwner`
* ONE OF:
  * Accept
    * Client sends a service message with `IID` and `accept`
    * Service
      * Adds user to `Group`
      * Updates `Invitation`
      * Sends message to `GOwner` to Feeds
      * Returns 204
      * Requirement: user must be `IUser`
  * Deny
    * Client sends a service message with `IID` and `deny` and reason (optional)
    * Service
      * Updates `Invitation`
      * Sends message to `GOwner` to Feeds with reason
      * Returns 204
      * Requirement: user must be `IUser`
  * Cancel
    * Client sends a service message with `IID` and `cancel`
    * Service
      * Updates `Invitation`
      * Deletes `IID` from Feeds
      * Returns 204
      * Requirement: user must be `GOwner`

### Request membership

* Client sends a service message with `GID`
* Service
  * Stores `Invitation` with `RequestMembership` type
  * Sends invitation to owner with `IID` to Feeds
* ONE OF:
  * Accept
    * Client sends a service message with `IID` and `accept`
    * Service
      * Adds user to `Group`
      * Updates `Invitation`
      * Sends message to `IUser` to Feeds
      * Returns 204
      * Requirement: user must be `GOwner`
  * Deny
    * Client sends a service message with `IID` and `deny` and reason (optional)
    * Service
      * Updates `Invitation`
      * Sends message to `IUser` to Feeds with reason
      * Returns 204
      * Requirement: user must be `GOwner`
  * Cancel
    * Client sends a service message with `IID` and `cancel`
    * Service
      * Updates `Invitation`
      * Deletes `IID` from Feeds
      * Returns 204
      * Requirement: user must be `IUser`

### Remove user self

* Client sends a service message with `GID`
* Service
  * Removes user from `Group`
  * Sends message to `GOwner` to Feeds
  * Returns 204

### Remove user

* Client sends a service message with
  * `GID`
  * username
* Service
  * Removes user from `Group`
  * DOES NOT send a message to feeds (or optionally does?)
  * Returns 204
  * Requirement: user must be owner of `GID`

### Add workspace

* Client sends a service message with
  * `GID`
  * `WSID`
* Service
  * ONE OF:
    * If user is `GID` owner and `WSID` administrator, service:
      * Adds `WSID` to `Group`
      * Sends message to `WSID` administrators to Feeds
      * Returns 204
    * If user is `GID` owner
      * Service:
        * Stores `Invitation` with `RequestAddToGroup` type
        * Sends invitation to `WSID` admins who are members of `GID` with `IID` to Feeds
        * Requirement: user must have at least read access to `WSID`
        * Requirement: at least one `WSID` admin must be a member of `GID`
      * ONE OF:
        * Accept
          * Client sends a service message with `IID` and `accept`
          * Service
            * Adds `WSID` to `Group`
            * Updates `Invitation`
            * Sends message to `WSID` administrators and `GOwner` to Feeds
            * Returns 204
            * Requirement: user must be `WSID` administrator and `GID` member
        * Deny
          * Client sends a service message with `IID` and `deny` and reason (optional)
          * Service
            * Updates `Invitation`
            * Sends message to `WSID` administrators who are members of `GID` and
              `GOwner` to Feeds with reason
            * Returns 204
            * Requirement: user must be `WSID` administrator and `GID` member
        * Cancel
          * Client sends a service message with `IID` and `cancel`
          * Service
            * Updates `Invitation`
            * Deletes `IID` from Feeds
            * Returns 204
            * Requirement: user must be `GOwner`
    * If user is `WSID` administrator and `GID` member
        * Service:
          * Stores `Invitation` with `RequestAddWorkspace` type
          * Sends invitation to `GOwner` with `IID` to Feeds
        * ONE OF:
          * Accept
            * Client sends a service message with `IID` and `accept`
            * Service
              * Adds `WSID` to `Group`
              * Updates `Invitation`
              * Sends message to `WSID` administrators to Feeds
              * Returns 204
              * Requirement: user must be `GOwner`
          * Deny
            * Client sends a service message with `IID` and `deny` and reason (optional)
            * Service
              * Updates `Invitation`
              * Sends message to original `WSID` administrator to Feeds with reason
              * Returns 204
              * Requirement: user must be `GOwner`
          * Cancel
            * Client sends a service message with `IID` and `cancel`
            * Service
              * Updates `Invitation`
              * Deletes `IID` from Feeds
              * Returns 204
              * Requirement: user must be original `WSID` administrator
    * Else error

### Remove workspace

* Client sends a service message with
  * `GID`
  * `WSID`
* Service
  * Removes `WSID` from `Group`
  * Sends message to `GOwner` and `WSID` administrators to Feeds
    * Except requester
  * Returns 204
  * Requirement: user must be owner of `GID` or `WSID` administrator

### Administration

* KBase administrators can bypass all requirements and can always see `PF`, but must specify
  they are acting in an administrative capacity by including that in the service message.

## Implementation notes

* When a `Group` is deleted, delete all related `Invitation`
* When accessing `Invitation`, check that `Group` exists. If not, delete `Invitation`
