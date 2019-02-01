# KBase Groups

Service for creating and managing groups of KBase users and associated Narratives.

Build status (master):
[![Build Status](https://travis-ci.org/kbase/groups.svg?branch=master)](https://travis-ci.org/kbase/groups)
[![codecov](https://codecov.io/gh/kbase/groups/branch/master/graph/badge.svg)](https://codecov.io/gh/kbase/groups)

## API Data structures

### User

Represents a member of a group.

```
{
    "name": <the user's user name>,
    "joined": <the date the user joined the group in epoch ms>,
    "lastvisit": <the last time the user visited the group in epoch ms>,
    "custom": {
        <custom field 1>: <custom value 1>,
        ...
        <custom field N>: <custom value N>
    }
}
```

`lastvisit` is only present for group administrators; it is otherwise `null`. It is also `null`
if the last visit date has never been set via the API.

See `Custom fields`, in particular `User fields`, below.

### Group

Represents a group of users and associated data.

```
{
    "id": <the group ID>,
    "private": <true if the group is private, false otherwise>,
    "privatemembers": <true if the members list is private, false otherwise>,
    "role": <'Owner', 'Admin', 'Member', or 'None', as appropriate>,
    "lastvisit": <the last time the user visted the group in epoch ms>,
    "name": <the group name>,
    "owner": <the User data or user name for the group owner>,
    "admins": <an array of User data of admins of the group>,
    "members": <an array of User data of members of the group>,
    "memcount": <the number of members in the group>
    "createdate": <the group creation date in epoch ms>,
    "moddate": <the last modification date of the group in epoch ms>
    "resources":
        {<resource type 1>: [<resource entry 1-1>, ..., <resource entry 1-N>],
         ...
         <resource type N>: [<resource entry N-1>, ..., <resource entry N-N>]
         },
    "rescount":
        {<resource type 1>: <the number of resources of this type in the group>,
         ...
         <resource type N>: <the number of resources of this type in the group>
         },
    "custom": {
        <custom field 1>: <custom value 1>,
        ...
        <custom field N>: <custom value N>
    }
}
```

In a full view of the group, the owner field contains a `User` structure. In a group list view,
the owner field contains only the user name of the owner.

`lastvisit` is `null` if the last visit date has never been set via the API for the current user.

`rescount` does not contain resource types for which the group has no resources (e.g. the
count is zero). `resources` *does* contain empty lists for resources that are supported by
the service but not included in the group for ease of iteration.

Note that `rescount` may include deleted resources until the group is fetched from the detail
view endpoint, at which point the deleted resources will be removed from the group.

See `Resources` and `Custom fields` below.

### Resources

Resources are items external to the groups service that may be associated with groups. All
resource entries have a `rid` field for the resource ID. The contents of this ID field depend
on the resource, but are always a string of at most 256 Unicode code points. The other fields in
the resource entry depend on the resource type.

The currently supported resource types are:

#### catalogmethod

Represents a KBase catalog service method.

```
{"rid": <the catalog method ID, e.g. Module.method>}
```

#### workspace

Represents information about a workspace.

```
{
    "rid": <the workspace ID>,
    "name": <the workspace name>,
    "narrname": <the name of the narrative contained in the workspace or null>
    "narrcreate": <the creation date, in epoch ms, of the narrative or null>
    "public": <true if the workspace is public, false otherwise>
    "perm": <the user's permission for the workspace>,
    "description": <the workspace description or null>
    "moddate": <the modification date of the workspace in epoch ms>
}
```

`narrcreate` is the save date, in epoch ms, of the first version of the narrative contained in
the workspace.

`perm` is one of `None`, `Read`, `Write`, `Admin`, or `Own`.

### Request

Represents a request to modify a group in some way.

```
{
    "id": <an arbitrary string that is the unique ID of the request>,
    "groupid": <the ID of the group that will be modified if the request is accepted>,
    "requester": <the username of the user that created the request>,
    "type": <the type of the request>,
    "resourcetype": <the type of the resource that is the target of the request>,
    "resource": <the ID of the resource that is the target of the request>,
    "status": <the status of the request>,
    "createdate": <the request creation date in epoch ms>,
    "expiredate": <the date the request expires in epoch ms>,
    "moddate": <the last modification date of the request in epoch ms>
}
```

The type of the request is either `Request` or `Invite`:

* `Request`s designate requests where a user that is not an administrator of a group is
  requesting that the group add a resource.
* `Invite`s designate requests where a group administrator is inviting a resource to join the
  group.

The `resourcetype` designates what kind of resource will be added to the group if the
request is accepted.
The resource types are the same as listed in `Resources` above, plus a built-in type, `user`,
that designates that accepting the request will add a user.

The `resource` is the ID of the resource to be added to the group - this is the same as the
`rid` field in `Resources`, or the user name for a user to be added.

The request status is one of `Open`, `Canceled`, `Expired`, `Accepted`, or `Denied`.

### Error

This data structure is returned for all service errors.

```
{
    "error": {
        "appcode": <the application error code, a 5 digit number>,
        "apperror": <the application error string, a descriptive string for the error>,
        "callid": <the ID of the service request>,
        "httpcode": <the http error code, e.g. 404>,
        "httpstatus": <the http status, e.g. Not Found>,
        "message": <an error message>,
        "time": <the time the error occurred in epoch ms>
    }
}
```

The call ID and service time can be used to find more details about the error in the service logs.

The application code and application error are two ways of representing the same error type. They
are only present for 4XX errors that are not general service errors. For example, navigating
to `/grops` instead of `/groups` will result in a 404 error without application error
information. Similarly, 405 and 415 errors will not include an application error code.


Current error types are:

```
10000	Authentication failed
10010	No authentication token
10020	Invalid token
20000	Unauthorized
30000	Missing input parameter
30001	Illegal input parameter
30010	Illegal user name
30020	Illegal group ID
30030	Illegal resource ID
40000	Group already exists
40010	Request already exists
40020	User already group member
40030	Resource already in group
50000	No such group
50010	No such request
50020	No such user
50030	No such custom field
50040	No such resource
50050	No such resource type
60000	Request closed
70000	Unsupported operation
```

## API

Endpoints that require authorization are noted below. To authorize a request, include an
`authorization` header with a KBase token as the value.

### Root

```
GET /

RETURNS:
{
    "servname": "Groups service",
    "servertime": <server time in epoch ms>,
    "gitcommithash": <git commit from build>,
    "version": <service version>
}

```

### List groups

```
AUTHORIZATION OPTIONAL
GET /group[?excludeupto=<exlude string>&order=<sort order>]

RETURNS:
A list of Groups. Only the id, name, owner, role, memcount, rescount, custom, lastvisit,
createdate, and moddate fields are included.
```

The owner field consists only of the user name for this endpoint. For most other endpoints,
the owner field is a full `User` data structure.

A maximum of 100 groups are returned.

Private groups are not included unless the user is a member of the group.

The query parameters are all optional:
* `order` - `asc` to sort the groups in order of their group IDs, `desc` to sort the groups
  in reverse order. If omitted the sort order is set to `asc`.
* `excludeupto` - a string that determines the starting point of the list,
  depending on the sort order. `asc` and `desc` sorts will include groups with
  group IDs, respectively, after and before the `excludeupto` string, non-inclusive.
  This can be used to page through the groups if needed.

If the user is anonymous or not a member of the group, only custom fields that are both public and
group listable (see custom fields below) are included. If the user is a member of the group,
all group listable fields are included.

`rescount` is empty for users that are not a member of the group.

### Get group names from IDs

```
AUTHORIZATION OPTIONAL
GET /names/<comma separated list of group IDs>

Returns:
[{
    "id": <group 1 id>,
    "name": <group 1 name>
 },
 ...
 {
    "id": <group N id>,
    "name": <group N name>
 }]
```

Whitespace only entries in the ID list are ignored. At most 1000 ids may be submitted per request.

If the user is anonymous or is not a member of a group and that group is private, the name of
that group will be `null`.

### Create a group

```
AUTHORIZATION REQUIRED
PUT /group/<group id>
{
    "name": <an arbitrary group name>,
    "private": <true for a private group, false (the default) for public, optional>,
    "privatemembers": <true (the default) for a private members list, false for public,
        optional>,
    "custom": {
        <custom field 1>: <custom value 1>,
        ...
        <custom field N>: <custom value N>
    }
}

RETURNS: A Group.
```

The group ID must start with a letter, consist only of lower case ASCII letters, numbers,
and hyphens, and be no longer than 100 characters.

The group name must be no longer than 256 Unicode code points.

If `private` is null or missing altogether, the group is set as public.
If `privatemembers` is null or missing altogether, the group member list is set as private.

See `Custom fields` below for information on custom fields.

Whitespace only strings are treated as `null`. `null` values are ignored for custom fields.

### Update a group

```
AUTHORIZATION REQUIRED
PUT /group/<group id>/update
{
    "name": <an arbitrary group name, optional>,
    "private": <true for a private group, false for public, optional>,
    "privatemembers": <true (the default) for a private members list, false for public,
        optional>,
    "custom": {
        <custom field 1>: <custom value 1>,
        ...
        <custom field N>: <custom value N>
    }
}
```

The user must be a group administrator.

The constraints on the parameters are the same as for the creation parameters.

If `name`, `private` or `privatemembers` are `null` or missing altogether, they are not
altered.

If custom fields are missing, they are not altered.
If they are `null`, they are removed. Otherwise they are set to the new value.

See `Custom fields` below for information on custom fields.

Whitespace only strings are treated as `null`.

### Get a group

```
AUTHORIZATION OPTIONAL
GET /group/<group id>

RETURNS: A Group.
```

If the user is not a member of the group or no authorization is provided and the group is
private, only the `groupid`, `private`, and `role` fields are included.

If no authorization is provided, the members list is populated or not based on the
`privatemembers` field, only public custom fields are included, and only public resources
associated with the group are returned.

If authorization is provided and the user is not a member of the group, the members list is
populated or not based on the `privatemembers` field, only public custom fields are included,
and only group-associated public resources and resources the user administrates are returned.

If authorization is provided and the user is a member of the group, the members list is populated,
all custom fields are included, and all group-associated resources are returned.

`rescount` is empty for users that are not a member of the group.

### Check if a group ID exists

```
GET /group/<group id>/exists

RETURNS:
{"exists": <true if the group exists, false otherwise>}
```

### Request membership to a group

```
AUTHORIZATION REQUIRED
POST /group/<group id>/requestmembership

RETURNS: A Request.
```

### Invite a user to a group

```
AUTHORIZATION REQUIRED
POST /group/<group id>/user/<user name>

RETURNS: A Request.
```

The user must be a group administrator.

### Remove a member from a group

```
AUTHORIZATION REQUIRED
DELETE /group/<group id>/user/<user name>
```

The user must be a group administrator or the user to be removed.

### Promote an existing member to an administrator

```
AUTHORIZATION REQUIRED
PUT /group/<group id>/user/<user name>/admin
```

The user must be the group owner.

### Demote an administrator to a member

```
AUTHORIZATION REQUIRED
DELETE /group/<group id>/user/<user name>/admin
```

The user must be the group owner.

### Update a user's fields

```
AUTHORIZATION REQUIRED
PUT /group/<group id>/user/<user name>/update
{
    "custom": {
        <custom field 1>: <custom value 1>,
        ...
        <custom field N>: <custom value N>
    }
}
```

The user must be a group administrator or the user whose fields are to be updated.

If custom fields are missing, they are not altered.
If fields are `null`, they are removed. Otherwise they are set to the new value.

See `Custom fields` below for information on custom fields.

Whitespace only strings are treated as `null`.

### Get a list of group IDs and names for which the user is a member

```
AUTHORIZATION REQUIRED
GET /member/

Returns:
[{
    "id": <group 1 id>,
    "name": <group 1 name>
 },
 ...
 {
    "id": <group N id>,
    "name": <group N name>
 }]
```

*All* the groups for which the user is a member are returned in one response, which is why only
the minimal amount of information per group is included.

### Add a resource to a group

```
AUTHORIZATION REQUIRED
POST /group/<group id>/resource/<resource type>/<resource id>

RETURNS: Either {"complete": true} or a Request with the additional field "complete"
with a value of false.
```

The resource is added immediately if the user is an administrator of both the group and
the resource. A request object is returned if the user is an administrator of at least one; if
not an error is returned.

### Remove a resource from a group

```
AUTHORIZATION REQUIRED
DELETE /group/<group id>/resource/<resource type>/<resource id>
```

The user must be an administrator of either the group or the resource.

### Get read permission for a resource

```
AUTHORIZATION REQUIRED
POST /group/<group id>/resource/<resource type>/<resource id>/getperm
```

The user must be a member of the group. Read permissions are only granted if the user
has no explicit permission to the resource and the resource is not publicly readable.

### Get a request

```
AUTHORIZATION REQUIRED
GET /request/id/<request id>

RETURNS: A Request with an additional field "actions" which is a list of actions allowed
to the user.
```

Possible actions are `Cancel`, `Accept`, and `Deny`.

### Get group information associated with a request

```
AUTHORIZATION REQUIRED
GET /request/id/<request id>/group

A Group. Only the id, name, owner, role, memcount, rescount, custom, lastvisit,
createdate, and moddate fields are included.
```

This endpoint allows a user who has been invited to a group, or who administrates a resource
that has been invited to a group, to view basic information about the group - even for private
groups. The request must be `Open`, the request type must be `Invite`, and the user must be
the target of the request or an administrator of a resource that is the target of the request.

If the group is not private this endpoint is not useful.

This endpoint returns a data structure identical to that of the `/group` endpoint, as if
the user is not a member of the group, for consistency's sake. As such, the `rescount` field
is present but always empty.

The owner field consists only of the user name for this endpoint. For most other endpoints,
the owner field is a full `User` data structure.

Only public custom fields are included.

### Get whether groups have open requests

```
AUTHORIZATION REQUIRED
GET /request/groups/<comma separated group ids>/new[?laterthan=<epoch ms>]

RETURNS:
{
    "id1": {"new": <"None", "Old", or "New">},
    ....
    "idN": {"new": <"None", "Old", or "New">}
}
```

The user must be an administrator of all the groups in the comma separated list.

Requests targeted towards groups are of type `Request`, not `Invite`.

Whitespace only entries in the ID list are ignored. At most 100 ids may be submitted per request.

The `laterthan` parameter determines which requests are treated as `Old` or `New`. If all
open requests for a group are older than or equal to `laterthan` `Old` is returned for that
group. If `laterthan` is not supplied a value of approximately 14Gy BCE is assumed and thus
in most cases groups that have `Open` requests will be marked as `New`.

Each group is mapped to a further mapping to allow for backwards-compatible expansion of the
API in the future.

### Get permission to read a resource associated with a request

```
AUTHORIZATION REQUIRED
POST /request/id/<request id>/getperm
```

The request type must be `Request`, the resource type cannot be `user`,
and the user must be a group administrator. Read permissions are only granted if the user
has no explicit permission to the resource and the resource is not publicly readable.

### Listing requests

There are three endpoints for listing requests detailed below - one for listing requests you
created, one for listing requests targeted at you, and one for listing requests targeted at
a specific group.

All endpoints return a maximum of 100 requests at once.

These endpoints have common parameter sets and behavior, other than the actual requests they
return. They all have the following optional query parameters:

* `closed` - include closed requests (e.g. those with a state other than `OPEN`) in the list.
  If omitted, only open requests are included.
* `order` - `asc` to sort the requests in order of the least recently modified,
  `desc` to sort by the most recently modified.
  If omitted, and if `closed` is also omitted, the sort order is set to `asc`.
  If closed requests are included, it is set to `desc`.
* `excludeupto` - a date in epoch milliseconds that determines the starting point of the list,
  depending on the sort order. `asc` and `desc` sorts will include requests with
  modification dates, respectively, after and before the `excludeupto` date, non-inclusive.
  This can be used to page through the requests if needed.

Examples:

* `?` - only include open requests and sort oldest first by modification date.
* `?closed` - include all requests and sort newest first by modification date.
* `?closed&order=asc&excludeupto=1500000000000` - include all requests, sort by
  least recently modified, and exclude any requests that were modified on or before
  Friday, July 14, 2017, at 2:40:00 AM GMT.
 

#### List created requests

```
AUTHORIZATION REQUIRED
GET /request/created[?parameters]

RETURNS: A list of Requests.
```

Returns requests that were created by the user.

#### List targeted requests

```
AUTHORIZATION REQUIRED
GET /request/targeted[?parameters]

RETURNS: A list of Requests.
```

Returns requests where the user is a target (including an administrator of the resource at
which the request is targeted) of the request.

#### Get the list of requests for a group

```
AUTHORIZATION REQUIRED
GET /group/<group id>/requests[?parameters]

RETURNS: A list of Requests.
```

The user must be a group administrator. The requests only include those where a group administrator
must take action on the request.

### Cancel a request

```
AUTHORIZATION REQUIRED
PUT /request/id/<request id>/cancel

RETURNS: A Request.
```

The user must be the creator of the request.

### Accept a request

```
AUTHORIZATION REQUIRED
PUT /request/id/<request id>/accept

RETURNS: A Request.
```

The user must be the target of the request (including an administrator of the resource at
which the request is targeted) or an administrator of the group at which the request is targeted.

### Deny a request

```
AUTHORIZATION REQUIRED
PUT /request/id/<request id>/deny
{
    "reason": <the reason the request was denied, optional>
}

RETURNS: A Request.
```

The user must be the target of the request (including an administrator of the resource at
which the request is targeted) or an administrator of the group at which the request is targeted.

The `reason` can be no more than 500 Unicode code points.
Currently, the reason is not exposed by the API, but that may change in the future.

## Custom fields

Custom fields may be associated with a group on group creation or update. The allowed fields
must be configured by server administrators in the `deploy.cfg` configuration file (see below).

Attempting to create or update a group with a field that is not configured will result in an
error. The maximum field size (including numbered fields, see below) is 50 Unicode code points.

At minimum, a field validator must be associated with each configured field. Any other
configuration keys for a field are ignored if a validator is not configured. The validator
checks that the value of the field meets some criteria that depends on the validator.
If the field does not meet those criteria the group creation or update fails.

To configure a validator, the classname of the validator factory class must be assigned to the
field in the `deploy.cfg` file:
```
field-<field name>-validator=us.kbase.groups.fieldvalidators.SimpleFieldValidatorFactory
```

A `<field name>` consists of lower case ASCII letters and numbers.

A validator factory must implement
`us.kbase.groups.core.fieldvalidation.FieldValidatorFactory`.

Regardless of the validator, no field value may be longer than 5000 Unicode code points.

### Numbered fields

Optionally, a field may be specified as numbered, in which case any field with the pattern
`<field name>-<integer>` is allowed. For example:
```
field-myfield-validator=us.kbase.groups.fieldvalidators.SimpleFieldValidatorFactory
field-myfield-is-numbered=true
```

In this case, any fields with an integer suffix separated by `-` are allowed (e.g `myfield`,
`myfield-22`, `myfield-1`, and `myfield-42`) and are validated by the same
validator. This is useful for data where there may be a list of values for the field.

Any value other than `true` for `field-<field name>-is-numbered` is treated as false.

### Public fields

By default, custom fields are only visible to members of the group. Fields can be made public
like so:

```
field-myfield-is-public=true
```

In this case, all users will be able to see the field. Any value other than `true` for
`field-<field name>-is-public` is treated as false.

Removing this setting will result in the field being set private for all extant and future fields.

### Group list fields

In order to reduce transport costs, by default, custom fields are only visible when a
full group is accessed, not when multiple groups are listed.
Fields can be set to be included in the list of groups like so:

```
field-myfield-show-in-list=true
```

In this case, the field will be shown in the group list, subject to privacy constraints.
Any value other than `true` for `field-<field name>-show-in-list` is treated as false.

Potentially large fields should not be included in the group list, and it is advisable to
keep the number of fields in the group list small.

Removing this setting will result in the field no longer being visible in groups lists
for all extant and future fields.

### User fields

**WARNING**: See Implementation notes below.

Custom fields can also be applied to group members. To create a user field, substitute the
`field-` prefix with `field-user-`:

```
field-user-myfield-validator=us.kbase.groups.fieldvalidators.SimpleFieldValidatorFactory
```

All the previous field modifiers apply (using the `field-user-` prefix) except for the group
list setting, since user custom fields are not visible in the groups list.

User fields have an additional setting that allows standard members, rather than only the owner
and administrators, to set the field for their own record:

```
field-user-myfield-is-user-settable=true
```

This setting is ignored for non-user fields.

Any value other than `true` for `field-user-<field name>-is-user-settable` is treated
as false.

### Validator parameters

Some validators have optional or required parameters. Validator parameters can be specified
like so:
```
field-myfield-validator=us.kbase.groups.fieldvalidators.SimpleFieldValidatorFactory
field-myfield-param-<parameter name>=<parameter value>
```

A mapping of all parameter names and values will be provided to the validator on creation.

### Altering field configurations

**WARNING**: Field validation only occurs when creating or updating a group - fields are not
validated or checked against the field configuration when retrieving groups.
If a field configuration is altered or removed from the configuration file, *the field
will still be visible and unchanged for any groups where it has been set*.
The field must meet the requirements of the new configuration (or cannot be created / updated
at all if the configuration is removed) on group creation or update, but the field can
always be removed.

### Available validators

#### us.kbase.groups.fieldvalidators.SimpleFieldValidatorFactory

Checks that the value contains no control characters, and, optionally, is below a maximum length.

Parameters:
```
field-<field name>-param-max-length=<maximum value length in Unicode code points>
field-<field name>-param-allow-line-feeds-and-tabs=<true for true, anything else for false>
```

All parameters are optional.

`allow-line-feeds-and-tabs` specifies that the `\n`, `\r`, and `\t` characters are allowed
in the value.

#### us.kbase.groups.fieldvalidators.EnumFieldValidatorFactory

Checks that the value is one of a set of specified values. This can be used for boolean values
(`true`, `false`), controlled vocabularies, etc. The values may not be longer than 50 Unicode
code points.

Parameters:
```
field-<field name>-param-allowed-values=<comma separated list of allowed values>
```

`allowed-values` is required. Examples:
```
field-boolean-param-allowed-values=true, false
field-feast-param-allowed-values=lambs, sloths, carp, orangutans, breakfast cereals, fruit bats
```

#### us.kbase.groups.fieldvalidators.GravatarFieldValidatorFactory

Checks that the value is a valid [Gravatar hash](https://en.gravatar.com/site/implement/hash/),
meaning that the first 32 characters of the string constitute a valid MD5.

Parameters (all optional):
```
field-<field name>-param-strict-length=<'true' or any other value for false>
field-<field name>-param-image-exists=<'true' or any other value for false>
```

`strict-length` will cause the validator to throw an error if the field is not exactly 32
characters, and is false by default. Gravatar allows for extra characters at the end of the hash.

`image-exists` will cause the validator to throw an error if there is no [image associated with
the hash (see Default Image)](https://en.gravatar.com/site/implement/images/), and is false
by default.

Examples:
```
field-gravatarhash-param-strict-length=true
field-gravatarhash-param-image-exists=nottrue
````

## Implementation notes

Currently the only backend supported is MongoDB, which supports a maximum 16MB document size.
As per MongoDB recommendation and to support atomic updates, queries and filters, the group
users and resources (but not requests) are included in the group document. This means there
is a maximum number of users and resources per group.

Resources take ~20-550 bytes, depending on the resource ID size, and so the MongoDB backend
can support ~30k-800k resources per group assuming no group users.

Users without custom fields take ~30-150 bytes, depending on the user name, and so the MongoDB
backend can support ~100k-500k users per group assuming no group resources.

Resources' and users' space consumption can be combined more or less linearly - more users means
less space for resources and vice versa.

User custom fields can drastically change the byte requirement for users - for example, adding
a 5000 (the maximum) Unicode code point text field means a user can consume 20kB (assuming all
the characters are outside the Basic Multilingual Plane), which means that as few as 800 users
could cause the group document to exceed the MongoDB limit and cause write errors.

As such, discretion should be used when defining custom fields for users, and it is recommended
that the fields be kept few and small in size.

## Requirements

Java 8 (OpenJDK OK)  
Apache Ant (http://ant.apache.org/)  
MongoDB 2.6+ (https://www.mongodb.com/)  
Jetty 9.3+ (http://www.eclipse.org/jetty/download.html)
    (see jetty-config.md for version used for testing)  
This repo (git clone https://github.com/kbase/groups)  
The jars repo (git clone https://github.com/kbase/jars)  
The two repos above need to be in the same parent folder.

## To start server

start mongodb  
if using mongo auth, create a mongo user  
cd into the groups repo  
`ant build`  
copy `deploy.cfg.example` to `deploy.cfg` and fill in appropriately  
`export KB_DEPLOYMENT_CONFIG=<path to deploy.cfg>`  
`cd jettybase`  
`./jettybase$ java -jar -Djetty.http.port=<port> <path to jetty install>/start.jar`  

## Developer notes

### Adding and releasing code

* Adding code
  * All code additions and updates must be made as pull requests directed at the develop branch.
    * All tests must pass and all new code must be covered by tests.
    * All new code must be documented appropriately
      * Javadoc
      * General documentation if appropriate
      * Release notes
* Releases
  * The master branch is the stable branch. Releases are made from the develop branch to the master
    branch.
  * Update the version as per the semantic version rules in `src/us/kbase/groups/api/Root.java`.
  * Tag the version in git and github.

### Running tests

* Copy `test.cfg.example` to `test.cfg` and fill in the values appropriately.
  * If it works as is start buying lottery tickets immediately.
* `ant test`

### UI

* Some fields are arbitrary text entered by a user. These fields should be HTML-escaped prior to
  display. Currently the fields include:
  * Group name
  * Group description
  * User submitted information in error messages.
  
Use common sense when displaying a field from the server regarding whether the field should be
html escaped or not.
  
### Exception mapping

In `us.kbase.groups.core.exceptions`:  
`GroupsException` and subclasses other than the below - 400  
`AuthenticationException` and subclasses - 401  
`UnauthorizedException` and subclasses - 403  
`NoDataException` and subclasses - 404  

`JsonMappingException` (from [Jackson](https://github.com/FasterXML/jackson)) - 400  

Anything else is mapped to 500.

## Test UI

`nodejs` must be installed:

```
npm --version
6.4.1
nodejs --version
v8.12.0
```

In `testui`:

```
npm install
npm run build
```

Then open `index.html` in your favorite browser. You may need to run the groups service and
an html server for the UI behind a reverse proxy to avoid CORS issues.

## TODO

see /design/*.md

* Security
  * (WS) Temporary permissions in workspace for request-based view of ws vs. permanent grant
* Reliability
  * Needs logging for most actions. Currently nothing is logged.
  * Feeds notification is currently Fire & Forget. Is this what we want? Options in order of
    time & maintenance cost:
    * F&F
    * Synchronous retry X times then fail
    * Add to in memory queue & continue retrying until success
    * Persistent queue
  * Feeds notification implementation is unclear - currently going straight to feeds, may go
    to Kafka instead.
  * Retries in the workspace handler.
  * Retries in the catalog handler.
* Performance
  * Currently group workspaces are pulled 1 at a time w/ 2 WSS calls per group workspace
    * (WS) Update get_permissions_mass to allow returning error codes for inaccessible /
      deleted / missing workspace instead of erroring out
    * (WS) Add bulk call for get_workspace_info with same error handling option
    * (WS) Add error codes to get_object_info3 and get_objects2
    * Cache results
  * Cache results of catalog service queries
* Usability
  * Endpoint for getting all requests targeted at groups I administrate
    * Currently I have to go group by group
  * Text search - need product team feedback
    * In an ideal world this would be added to search but...
  * Hide groups? Since we can't delete groups we'll wind up with a bunch of crap in the groups
    list. Private groups aren't exactly the same thing, I think.
  * When display user who denied request & reason? Need product team feedback
    * Currently request denials are not notified
  * System administration support - what do we need?
  * Filters and sorts for groups
    * Every filter & sort combination (usually) requires a new MongoDB index & more
      time & maintenance cost, so choose carefully
    * Remember - skip is evil
    * Find groups where I'm (owner / admin / member)
    * Find groups where user X is an owner or admin
    * Find groups where users X is a member and I'm a member
    * Find groups that contain workspaces I administrate
    * Find groups that contain workspace X and where I'm a group member
    * Find groups that contain catalog methods I own
    * Find groups that contain catalog method X
* New features
  * Relations between groups
    * This needs a lot of thought / design if the relations are hierarchical /
      directional.
      * Cycle detection, etc.
  * Change owner
* Testing
  * travis
    * try mongo 4 - maybe wait for 4.2
  * finish last few tests
  * integration tests
* Other
  * HTTP2 support
  * Reduce code duplication between services - see TODO.md
  * May need more field validators depending on UI needs
  * Although WS Admin read privs are no longer needed for groups, it'd still be good to finish.
