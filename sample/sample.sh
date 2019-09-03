#!/bin/bash
SLACK_INCOMING_WEBHOOK_URL="$1"
if [[ -z "${SLACK_INCOMING_WEBHOOK_URL}" ]]; then
  echo "Usage: $0 <SLACK_INCOMING_WEBHOOK_URL>"
  exit 1
fi

curl -v http://localhost:8080/watchers -H 'Content-type:application/json' -d "@-" << EOS
[
  {
    "interval": 60,
    "source": {
      "type": "html",
      "params": {
        "uri": "https://github.com/trending",
        "selector": "article"
      }
    },
    "sink": {
      "type": "slack",
      "params": {
        "incomingWebhookUrl": "${SLACK_INCOMING_WEBHOOK_URL}",
        "template": "Check {{ current.subject }} in https://github.com !"
      }
    }
  }
]
EOS
