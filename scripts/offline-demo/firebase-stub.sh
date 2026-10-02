#!/usr/bin/env bash
# Writes placeholder Firebase config so local dev builds work without real Firebase credentials.
# The committed native projects reference GoogleService-Info.plist and the google-services Gradle
# plugin, so the files must exist. Analytics/push won't work with these; nothing in the demo needs them.
# Existing (real) files are never overwritten.
set -euo pipefail
cd "$(dirname "$0")/../../mobile"

# Firebase iOS raises if the API key isn't 39 chars starting with "A".
KEY="AIzaSy$(printf '0%.0s' {1..33})"

[ -f GoogleService-Info.plist ] || cat > GoogleService-Info.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>API_KEY</key><string>$KEY</string>
  <key>GCM_SENDER_ID</key><string>000000000000</string>
  <key>BUNDLE_ID</key><string>com.cmms.atlas</string>
  <key>PROJECT_ID</key><string>atlas-offline-demo</string>
  <key>STORAGE_BUCKET</key><string>atlas-offline-demo.appspot.com</string>
  <key>GOOGLE_APP_ID</key><string>1:000000000000:ios:0000000000000000</string>
  <key>IS_ANALYTICS_ENABLED</key><false/>
  <key>IS_GCM_ENABLED</key><true/>
  <key>IS_SIGNIN_ENABLED</key><true/>
  <key>PLIST_VERSION</key><string>1</string>
</dict>
</plist>
EOF

[ -f android/app/google-services.json ] || cat > android/app/google-services.json <<EOF
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "atlas-offline-demo",
    "storage_bucket": "atlas-offline-demo.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000",
        "android_client_info": { "package_name": "com.atlas.cmms" }
      },
      "oauth_client": [],
      "api_key": [{ "current_key": "$KEY" }],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    }
  ],
  "configuration_version": "1"
}
EOF
echo "Firebase stub config in place."
