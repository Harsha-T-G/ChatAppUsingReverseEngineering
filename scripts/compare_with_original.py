#!/usr/bin/env python3
"""Compares a REA recording of THIS backend with the REA recording of the original chat server.

The idea: point REA at the rebuilt chat page, click through it, and let the same tool that
produced the blueprint judge the rebuild.

Usage:
  python3 scripts/compare_with_original.py <rea-capture-of-rebuild.json>

  # how docs/original-shapes.json was produced (needs the original capture, which is not in the repo):
  python3 scripts/compare_with_original.py --summarize <rea-capture-of-original.json> > docs/original-shapes.json

A REA capture is the JSON printed by:
  rea inspect-web-page http://127.0.0.1:<port> <TARGET_ID> --observation-ms 75000 \
      --include-json-body-shapes --include-websocket-shapes --json

Only field names, types and status codes are compared, because that is all REA records.
"""
import json
import pathlib
import re
import sys
from urllib.parse import urlsplit

ORIGINAL = pathlib.Path(__file__).resolve().parent.parent / "docs" / "original-shapes.json"
ID = re.compile(r"/[a-z0-9]{26}(?=/|$)")
USERNAME = re.compile(r"/users/username/[^/]+")


def normalize(url):
    path = urlsplit(url).path
    path = ID.sub("/{id}", path)
    return USERNAME.sub("/users/username/{username}", path)


def top_level_fields(shape):
    """Top-level field names of an object, or of the items when the body is an array."""
    if not shape:
        return []
    root = shape.get("root_type")
    names = set()
    for item in shape.get("properties", []):
        path = item["path"]
        if root == "array":
            if len(path) == 2 and path[1]["kind"] == "property":
                names.add(path[1]["name"])
        elif len(path) == 1 and path[0]["kind"] == "property":
            names.add(path[0]["name"])
    return sorted(names)


def summarize(capture):
    result = capture["normalized_result"]
    calls = {}
    for request in result["network"]["requests"]:
        path = normalize(request["url"])
        if "/api/" not in path:
            continue
        key = request["method"] + " " + path
        call = calls.setdefault(key, {"times_seen": 0, "statuses": set(), "query": set(), "request": set(), "response": set()})
        call["times_seen"] += 1
        call["statuses"].add(request["status"])
        query = urlsplit(request["url"]).query
        call["query"] |= {pair.split("=")[0] for pair in query.split("&") if pair}
        shapes = request.get("body_shapes") or {}
        call["request"] |= set(top_level_fields(shapes.get("request")))
        call["response"] |= set(top_level_fields(shapes.get("response")))

    frames = {}
    for connection in result["network"]["websocket_connections"]:
        for event in connection["events"]:
            shape = (event.get("payload_shape") or {}).get("json_shape") or {}
            fields = set()
            for item in shape.get("properties", []):
                path = item["path"]
                if len(path) <= 2 and all(part["kind"] == "property" for part in path):
                    fields.add(".".join(part["name"] for part in path))
            if not fields:
                continue
            key = event["direction"] + ": " + ", ".join(sorted(fields))
            frames[key] = frames.get(key, 0) + 1

    return {
        "evidence_id": capture["evidence_id"],
        "requests": len(result["network"]["requests"]),
        "dropped_events": result["completeness"]["dropped_events"]["total"],
        "calls": {key: {name: sorted(value) if isinstance(value, set) else value for name, value in call.items()}
                  for key, call in sorted(calls.items())},
        "websocket_frames": dict(sorted(frames.items())),
    }


def compare(rebuild, original):
    print(f"Original recording: {original['evidence_id'][:19]}...  ({len(original['calls'])} endpoints)")
    print(f"Rebuild recording:  {rebuild['evidence_id'][:19]}...  ({len(rebuild['calls'])} endpoints, "
          f"{rebuild['requests']} requests, {rebuild['dropped_events']} dropped events)\n")

    unknown, status_diff, extra_fields = [], [], []
    matched = expected = 0
    for key, ours in rebuild["calls"].items():
        theirs = original["calls"].get(key)
        if theirs is None:
            unknown.append(key)
            print(f"[NEW ] {key}  (not in the original recording)")
            continue
        success = [s for s in ours["statuses"] if s < 400]
        status_ok = all(s in theirs["statuses"] for s in success)
        got = set(ours["response"]) & set(theirs["response"])
        # Error bodies (4xx) have their own shape, so only successful calls count towards "extra" fields.
        extra_response = sorted(set(ours["response"]) - set(theirs["response"])) if theirs["response"] and all(s < 400 for s in ours["statuses"]) else []
        extra_request = sorted(set(ours["request"]) - set(theirs["request"]))
        matched += len(got)
        expected += len(theirs["response"])
        if not status_ok:
            status_diff.append(key)
        if extra_response or extra_request:
            extra_fields.append((key, extra_request, extra_response))
        mark = "OK  " if status_ok and not extra_response else "DIFF"
        print(f"[{mark}] {key:<52} status {ours['statuses']} (original {theirs['statuses']})  "
              f"{len(got)}/{len(theirs['response'])} response fields")

    print("\nLive-update frames (field names per frame):")
    frame_new = []
    for key, count in rebuild["websocket_frames"].items():
        known = key in original["websocket_frames"]
        if not known:
            frame_new.append(key)
        print(f"  [{'OK  ' if known else 'NEW '}] {count}x {key}")

    print("\nSummary")
    print(f"  Endpoints the rebuilt page calls that exist in the original: {len(rebuild['calls']) - len(unknown)}/{len(rebuild['calls'])}")
    print(f"  Same success status as the original: {len(rebuild['calls']) - len(unknown) - len(status_diff)}/{len(rebuild['calls']) - len(unknown)}")
    print(f"  Original response fields returned: {matched}/{expected}")
    print(f"  Live-update frame shapes also seen in the original: {len(rebuild['websocket_frames']) - len(frame_new)}/{len(rebuild['websocket_frames'])}")
    if extra_fields:
        print("  Fields the rebuild uses that the original recording does not have:")
        for key, extra_request, extra_response in extra_fields:
            if extra_request:
                print(f"    {key}: request {extra_request}")
            if extra_response:
                print(f"    {key}: response {extra_response}")
    if frame_new:
        print("  Frame shapes the original recording does not have:")
        for key in frame_new:
            print(f"    {key}")


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--summarize":
        json.dump(summarize(json.load(open(sys.argv[2]))), sys.stdout, indent=2)
        print()
        return 0
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    compare(summarize(json.load(open(sys.argv[1]))), json.loads(ORIGINAL.read_text()))
    return 0


if __name__ == "__main__":
    sys.exit(main())
