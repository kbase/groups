# Text Search

Since MongoDB only supports one text index per collection and does not return which fields
were hit during the search, it seems like it's not an option given we wish to support private
fields. Private fields mean we need to exclude search hits where the user is not a member of
the group in question and the search only hits private fields.

Furthermore, the one text index per collection limit makes it painful to add new text indexed
fields, as the index will have to be rebuilt each time.

Options:

## Only support public fields with MongoDB text index

Pros:

1. Supports stemming and stop words
2. Supports ordering by relevance

Cons:

1. Private fields are not supported
2. Adding new text search fields requires rebuilding the mongo index before the server can start
3. Complicates the schema for custom fields (details not shown)

## Standard MongoDB indexing with simple tokenizing

Add `private` and `public` fields to the MongoDB record that contain a merge of
tokenized versions of the searchable fields.

Pros:
1. Can support separate indexes for private and public fields

Cons:
1. No stemming and stop words
2. No ordering by relevance
3. Likely thousands of index entries per group - slow create & update
4. Makes group updates much more tricky (details not shown)
5. Adding or removing a text search field may require reindexing all groups (see below)

## Standard MongoDB indexing with stemming and stop words

Add `private` and `public` fields to the MongoDB record that contain a merge of
tokenized versions, including stop words and stemming, of the searchable fields.

In practice, this probably means making Lucene a dependency and using it for stemming & stop
words.

Pros:
1. Can support separate indexes for private and public fields
2. Supports stemming and stop words

Cons:
1. No ordering by relevance
2. More work than simple tokenizing
3. Likely thousands of index entries per group - slow create & update
4. Makes group updates much more tricky (details not shown)
5. Adding or removing a text search field may require reindexing all groups (see below)

## Delegate to ElasticSearch for text search

The Groups service would have its own ES namespace and completely wrap ES - searches would
still be submitted to the groups service.

Pros:
1. Can support separate indexes for private and public fields
2. Supports stemming and stop words
3. Supports ordering by relevance

Cons:
1. More work than simple tokenizing (not clear whether this is more work than using Lucene
   directly. Probably is).
2. Need an admin command to reindex a specific group in case the server goes down between
   a MongoDB update and ES update
3. Adding or removing a text search field may require reindexing all groups (see below)

## ~~Submit groups to KBase Search service~~

# General issues

## Adding or removing a text search field may require reindexing all groups

Removing a text search field requires reindexing all groups in order to remove index entries
for that field.

Normally adding a new field would not require reindexing the groups data, but there's one
case where it could be required - when a field is removed and then later readded as a text
search field. This means there may be entries in the database for the field which need to be
included in the index but are not.

All the options below could be run in the background if immediate consistency is not a concern.
Otherwise, the server(s) would need to be taken down, the field configuration and fields updated,
and the server(s) restarted.

Options:

### Add a CLI interface so an admin can reindex

Pros:

1. Simplest solution

Cons:

1. Requires direct connection to MongoDB
2. Requires admin to manually trigger reindex

### Add an API interface so an admin can reindex

Pros:

1. Admin can start a reindex via curl, etc.

Cons:

1. Requires job monitoring & listing code
2. Requires admin to manually trigger reindex

### Store index specifications in database

When a change is detected on startup, automatically trigger reindex in a background thread.

Pros:

1. Automatic - no admin intervention required, no CLI / API / Job code needed.

Cons:

1. Probably most complex implementation
2. Needs some sort of global lock (zookeeperish) to prevent many instances of the server
   running the reindex at once.

 