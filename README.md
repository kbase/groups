# KBase Groups

Service for creating and managing groups of KBase users and associated Narratives.

Build status (master):
[![Build Status](https://travis-ci.org/kbase/groups.svg?branch=master)](https://travis-ci.org/kbase/groups)
[![codecov](https://codecov.io/gh/kbase/groups/branch/master/graph/badge.svg)](https://codecov.io/gh/kbase/groups)

## API

Endpoints that require authorization are noted below. To authorize a request, include an
`authorization` header with a KBase token as the value.

### Data structures

#### WorkspaceInformation

Represents information about a workspace.

```
{
    "id": <the workspace ID>,
    "name": <the workspace name>,
    "narrname": <the name of the narrative contained in the workspace or null>
    "public": <true if the workspace is public, false otherwise>
    "admin": <true if the user is an admin of the workspace, false otherwise>
}
```

#### Group

Represents a group of users and associated data.

```
{
    "id": <the group ID>,
    "name": <the group name>,
    "owner": <the username of the group owner>,
    "type": <the type of the group, e.g. Team, Project, etc.>,
    "admins": <an array of usernames of admins of the group>,
    "members": <an array of usernames of members of the group>,
    "description": <a description of the group>,
    "createdate": <the group creation date in epoch ms>,
    "moddate": <the last modification date of the group in epoch ms>
    "workspaces": <a list of WorkspaceInformation>
}
```

#### Request

Represents a request to modify a group in some way.

```
{
    "id": <an arbitrary string that is the unique ID of the request>,
    "groupid": <the ID of the group that will be modified if the request is accepted>,
    "requester": <the username of the user that created the request>,
    "type": <the type of the request>,
    "status": <the status of the request>,
    "targetuser": <the user at which the request is targeted, if any>,
    "createdate": <the request creation date in epoch ms>,
    "expiredate": <the date the request expires in epoch ms>,
    "moddate": <the last modification date of the request in epoch ms>
}
```

The type of the request dictates what fields are populated and what changes will occur if the
request is accepted. The types are:

`Request group membership` - If accepted by a group administrator, the user that created
the request will be added to the group membership. The target user is absent for this request
type.

`Invite to group` - If accepted by the target user, the target user will be added to the
group membership.

The request status is one of `Open`, `Canceled`, `Expired`, `Accepted`, or `Denied`.

#### Error

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
40000	Group already exists
40010	Request already exists
40020	User already group member
40030	Workspace already in group
50000	No such group
50010	No such request
50020	No such user
50030	No such workspace
60000	Unsupported operation
```

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
GET /group

RETURNS:
A list of Groups. Only the id, name, owner, and type fields are included.
```

### Create a group

```
AUTHORIZATION REQUIRED
PUT /group/<group id>
{
    "name": <an arbitrary group name>
    "type": <the type of the group, optional>
    "description": <the description of the group, optional>
}

RETURNS: A Group.
```

The group ID must start with a letter, consist only of lower case ASCII letters, numbers,
and hyphens, and be no longer than 100 characters.

The group name must be no longer than 256 Unicode code points.

The valid type values are one of `Organization`, `Project`, or `Team`. The default type
is `Organization`.

The group description must be no longer than 5000 Unicode code points.

### Get a group

```
AUTHORIZATION OPTIONAL
GET /group/<group id>

RETURNS: A Group.
```

If no authorization is provided, the members list is empty and only public workspaces associated
with the group are returned.

If authorization is provided and the user is not a member of the group, the members list is
empty and only group-associated public workspaces and workspaces the user administrates are
returned.

If authorization is provided and the user is a member of the group, the members list is populated
and all group-associated workspaces are returned.

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

### Add a workspace to a group

```
AUTHORIZATION REQUIRED
PUT /group/<group id>/workspace/<workspace id>
RETURNS: Either {"complete": true} or a Request with the additional field "complete"
with a value of false.
```

The workspace is added immediately if the user is an administrator of both the group or
the workspace. A request object is returned if the user is an administrator of at least one; if
not an error is returned.

### Remove a workspace from a group

```
AUTHORIZATION REQUIRED
DELETE /group/<group id>/workspace/<workspace id>
```

The user must be an administrator of either the group or the workspace.

### Get the list of requests for a group

```
AUTHORIZATION REQUIRED
GET /group/<group id>/requests

RETURNS: A list of Requests.
```

The user must be a group administrator. The requests only include those where a group administrator
must take action on the request.

### Get a request

```
AUTHORIZATION REQUIRED
GET /request/id/<request id>

RETURNS: A Request with an additional field "actions" which is a list of actions allowed
to the user.
```

Possible actions are `Cancel`, `Accept`, and `Deny`.

### List created requests

```
AUTHORIZATION REQUIRED
GET /request/created

RETURNS: A list of Requests.
```

Returns requests that were created by the user.

### List targeted requests

```
AUTHORIZATION REQUIRED
GET /request/targeted

RETURNS: A list of Requests.
```

Returns requests where the user is the target of the request.

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

The user must be the target of the request or an administrator of the group at which the
request is targeted.

### Deny a request

```
AUTHORIZATION REQUIRED
PUT /request/id/<request id>/deny
{
    "reason": <the reason the request was denied, optional>
}

RETURNS: A Request.
```

The user must be the target of the request or an administrator of the group at which the
request is targeted.

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

* Add / remove workspaces from group
  * return minimal metadata (probably have a workspaces endpoint rather than returning with
    group) for each workspace
* expire requests
* For all request listings, sort by creation date (up or down), set a max, and allow filtering by
  creation date for non-evil paging.
* Allow getting closed requests
* Filter groups by user and workspaces.
* Mutate groups
* Admin lists
* Search - need product team feedback
* When display user who denied request & reason? Need product team feed back
* Feeds implementation
* Gravatar support
* System admin support (what do they need to be able to do other than see everything?)
* travis
  * try mongo 4 - maybe wait for a couple bugfix versions
* Maybe filter groups by mod date & set limit to allow for cheap paging. Not really expecting a
  huge number of groups though.
* Add lots of tests.
  * Integration and otherwise
* HTTP2 support
