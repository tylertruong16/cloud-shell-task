# README

## Requirements

- **Java**: 22
- **Server**: Must have Chrome installed

## Environment Variables

### `PROFILE_TABLE_URL`

This is the URL path to the profile table. For example: `https://xxx/table/profile_manager`.

### `CMD`

This contains the command to be executed in Google Shell. For example, to run the following Docker command:

#### run docker run -d -p 5901:5901 -p -e {0} 6901:6901  nhatdoanexpo/vetgodeploy:v1.0
The app will replace the `{0}` placeholder with the email address.
