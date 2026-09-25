#!/usr/bin/env python3
"""Local-only staging sync backend for the original prototype.

This server is deterministic test infrastructure. It has no account auth, no
production database, and no R2 integration. It exists to exercise cursor,
idempotency, tombstone, entitlement, and quota behavior locally.
"""

import argparse
import json
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


class SyncState:
    def __init__(self, quota_bytes=0):
        self.revision = 0
        self.operations = {}
        self.changes = []
        self.quota_bytes = quota_bytes


def json_bytes(value):
    return (json.dumps(value, separators=(",", ":")) + "\n").encode("utf-8")


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):  # noqa: N802 - BaseHTTPRequestHandler API
        parsed = urlparse(self.path)
        if parsed.path == "/v1/sync/entitlement":
            self.respond(200, {"entitlement": "CLOUD_SYNC", "quota_bytes": self.server.state.quota_bytes})
            return
        if parsed.path == "/v1/sync/changes":
            cursor = int(parse_qs(parsed.query).get("cursor", ["0"])[0])
            changes = [change for change in self.server.state.changes
                       if change["server_revision"] > cursor]
            self.respond(200, {"cursor": self.server.state.revision, "changes": changes})
            return
        self.respond(404, {"error": "unknown endpoint"})

    def do_POST(self):  # noqa: N802 - BaseHTTPRequestHandler API
        if self.path != "/v1/sync/push":
            self.respond(404, {"error": "unknown endpoint"})
            return
        try:
            body = json.loads(self.read_body().decode("utf-8"))
            operations = body.get("operations", [])
            if not isinstance(operations, list):
                raise ValueError("operations must be a list")
            total_bytes = sum(len(json.dumps(item).encode("utf-8")) for item in operations)
            if self.server.state.quota_bytes and total_bytes > self.server.state.quota_bytes:
                self.respond(413, {"error": "quota", "retryable": False})
                return
            accepted = []
            for operation in operations:
                operation_id = operation.get("operation_id", "")
                if not operation_id:
                    raise ValueError("operation_id is required")
                if operation_id in self.server.state.operations:
                    accepted.append(operation_id)
                    continue
                payload = operation.get("payload", "")
                lowered = str(payload).lower()
                if "authorization" in lowered or "bearer " in lowered or "api_key" in lowered:
                    raise ValueError("provider credentials are not accepted by sync")
                self.server.state.revision += 1
                action = operation.get("action", "UPSERT")
                change = {
                    "entity_id": operation.get("entity_id", ""),
                    "entity_type": operation.get("entity_type", ""),
                    "payload": "" if action == "DELETE" else payload,
                    "updated_at": operation.get("created_at", 0),
                    "deleted_at": operation.get("created_at", 0) if action == "DELETE" else 0,
                    "origin_device_id": operation.get("device_id", "staging-device"),
                    "server_revision": self.server.state.revision,
                }
                self.server.state.operations[operation_id] = change
                self.server.state.changes.append(change)
                accepted.append(operation_id)
            self.respond(200, {"accepted": accepted, "cursor": self.server.state.revision})
        except (ValueError, json.JSONDecodeError) as error:
            self.respond(400, {"error": str(error)})

    def read_body(self):
        length = int(self.headers.get("Content-Length", "0"))
        return self.rfile.read(length)

    def respond(self, status, payload):
        body = json_bytes(payload)
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format_string, *args):
        if not getattr(self.server, "quiet", False):
            sys.stderr.write("[mochi-sync-staging] " + (format_string % args) + "\n")


class StagingServer(ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8787)
    parser.add_argument("--quota-bytes", type=int, default=0)
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()
    server = StagingServer((args.host, args.port), Handler)
    server.state = SyncState(args.quota_bytes)
    server.quiet = args.quiet
    print("MoCHi local sync staging listening on http://%s:%d/" % server.server_address, flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.shutdown()
        server.server_close()


if __name__ == "__main__":
    main()
