# KBase Groups Service release notes

## 0.1.6

### Admin notes:

* The `group` collection `own_1` and `admin_1` MongoDB indexes have been replaced by
  `own_1_id_1` and `admin_1_id_1` indexes and can be deleted.
* In total, 14 new MongoDB indexes have been added to support the new features in this version.
  As such, the first server startup on existing data will take extra time as the indexes are
  built.

### Release notes

* Added the `ids`, `role`, `resourcetype`, and `resource` parameters to the `/groups`
  endpoint.
* Added the `/request/groups` endpoint.
* Added the `/request/id/<id>/resource` endpoint.
* Added `resourcetype` and `resource` parameters to the four request listing endpoints.
* Resource administrators can now see their resources in private groups
  for which they are not a member in the `/group/<group id> endpoint`.

## 0.1.5

* Group administrators may now promote and demote other administrators.

## 0.1.4

* The group `rescount` field is now public.

## 0.1.3

* BACKWARDS INCOMPATIBILITY: No longer exposes the group owner's and administrators' join date
  to non-members.
* Resource information now includes an `added` field that denotes when the resource was added to
  the group.

## 0.1.2

* BACKWARDS INCOMPATIBILITY: The `/request/groups/<ids>/new` endpoint no longer accepts a
  `laterthan` date and bases the old vs. new request determination on the last visited date
  for the group.

## 0.1.1

* BACKWARDS INCOMPATIBILITY: The user role enum values are now capitalized like all the other
  enums in the API, e.g. None, Member, Admin, and Owner.

* Added a `/request/id/<id>/group` endpoint that returns minimal group information for
  Invite-type requests.
* Added a `/request/groups/<csv ids>/new` endpoint that returns whether a set of groups have
  open requests on a per group basis.
* Added a `/group/<id>/visit` endpoint that sets the last visited date for the current user for
  the group, and added the last visited date to the API.

## 0.1.0

* Initial release