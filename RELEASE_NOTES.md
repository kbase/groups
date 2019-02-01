# KBase Groups Service release notes

## 0.1.1

* BACKWARDS INCOMPATIBILIY: The user role enum values are now capitalized like all the other
  enums in the API, e.g. None, Member, Admin, and Owner.

* Added a /request/id/<id>/group endpoint that returns minimal group information for Invite-type
  requests.
* Added a /request/groups/<csv ids>/new endpoint that returns whether a set of groups have
  open requests on a per group basis.
* Added a /group/<id>/visit endpoint that sets the last visited date for the current user for the
  group, and added the last visited date to the API.

## 0.1.0

* Initial release