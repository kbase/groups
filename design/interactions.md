# Group interactions

This document describes client interactions with the Groups service based on the design in
`design.md` in this folder.

The interactions for the stretch design goals are not included - they will be added if an when
it's clear there is time available for implementation.

## Data Structures

### `Group`:
* Unique immutable ID (UUID) - `GID`
* Owner username - `GOwner`
* Unique mutable name - `GName`
* Mutable group type - `GType`
* Mutable description - `GDesc`
* Mutable list of member users - `GUsers`
* Mutable list of member workspaces / narratives - `GWS`

`PF` - private fields; `GUsers` and `GWS`

### `Invitation`:
* Unique immutable ID (UUID) - `IID`
* `GID`
* username (optional, omitted if the group owner must respond) `IUser`
* Invitation type
* Workspace ID (optional, included when a workspace is involved. May be decorated with
  Narrative / workspace names when appropriate)

## Interactions

### Group creation

* Client sends message to service with
  * Requested group name
  * Group type
  * Description
* Service returns `Group` with empty `GUsers` and `GWS`

### Group view

* Client sends message to service with `GID`
* Service returns `Group`
  * `PF` is empty if user is not a member

### List groups

* Client sends a service message
* Service returns list of `Group`
  * `PF` are not included in the list
  * Assume that this list is always small enough that returning all groups at once is not an issue

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
     * Service returns list of `Invitation` for that group
     * Requirement: user must be `GOwner`

### Add user

* Client sends a service message with
  * `GID`
  * username
  * Requirement: user must be `GOwner`
* Service
  * Stores invitation with `AddUserToGroup` type
  * Sends invitation to user with `IID` to Feeds service
* ONE OF:
  * Accept
    * Client sends a service message with `IID` and `accept`
    * Service
      * Adds user to group
      * Deletes `Invitation`
      * Sends message to `GOwner` to Feeds
      * Returns 204
    * Requirement: user must be `IUser`
  * Deny
    * Client sends a service message with `IID` and `deny`
    * Service
      * Deletes `Invitation`
      * Sends message to `GOwner` to Feeds
      * Returns 204
      * Requirement: user must be `IUser`
  * Cancel
    * Client sends a service message with `IID` and `cancel`
    * Service
      * Deletes `Invitation`
      * Deletes `IID` from Feeds
      * Returns 204
    * Requirement: user must be `GOwner`

