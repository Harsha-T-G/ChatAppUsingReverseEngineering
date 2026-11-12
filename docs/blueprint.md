# Mattermost chat: blueprint recorded by REA

- Evidence ID: `ev_79d802de252c870e24553cadc7171382736194a980a0c4b1609591381f987d82` (confidence: observed)
- Tool: REA REA Chrome DevTools Protocol observation provider, command `inspect-web-page` with `--include-json-body-shapes --include-websocket-shapes`
- Window: 2026-11-12T15:16:55.180Z to 2026-11-12T15:24:55.776Z (480 s)
- Recorded: 248 requests, 57 distinct API endpoints, 2 WebSocket connections, 0 dropped events
- Only field names and types were recorded, never values (so no passwords or message text are in this file).

## The 12 calls needed to rebuild the chat

| Call | Times seen | Status | Request fields | Response fields |
|---|---|---|---|---|
| `POST /api/v4/users` | 1 | 201 | email, password, username | object: auth_service, create_at, delete_at, disable_welcome_email, email, first_name, id, last_name, last_password_update, locale, nickname, notify_props … +6 more |
| `POST /api/v4/users/login` | 1 | 200 | device_id, id, password, token | object: auth_service, create_at, delete_at, disable_welcome_email, email, first_name, id, last_name, last_password_update, locale, nickname, notify_props … +6 more |
| `GET /api/v4/users/me` | 2 | 200 | (none) | object: auth_service, create_at, delete_at, disable_welcome_email, email, first_name, id, last_name, last_password_update, locale, nickname, notify_props … +6 more |
| `POST /api/v4/teams` | 1 | 201 | allow_open_invite, allowed_domains, company_name, create_at, delete_at, description, display_name, email, group_constrained, id, invite_id, name, scheme_id, type, update_at | object: allow_open_invite, allowed_domains, cloud_limits_archived, company_name, create_at, delete_at, description, display_name, email, group_constrained, id, invite_id … +8 more |
| `GET /api/v4/users/me/teams` | 2 | 200 | (none) | array:  |
| `POST /api/v4/channels` | 2 | 201 | create_at, creator_id, delete_at, display_name, group_constrained, header, id, last_post_at, last_root_post_at, name, purpose, scheme_id, team_id, type, update_at | object: autotranslation, banner_info, create_at, creator_id, default_category_name, delete_at, disable_join_leave_messages, discoverable, display_name, extra_update_at, group_constrained, header … +18 more |
| `GET /api/v4/channels/{id}` | 2 | 200 | (none) | object: autotranslation, banner_info, create_at, creator_id, default_category_name, delete_at, disable_join_leave_messages, discoverable, display_name, extra_update_at, group_constrained, header … +18 more |
| `GET /api/v4/users/me/teams/{id}/channels` | 1 | 200 | (none) | array: [], [].autotranslation, [].banner_info, [].create_at, [].creator_id, [].default_category_name, [].delete_at, [].disable_join_leave_messages, [].discoverable, [].display_name, [].extra_update_at, [].group_constrained … +19 more |
| `GET /api/v4/channels/{id}/posts` | 2 | 200 | (none) | object: first_inaccessible_post_time, next_post_id, order, posts, prev_post_id |
| `POST /api/v4/posts` | 6 | 201 | channel_id, create_at, file_ids, message, metadata, pending_post_id, props, reply_count, root_id, update_at, user_id | object: channel_id, create_at, delete_at, edit_at, file_ids, hashtags, id, is_pinned, last_reply_at, message, metadata, original_id … +9 more |
| `DELETE /api/v4/posts/{id}` | 1 | 200 | (none) | object: status |
| `PUT /api/v4/posts/{id}/patch` | 1 | 200 | channelId, channel_id, createAt, create_at, delete_at, edit_at, fileInfos, file_ids, hashtags, id, is_pinned, last_reply_at, message, metadata, original_id, participants, pending_post_id, props, remote_id, reply_count, rootId, root_id, type, updateAt, update_at, uploadsInProgress, user_id | object: channel_id, create_at, delete_at, edit_at, file_ids, hashtags, id, is_pinned, last_reply_at, message, metadata, original_id … +8 more |

## Every endpoint recorded, with fields

### 1. Users and login

**`POST /api/v4/roles/names`** — seen 4×, status 200
- request (array): `[]`:string
- response (array): `[]`:object, `[].built_in`:boolean, `[].create_at`:number, `[].delete_at`:number, `[].description`:string, `[].display_name`:string, `[].id`:string, `[].name`:string, `[].permissions`:array, `[].scheme_id`:null, `[].scheme_managed`:boolean, `[].update_at`:number

**`GET /api/v4/users`** — seen 2×, status 200, query: page, per_page, roles
- response (array): `[]`:object, `[].auth_service`:string, `[].create_at`:number, `[].delete_at`:number, `[].disable_welcome_email`:boolean, `[].email`:string, `[].first_name`:string, `[].id`:string, `[].last_name`:string, `[].locale`:string, `[].nickname`:string, `[].notify_props`:object, `[].position`:string, `[].remote_id`:string, `[].roles`:string, `[].timezone`:object, `[].update_at`:number, `[].username`:string, `[].bot_description`:string, `[].is_bot`:boolean, `[].last_picture_update`:number

**`POST /api/v4/users`** — seen 1×, status 201
- request (object): `email`:string, `password`:string, `username`:string
- response (object): `auth_service`:string, `create_at`:number, `delete_at`:number, `disable_welcome_email`:boolean, `email`:string, `first_name`:string, `id`:string, `last_name`:string, `last_password_update`:number, `locale`:string, `nickname`:string, `notify_props`:object, `position`:string, `remote_id`:string, `roles`:string, `timezone`:object, `update_at`:number, `username`:string

**`POST /api/v4/users/login`** — seen 1×, status 200
- request (object): `device_id`:string, `id`:string, `password`:string, `token`:string
- response (object): `auth_service`:string, `create_at`:number, `delete_at`:number, `disable_welcome_email`:boolean, `email`:string, `first_name`:string, `id`:string, `last_name`:string, `last_password_update`:number, `locale`:string, `nickname`:string, `notify_props`:object, `position`:string, `remote_id`:string, `roles`:string, `timezone`:object, `update_at`:number, `username`:string

**`GET /api/v4/users/me`** — seen 2×, status 200
- response (object): `auth_service`:string, `create_at`:number, `delete_at`:number, `disable_welcome_email`:boolean, `email`:string, `first_name`:string, `id`:string, `last_name`:string, `last_password_update`:number, `locale`:string, `nickname`:string, `notify_props`:object, `position`:string, `remote_id`:string, `roles`:string, `timezone`:object, `update_at`:number, `username`:string

**`PUT /api/v4/users/me/patch`** — seen 1×, status 200
- request (object): `auth_service`:string, `create_at`:number, `delete_at`:number, `disable_welcome_email`:boolean, `email`:string, `first_name`:string, `id`:string, `last_name`:string, `last_password_update`:number, `locale`:string, `nickname`:string, `notify_props`:object, `position`:string, `remote_id`:string, `roles`:string, `timezone`:object, `update_at`:number, `username`:string
- response (object): `auth_service`:string, `create_at`:number, `delete_at`:number, `disable_welcome_email`:boolean, `email`:string, `first_name`:string, `id`:string, `last_name`:string, `last_password_update`:number, `locale`:string, `nickname`:string, `notify_props`:object, `position`:string, `remote_id`:string, `roles`:string, `timezone`:object, `update_at`:number, `username`:string

**`GET /api/v4/users/me/preferences`** — seen 2×, status 200
- response (array): `[]`:object, `[].category`:string, `[].name`:string, `[].user_id`:string, `[].value`:string

**`POST /api/v4/users/search`** — seen 15×, status 200
- request (object): `term`:string
- response (array): `[]`:object, `[].auth_service`:string, `[].bot_description`:string, `[].create_at`:number, `[].delete_at`:number, `[].disable_welcome_email`:boolean, `[].email`:string, `[].first_name`:string, `[].id`:string, `[].is_bot`:boolean, `[].last_name`:string, `[].last_picture_update`:number, `[].locale`:string, `[].nickname`:string, `[].notify_props`:object, `[].position`:string, `[].remote_id`:string, `[].roles`:string, `[].timezone`:object, `[].update_at`:number, `[].username`:string

**`GET /api/v4/users/stats`** — seen 1×, status 200
- response (object): `total_users_count`:number

**`GET /api/v4/users/stats/filtered`** — seen 2×, status 200, query: include_bots, include_deleted
- response (object): `total_users_count`:number

**`POST /api/v4/users/status/ids`** — seen 7×, status 200
- request (array): `[]`:string
- response (array): `[]`:object, `[].dnd_end_time`:number, `[].last_activity_at`:number, `[].manual`:boolean, `[].status`:string, `[].user_id`:string

**`GET /api/v4/users/username/{username}`** — seen 1×, status 404
- response (object): `detailed_error`:string, `id`:string, `message`:string, `request_id`:string, `status_code`:number

**`GET /api/v4/users/{id}/channel_members`** — seen 2×, status 200, query: page, per_page

**`GET /api/v4/users/{id}/image`** — seen 6×, status 200, query: _

**`PUT /api/v4/users/{id}/preferences`** — seen 12×, status 200
- request (array): `[]`:object, `[].category`:string, `[].name`:string, `[].user_id`:string, `[].value`:string
- response (object): `status`:string

### 2. Teams

**`GET /api/v4/teams`** — seen 1×, status 200, query: exclude_policy_constrained, for_directory, include_total_count, page, per_page
- response (array): `[]`:object, `[].allow_open_invite`:boolean, `[].allowed_domains`:string, `[].cloud_limits_archived`:boolean, `[].company_name`:string, `[].create_at`:number, `[].delete_at`:number, `[].description`:string, `[].display_name`:string, `[].email`:string, `[].group_constrained`:boolean, `[].id`:string, `[].invite_id`:string, `[].name`:string, `[].policy_auto_add`:boolean, `[].policy_enforced`:boolean, `[].policy_id`:null, `[].policy_is_active`:boolean, `[].scheme_id`:string, `[].type`:string, `[].update_at`:number

**`POST /api/v4/teams`** — seen 1×, status 201
- request (object): `allow_open_invite`:boolean, `allowed_domains`:string, `company_name`:string, `create_at`:number, `delete_at`:number, `description`:string, `display_name`:string, `email`:string, `group_constrained`:boolean, `id`:string, `invite_id`:string, `name`:string, `scheme_id`:string, `type`:string, `update_at`:number
- response (object): `allow_open_invite`:boolean, `allowed_domains`:string, `cloud_limits_archived`:boolean, `company_name`:string, `create_at`:number, `delete_at`:number, `description`:string, `display_name`:string, `email`:string, `group_constrained`:boolean, `id`:string, `invite_id`:string, `name`:string, `policy_auto_add`:boolean, `policy_enforced`:boolean, `policy_id`:null, `policy_is_active`:boolean, `scheme_id`:string, `type`:string, `update_at`:number

**`GET /api/v4/teams/{id}`** — seen 1×, status 200
- response (object): `allow_open_invite`:boolean, `allowed_domains`:string, `cloud_limits_archived`:boolean, `company_name`:string, `create_at`:number, `delete_at`:number, `description`:string, `display_name`:string, `email`:string, `group_constrained`:boolean, `id`:string, `invite_id`:string, `name`:string, `policy_auto_add`:boolean, `policy_enforced`:boolean, `policy_id`:null, `policy_is_active`:boolean, `scheme_id`:string, `type`:string, `update_at`:number

**`POST /api/v4/teams/{id}/invite/email`** — seen 1×, status 200, query: graceful
- request (array): `[]`:string
- response (array): `[]`:object, `[].email`:string, `[].error`:object

**`GET /api/v4/users/me/teams`** — seen 2×, status 200

**`GET /api/v4/users/me/teams/members`** — seen 3×, status 200
- response (array): `[]`:object, `[].delete_at`:number, `[].explicit_roles`:string, `[].roles`:string, `[].scheme_admin`:boolean, `[].scheme_guest`:boolean, `[].scheme_user`:boolean, `[].team_id`:string, `[].user_id`:string

**`GET /api/v4/users/me/teams/unread`** — seen 4×, status 200, query: include_collapsed_threads
- response (array): `[]`:object, `[].mention_count`:number, `[].mention_count_root`:number, `[].msg_count`:number, `[].msg_count_root`:number, `[].team_id`:string, `[].thread_count`:number, `[].thread_mention_count`:number, `[].thread_urgent_mention_count`:number

### 3. Channels

**`POST /api/v4/channels`** — seen 2×, status 201
- request (object): `create_at`:number, `creator_id`:string, `delete_at`:number, `display_name`:string, `group_constrained`:boolean, `header`:string, `id`:string, `last_post_at`:number, `last_root_post_at`:number, `name`:string, `purpose`:string, `scheme_id`:string, `team_id`:string, `type`:string, `update_at`:number
- response (object): `autotranslation`:boolean, `banner_info`:null, `create_at`:number, `creator_id`:string, `default_category_name`:string, `delete_at`:number, `disable_join_leave_messages`:boolean, `discoverable`:boolean, `display_name`:string, `extra_update_at`:number, `group_constrained`:boolean, `header`:string, `id`:string, `last_post_at`:number, `last_root_post_at`:number, `managed_category_name`:string, `name`:string, `policy_auto_add`:boolean, `policy_enforced`:boolean, `policy_id`:null, `policy_is_active`:boolean, `props`:null, `purpose`:string, `scheme_id`:string, `shared`:null, `team_id`:string, `total_msg_count`:number, `total_msg_count_root`:number, `type`:string, `update_at`:number

**`POST /api/v4/channels/members/me/view`** — seen 16×, status 200
- request (object): `channel_id`:string, `collapsed_threads_supported`:boolean
- response (object): `last_viewed_at_times`:object, `status`:string

**`GET /api/v4/channels/{id}`** — seen 2×, status 200
- response (object): `autotranslation`:boolean, `banner_info`:null, `create_at`:number, `creator_id`:string, `default_category_name`:string, `delete_at`:number, `disable_join_leave_messages`:boolean, `discoverable`:boolean, `display_name`:string, `extra_update_at`:number, `group_constrained`:boolean, `header`:string, `id`:string, `last_post_at`:number, `last_root_post_at`:number, `managed_category_name`:string, `name`:string, `policy_auto_add`:boolean, `policy_enforced`:boolean, `policy_id`:null, `policy_is_active`:boolean, `props`:null, `purpose`:string, `scheme_id`:string, `shared`:null, `team_id`:string, `total_msg_count`:number, `total_msg_count_root`:number, `type`:string, `update_at`:number

**`GET /api/v4/channels/{id}/members/me`** — seen 4×, status 200
- response (object): `autotranslation_disabled`:boolean, `channel_id`:string, `explicit_roles`:string, `last_update_at`:number, `last_viewed_at`:number, `mention_count`:number, `mention_count_root`:number, `msg_count`:number, `msg_count_root`:number, `notify_props`:object, `roles`:string, `scheme_admin`:boolean, `scheme_guest`:boolean, `scheme_user`:boolean, `urgent_mention_count`:number, `user_id`:string

**`GET /api/v4/channels/{id}/stats`** — seen 5×, status 200, query: exclude_files_count
- response (object): `channel_id`:string, `files_count`:number, `guest_count`:number, `member_count`:number, `pinnedpost_count`:number

**`GET /api/v4/users/me/channels`** — seen 2×, status 200
- response (array): `[]`:object, `[].autotranslation`:boolean, `[].banner_info`:null, `[].create_at`:number, `[].creator_id`:string, `[].default_category_name`:string, `[].delete_at`:number, `[].disable_join_leave_messages`:boolean, `[].discoverable`:boolean, `[].display_name`:string, `[].extra_update_at`:number, `[].group_constrained`:null, `[].header`:string, `[].id`:string, `[].last_post_at`:number, `[].last_root_post_at`:number, `[].managed_category_name`:string, `[].name`:string, `[].policy_auto_add`:boolean, `[].policy_enforced`:boolean, `[].policy_id`:null, `[].policy_is_active`:boolean, `[].props`:null, `[].purpose`:string, `[].scheme_id`:null, `[].shared`:null, `[].team_id`:string, `[].total_msg_count`:number, `[].total_msg_count_root`:number, `[].type`:string, `[].update_at`:number

**`GET /api/v4/users/me/teams/{id}/channels`** — seen 1×, status 200, query: include_deleted
- response (array): `[]`:object, `[].autotranslation`:boolean, `[].banner_info`:null, `[].create_at`:number, `[].creator_id`:string, `[].default_category_name`:string, `[].delete_at`:number, `[].disable_join_leave_messages`:boolean, `[].discoverable`:boolean, `[].display_name`:string, `[].extra_update_at`:number, `[].group_constrained`:null, `[].header`:string, `[].id`:string, `[].last_post_at`:number, `[].last_root_post_at`:number, `[].managed_category_name`:string, `[].name`:string, `[].policy_auto_add`:boolean, `[].policy_enforced`:boolean, `[].policy_id`:null, `[].policy_is_active`:boolean, `[].props`:null, `[].purpose`:string, `[].scheme_id`:null, `[].shared`:null, `[].team_id`:string, `[].total_msg_count`:number, `[].total_msg_count_root`:number, `[].type`:string, `[].update_at`:number

**`GET /api/v4/users/me/teams/{id}/channels/members`** — seen 1×, status 200
- response (array): `[]`:object, `[].autotranslation_disabled`:boolean, `[].channel_id`:string, `[].explicit_roles`:string, `[].last_update_at`:number, `[].last_viewed_at`:number, `[].mention_count`:number, `[].mention_count_root`:number, `[].msg_count`:number, `[].msg_count_root`:number, `[].notify_props`:object, `[].roles`:string, `[].scheme_admin`:boolean, `[].scheme_guest`:boolean, `[].scheme_user`:boolean, `[].urgent_mention_count`:number, `[].user_id`:string

**`GET /api/v4/users/{id}/teams/{id}/channels/categories`** — seen 4×, status 200
- response (object): `categories`:array, `order`:array

**`PUT /api/v4/users/{id}/teams/{id}/channels/categories`** — seen 2×, status 200
- request (array): `[]`:object, `[].channel_ids`:array, `[].collapsed`:boolean, `[].display_name`:string, `[].id`:string, `[].muted`:boolean, `[].sort_order`:number, `[].sorting`:string, `[].team_id`:string, `[].type`:string, `[].user_id`:string
- response (array): `[]`:object, `[].channel_ids`:array, `[].collapsed`:boolean, `[].display_name`:string, `[].id`:string, `[].muted`:boolean, `[].sort_order`:number, `[].sorting`:string, `[].team_id`:string, `[].type`:string, `[].user_id`:string

### 4. Messages (posts)

**`GET /api/v4/channels/{id}/posts`** — seen 2×, status 200, query: collapsedThreads, collapsedThreadsExtended, since, skipFetchThreads
- response (object): `first_inaccessible_post_time`:number, `next_post_id`:string, `order`:array, `posts`:object, `prev_post_id`:string

**`POST /api/v4/posts`** — seen 6×, status 201
- request (object): `channel_id`:string, `create_at`:number, `file_ids`:array, `message`:string, `metadata`:object, `pending_post_id`:string, `props`:object, `reply_count`:number, `root_id`:string, `update_at`:number, `user_id`:string
- response (object): `channel_id`:string, `create_at`:number, `delete_at`:number, `edit_at`:number, `file_ids`:array, `hashtags`:string, `id`:string, `is_pinned`:boolean, `last_reply_at`:number, `message`:string, `metadata`:object, `original_id`:string, `participants`:null, `pending_post_id`:string, `props`:object, `remote_id`:string, `reply_count`:number, `root_id`:string, `type`:string, `update_at`:number, `user_id`:string

**`DELETE /api/v4/posts/{id}`** — seen 1×, status 200
- response (object): `status`:string

**`PUT /api/v4/posts/{id}/patch`** — seen 1×, status 200
- request (object): `channelId`:string, `channel_id`:string, `createAt`:number, `create_at`:number, `delete_at`:number, `edit_at`:number, `fileInfos`:array, `file_ids`:array, `hashtags`:string, `id`:string, `is_pinned`:boolean, `last_reply_at`:number, `message`:string, `metadata`:object, `original_id`:string, `participants`:null, `pending_post_id`:string, `props`:object, `remote_id`:string, `reply_count`:number, `rootId`:string, `root_id`:string, `type`:string, `updateAt`:number, `update_at`:number, `uploadsInProgress`:array, `user_id`:string
- response (object): `channel_id`:string, `create_at`:number, `delete_at`:number, `edit_at`:number, `file_ids`:array, `hashtags`:string, `id`:string, `is_pinned`:boolean, `last_reply_at`:number, `message`:string, `metadata`:object, `original_id`:string, `participants`:null, `pending_post_id`:string, `props`:object, `reply_count`:number, `root_id`:string, `type`:string, `update_at`:number, `user_id`:string

**`DELETE /api/v4/users/me/channels/{id}/drafts`** — seen 8×, status 200
- response (object): `status`:string

**`GET /api/v4/users/me/teams/{id}/drafts`** — seen 1×, status 200

**`GET /api/v4/users/{id}/channels/{id}/posts/unread`** — seen 4×, status 200, query: collapsedThreads, collapsedThreadsExtended, limit_after, limit_before, skipFetchThreads
- response (object): `first_inaccessible_post_time`:number, `next_post_id`:string, `order`:array, `posts`:object, `prev_post_id`:string

**`GET /api/v4/users/{id}/teams/{id}/threads`** — seen 1×, status 200, query: after, before, deleted, extended, per_page, since, threadsOnly, totalsOnly, unread
- response (object): `threads`:null, `total`:number, `total_unread_mentions`:number, `total_unread_threads`:number, `total_unread_urgent_mentions`:number

### 5. Everything else (config, licence, plugins, telemetry)

**`POST /api/v4/access_control/decisions/actions/search`** — seen 4×, status 200
- request (object): `actions`:array, `resource`:object
- response (object): `decisions`:object, `resource`:object, `results`:array

**`GET /api/v4/agents`** — seen 4×, status 200

**`GET /api/v4/agents/status`** — seen 4×, status 200
- response (object): `available`:boolean, `reason`:string

**`GET /api/v4/analytics/old`** — seen 1×, status 200, query: name, team_id
- response (array): `[]`:object, `[].name`:string, `[].value`:number

**`POST /api/v4/client_perf`** — seen 7×, status 200

**`GET /api/v4/config/client`** — seen 3×, status 200, query: format
- response (object): `AboutLink`:string, `AllowBannerDismissal`:string, `AllowCustomThemes`:string, `AllowDownloadLogs`:string, `AllowPersistentNotifications`:string, `AllowPersistentNotificationsForGuests`:string, `AllowSyncedDrafts`:string, `AllowedThemes`:string, `AndroidAppDownloadLink`:string, `AndroidLatestVersion`:string, `AndroidMinVersion`:string, `AppDownloadLink`:string, `AppsPluginEnabled`:string, `AsymmetricSigningPublicKey`:string, `AuditLoggingActive`:string, `AvailableLocales`:string, `BannerColor`:string, `BannerText`:string, `BannerTextColor`:string, `BuildDate`:string, `BuildEnterpriseReady`:string, `BuildHash`:string, `BuildHashEnterprise`:string, `BuildNumber`:string, `BurnOnReadDurationSeconds`:string, `BurnOnReadMaximumTimeToLiveSeconds`:string, `CWSMock`:string, `CWSURL`:string, `CollapsedThreads`:string, `CustomBrandText`:string, `CustomDescriptionText`:string, `CustomUrlSchemes`:string, `DataRetentionEnableFileDeletion`:string, `DataRetentionEnableMessageDeletion`:string, `DataRetentionFileRetentionHours`:string, `DataRetentionMessageRetentionHours`:string, `DefaultClientLocale`:string, `DefaultTheme`:string, `DelayChannelAutocomplete`:string, `DeleteAccountLink`:string, `DiagnosticId`:string, `DiagnosticsEnabled`:string, `DisableAppBar`:string, `DisableRefetchingOnBrowserFocus`:string, `DisableWakeUpReconnectHandler`:string, `EmailNotificationContentsType`:string, `EnableAccessControlAuditLogging`:string, `EnableAskCommunityLink`:string, `EnableAttributeBasedAccessControl`:string, `EnableBanner`:string, `EnableBotAccountCreation`:string, `EnableBurnOnRead`:string, `EnableChannelCategorySorting`:string, `EnableChannelPolicyIndicators`:string, `EnableChannelViewedMessages`:string, `EnableClientMetrics`:string, `EnableClientPerformanceDebugging`:string, `EnableCluster`:string, `EnableCommands`:string, `EnableCompliance`:string, `EnableComplianceExport`:string, `EnableConfirmNotificationsToChannel`:string, `EnableCrossTeamSearch`:string, `EnableCustomBrand`:string, `EnableCustomEmoji`:string, `EnableCustomGroups`:string, `EnableCustomUserStatuses`:string, `EnableDesktopLandingPage`:string, `EnableDeveloper`:string, `EnableDiagnostics`:string, `EnableEmailBatching`:string, `EnableEmailInvitations`:string, `EnableEmojiPicker`:string, `EnableFile`:string, `EnableFileAttachments`:string, `EnableGifPicker`:string, `EnableGuestAccounts`:string, `EnableGuestMagicLink`:string, `EnableIncomingWebhooks`:string, `EnableInlineLatex`:string, `EnableJoinLeaveMessageByDefault`:string, `EnableLastActiveTime`:string, `EnableLatex`:string, `EnableLdap`:string, `EnableLinkPreviews`:string, `EnableMarketplace`:string, `EnableMetrics`:string, `EnableMobileFileDownload`:string, `EnableMobileFileUpload`:string, `EnableMultifactorAuthentication`:string, `EnableOAuthServiceProvider`:string, `EnableOnboardingFlow`:string, `EnableOpenServer`:string, `EnableOutgoingOAuthConnections`:string, `EnableOutgoingWebhooks`:string, `EnablePermalinkPreviews`:string, `EnablePostIconOverride`:string, `EnablePostUsernameOverride`:string, `EnablePreviewModeBanner`:string, `EnablePublicLink`:string, `EnableSVGs`:string, `EnableSaml`:string, `EnableSignInWithEmail`:string, `EnableSignInWithUsername`:string, `EnableSignUpWithEmail`:string, `EnableSignUpWithGoogle`:string, `EnableSignUpWithOffice365`:string, `EnableSignUpWithOpenId`:string, `EnableTesting`:string, `EnableThemeSelection`:string, `EnableTutorial`:string, `EnableUserAccessTokens`:string, `EnableUserCreation`:string, `EnableUserDeactivation`:string, `EnableUserManagedAttributes`:string, `EnableUserStatuses`:string, `EnableUserTypingMessages`:string, `EnforceMultifactorAuthentication`:string, `ExperimentalEnableAuthenticationTransfer`:string, `ExperimentalEnableAutomaticReplies`:string, `ExperimentalEnableDefaultChannelLeaveJoinMessages`:string, `ExperimentalEnablePostMetadata`:string, `ExperimentalGroupUnreadChannels`:string, `ExperimentalPrimaryTeam`:string, `ExperimentalSharedChannels`:string, `ExperimentalTimezone`:string, `ExtendSessionLengthWithActivity`:string, `FeatureFlagAggregatePluginMetrics`:string, `FeatureFlagAppsEnabled`:string, `FeatureFlagAttributeValueMasking`:string, `FeatureFlagAutoTranslation`:string, `FeatureFlagBurnOnRead`:string, `FeatureFlagCJKSearch`:string, `FeatureFlagChannelAttributes`:string, `FeatureFlagChannelAttributesRequired`:string, `FeatureFlagChannelPermissionPolicies`:string, `FeatureFlagClassificationMarkings`:string, `FeatureFlagClusterGracefulDrain`:string, `FeatureFlagContentFlagging`:string, `FeatureFlagDiscoverableChannels`:string, `FeatureFlagEnableAIPluginBridge`:string, `FeatureFlagEnableAIRecaps`:string, `FeatureFlagEnableDocs`:string, `FeatureFlagEnableMFIPluginSignaturePublicKey`:string, `FeatureFlagEnableMattermostEntry`:string, `FeatureFlagEnableSharedChannelsDMs`:string, `FeatureFlagEnableShiftEscapeToMarkAllRead`:string, `FeatureFlagEnableSyncAllUsersForRemoteCluster`:string, `FeatureFlagEnforceLogPathRoot`:string, `FeatureFlagExperimentalCrossTeamSearch`:string, `FeatureFlagIntegratedBoards`:string, `FeatureFlagManagedChannelCategories`:string, `FeatureFlagMmBlocksEnabled`:string, `FeatureFlagMobileEphemeralMode`:string, `FeatureFlagMobileSSOCodeExchange`:string, `FeatureFlagMoveThreadsEnabled`:string, `FeatureFlagNormalizeLdapDNs`:string, `FeatureFlagNotificationMonitoring`:string, `FeatureFlagPermissionPolicies`:string, `FeatureFlagPolicySimulation`:string, `FeatureFlagPostAttributes`:string, `FeatureFlagPostDeliveryTracking`:string, `FeatureFlagPropertyFieldGraph`:string, `FeatureFlagPropertyFieldRank`:string, `FeatureFlagRecurringScheduledPosts`:string, `FeatureFlagResourceAttributesInPolicies`:string, `FeatureFlagSessionAttributes`:string, `FeatureFlagTeamMembershipAccessControl`:string, `FeatureFlagTestBoolFeature`:string, `FeatureFlagTestFeature`:string, `FeatureFlagWysiwygEditor`:string, `FileLevel`:string, `ForgotPasswordLink`:string, `GiphySdkKey`:string, `GoogleDeveloperKey`:string, `GuestAccountsEnforceMultifactorAuthentication`:string, `HasImageProxy`:string, `HelpLink`:string, `HideGuestTags`:string, `InstallationDate`:string, `IosAppDownloadLink`:string, `IosLatestVersion`:string, `IosMinVersion`:string, `IsDefaultMarketplace`:string, `IsFipsEnabled`:string, `LdapFirstNameAttributeSet`:string, `LdapLastNameAttributeSet`:string, `LdapLoginFieldName`:string, `LdapNicknameAttributeSet`:string, `LdapPictureAttributeSet`:string, `LdapPositionAttributeSet`:string, `LockProfileFieldsForEmailUsers`:string, `LockTeammateNameDisplay`:string, `ManagedResourcePaths`:string, `MaxFileSize`:string, `MaxMarkdownNodes`:string, `MaxNotificationsPerChannel`:string, `MaxPostSize`:string, `MaximumPersonalAccessTokenLifetimeDays`:string, `MinimumHashtagLength`:string, `MobileExternalBrowser`:string, `NoAccounts`:string, `OpenIdButtonColor`:string, `OpenIdButtonText`:string, `PasswordEnableForgotLink`:string, `PasswordMinimumLength`:string, `PasswordRequireLowercase`:string, `PasswordRequireNumber`:string, `PasswordRequireSymbol`:string, `PasswordRequireUppercase`:string, `PersistentNotificationIntervalMinutes`:string, `PersistentNotificationMaxCount`:string, `PersistentNotificationMaxRecipients`:string, `PluginsEnabled`:string, `PostEditTimeLimit`:string, `PostPriority`:string, `PrivacyPolicyLink`:string, `ReportAProblemLink`:string, `ReportAProblemMail`:string, `ReportAProblemType`:string, `RequireEmailVerification`:string, `RestrictDirectMessage`:string, `RunJobs`:string, `SQLDriverName`:string, `SamlFirstNameAttributeSet`:string, `SamlLastNameAttributeSet`:string, `SamlLoginButtonText`:string, `SamlNicknameAttributeSet`:string, `SamlPositionAttributeSet`:string, `SchemaVersion`:string, `SendEmailNotifications`:string, `SendPushNotifications`:string, `ServiceEnvironment`:string, `ShowEmailAddress`:string, `ShowFullName`:string, `SiteName`:string, `SiteURL`:string, `SupportEmail`:string, `TeammateNameDisplay`:string, `TelemetryId`:string, `TermsOfServiceLink`:string, `TimeBetweenUserTypingUpdatesMilliseconds`:string, `UniqueEmojiReactionLimitPerPost`:string, `UpgradedFromTE`:string, `UseAnonymousURLs`:string, `UsersStatusAndProfileFetchingPollIntervalMilliseconds`:string, `Version`:string, `WebsocketPort`:string, `WebsocketSecurePort`:string, `WebsocketURL`:string, `WranglerAllowedEmailDomain`:string, `WranglerMoveThreadFromDirectMessageChannelEnable`:string, `WranglerMoveThreadFromGroupMessageChannelEnable`:string, `WranglerMoveThreadFromPrivateChannelEnable`:string, `WranglerMoveThreadMaxCount`:string, `WranglerMoveThreadToAnotherTeamEnable`:string, `WranglerPermittedWranglerRoles`:string, `YoutubeReferrerPolicy`:string

**`GET /api/v4/license/client`** — seen 2×, status 200
- response (object): `IsLicensed`:string

**`GET /api/v4/limits/server`** — seen 4×, status 200
- response (object): `activeUserCount`:number, `lastAccessiblePostTime`:number, `maxUsersHardLimit`:number, `maxUsersLimit`:number, `postHistoryLimit`:number, `singleChannelGuestCount`:number, `singleChannelGuestLimit`:number

**`GET /api/v4/plugins/webapp`** — seen 1×, status 200
- response (array): `[]`:object, `[].homepage_url`:string, `[].icon_path`:string, `[].id`:string, `[].min_server_version`:string, `[].name`:string, `[].props`:object, `[].release_notes_url`:string, `[].support_url`:string, `[].version`:string, `[].webapp`:object

**`GET /api/v4/properties/groups/access_control/system/fields`** — seen 3×, status 200, query: target_type

**`GET /api/v4/properties/groups/access_control/system/values`** — seen 1×, status 200

**`GET /api/v4/properties/groups/access_control/template/fields`** — seen 1×, status 200, query: target_type

**`GET /api/v4/system/notices/{id}`** — seen 1×, status 200, query: client, clientVersion

**`GET /api/v4/system/onboarding/complete`** — seen 1×, status 200
- response (object): `name`:string, `value`:string

**`POST /api/v4/system/onboarding/complete`** — seen 1×, status 200
- request (object): `install_plugins`:array, `organization`:string
- response (object): `status`:string

**`GET /api/v4/trial-license/prev`** — seen 4×, status 403
- response (object): `detailed_error`:string, `id`:string, `message`:string, `request_id`:string, `status_code`:number

**`GET /plugins/github/api/v1/connected`** — seen 2×, status 404, query: reminder

## WebSocket (live updates)

- `/api/v4/websocket`: 15 frames (2 sent, 13 received)
- `/api/v4/websocket`: 121 frames (28 sent, 93 received)

| Direction | Frames | Top-level fields | Fields inside `data` |
|---|---|---|---|
| received | 16 | seq_reply, status | - |
| sent | 14 | action, seq | - |
| received | 14 | data, seq_reply, status | node_id, server_time, text, version |
| received | 12 | broadcast, data, event, seq | - |
| received | 12 | broadcast, data, event, seq | preferences |
| received | 12 | broadcast, data, event, seq | plugin_statuses |
| received | 9 | broadcast, data, event, seq | channel_display_name, channel_name, channel_type, post, sender_name, set_online, team_id |
| sent | 9 | action, data, seq | channel_id, parent_id |
| received | 8 | broadcast, data, event, seq | left_sidebar_enabled, review_target_day_type, review_target_days |
| sent | 6 | action, data, seq | channel_id |
| received | 6 | broadcast, data, event, seq | channel_times |
| received | 4 | broadcast, data, event, seq | config |
| received | 2 | broadcast, data, event, seq | connection_id, server_hostname, server_version |
| received | 2 | broadcast, data, event, seq | user |
| received | 2 | broadcast, data, event, seq | team_id, user_id |
| received | 2 | broadcast, data, event, seq | channel_id, team_id |
| received | 2 | broadcast, data, event, seq | updatedCategories |
| received | 1 | broadcast, data, event, seq | status, user_id |
| sent | 1 | action, data, seq | team_id |
| received | 1 | broadcast, data, event, seq | delete_by, post |
| received | 1 | broadcast, data, event, seq | post |

## Limits REA reported

- Observation starts when REA attaches; prior network and console activity is unavailable.
- Worker targets are a final inventory snapshot, not worker-session observation. Their attached flag describes any debugging client; this page-session collector does not observe worker network, console, or script events, and short-lived workers may be absent from the snapshot.
- Raw network headers, bodies, cookies, storage values, console objects, and WebSocket payloads are never retained; explicitly requested primitive console text is retained verbatim, while payload captures retain value-free shapes.
- Source maps are reported only as declarative URLs and are not fetched.
- URL-less scripts and console events without an allowed source URL are excluded because their origin cannot be proven.
- CDP discovery could not establish the target's debugger attachment state.
- Body shapes were unavailable for some non-JSON responses (for example profile images).
