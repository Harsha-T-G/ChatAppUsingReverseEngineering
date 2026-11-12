#!/usr/bin/env python3
"""Replays the 12 recorded calls against the running backend and compares each
response with what REA recorded (docs/rea-contract.json).

Usage:  python3 scripts/demo.py [base_url]      (default http://localhost:8090)

For every call it prints the status (ours vs recorded) and how many of the
recorded response fields our backend returns. Needs only the Python standard
library.
"""
import json
import pathlib
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://localhost:8090"
CONTRACT = json.loads((pathlib.Path(__file__).resolve().parent.parent / "docs" / "rea-contract.json").read_text())
RECORDED = {(c["method"], c["path"]): c for c in CONTRACT["calls"]}

results = []


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(BASE + path, data=data, method=method)
    if data is not None:
        request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return response.status, json.loads(response.read() or b"null"), response.headers.get("Token")
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read() or b"null"), None


def field_names(payload):
    """Top-level field names; for a list, the fields of its items."""
    if isinstance(payload, list):
        return sorted({key for item in payload if isinstance(item, dict) for key in item})
    return sorted(payload) if isinstance(payload, dict) else []


def check(label, method, recorded_path, status, payload):
    recorded = RECORDED[(method, recorded_path)]
    ours = set(field_names(payload))
    expected = set(recorded["response_fields"])
    matched = ours & expected
    missing = sorted(expected - ours)
    status_ok = status in recorded["statuses"]
    results.append((label, method, recorded_path, status, recorded["statuses"], len(matched), len(expected), missing, status_ok))
    mark = "OK  " if status_ok else "DIFF"
    coverage = f"{len(matched)}/{len(expected)} recorded fields" if expected else "no fields recorded (list was empty in the recording)"
    print(f"[{mark}] {label:<16} {method:<6} {recorded_path:<42} status {status} (recorded {recorded['statuses']})  {coverage}")


def main():
    stamp = str(int(time.time()))
    username, email, password = f"demo{stamp}", f"demo{stamp}@example.com", "masterclass-demo-1"
    print(f"Backend: {BASE}\nBlueprint: REA evidence {CONTRACT['evidence_id'][:16]}...\n")

    status, user, _ = call("POST", "/api/v4/users", body={"email": email, "username": username, "password": password})
    check("Sign up", "POST", "/api/v4/users", status, user)

    status, logged_in, token = call("POST", "/api/v4/users/login", body={"id": user["id"], "password": password, "token": "", "device_id": ""})
    check("Log in", "POST", "/api/v4/users/login", status, logged_in)

    status, me, _ = call("GET", "/api/v4/users/me", token)
    check("Who am I", "GET", "/api/v4/users/me", status, me)

    status, team, _ = call("POST", "/api/v4/teams", token, {"name": f"team-{stamp}", "display_name": "Masterclass", "type": "O"})
    check("Create team", "POST", "/api/v4/teams", status, team)

    status, teams, _ = call("GET", "/api/v4/users/me/teams", token)
    check("My teams", "GET", "/api/v4/users/me/teams", status, teams)

    status, channel, _ = call("POST", "/api/v4/channels", token, {"team_id": team["id"], "name": "demo", "display_name": "Demo", "type": "O"})
    check("Create channel", "POST", "/api/v4/channels", status, channel)

    status, channels, _ = call("GET", f"/api/v4/users/me/teams/{team['id']}/channels", token)
    check("My channels", "GET", "/api/v4/users/me/teams/{id}/channels", status, channels)

    status, one_channel, _ = call("GET", f"/api/v4/channels/{channel['id']}", token)
    check("Open channel", "GET", "/api/v4/channels/{id}", status, one_channel)

    posts = []
    for text in ("Hello masterclass", "This backend was rebuilt from a recording", "This one will be deleted"):
        status, post, _ = call("POST", "/api/v4/posts", token, {"channel_id": channel["id"], "message": text})
        posts.append(post)
    check("Send message", "POST", "/api/v4/posts", status, post)

    status, edited, _ = call("PUT", f"/api/v4/posts/{posts[1]['id']}/patch", token, {"message": posts[1]["message"] + " (edited)"})
    check("Edit message", "PUT", "/api/v4/posts/{id}/patch", status, edited)

    status, deleted, _ = call("DELETE", f"/api/v4/posts/{posts[2]['id']}", token)
    check("Delete message", "DELETE", "/api/v4/posts/{id}", status, deleted)

    status, listing, _ = call("GET", f"/api/v4/channels/{channel['id']}/posts", token)
    check("Read messages", "GET", "/api/v4/channels/{id}/posts", status, listing)

    print("\nMessages now in the channel (newest first):")
    for post_id in listing["order"]:
        print("  -", listing["posts"][post_id]["message"])

    total_matched = sum(r[5] for r in results)
    total_expected = sum(r[6] for r in results)
    statuses_ok = sum(1 for r in results if r[8])
    print(f"\nSummary: {statuses_ok}/{len(results)} calls return the recorded status; "
          f"{total_matched}/{total_expected} recorded response fields are returned.")
    missing = [(r[0], r[7]) for r in results if r[7]]
    if missing:
        print("Recorded fields this mini version does not return yet:")
        for label, names in missing:
            print(f"  {label}: {', '.join(names)}")
    return 0 if statuses_ok == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
