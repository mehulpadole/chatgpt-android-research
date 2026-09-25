#!/usr/bin/env python3
"""Original, deterministic NDJSON stream backend for the Phase 5 prototype.

This is a test harness, not an OpenAI-compatible server and not a production
service. It intentionally accepts synthetic requests without authentication.
"""

import argparse
import json
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def frame(event_type, turn_id, **fields):
    payload = {"type": event_type, "turn_id": turn_id}
    payload.update(fields)
    return (json.dumps(payload, separators=(",", ":")) + "\n").encode("utf-8")


class StreamHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_POST(self):  # noqa: N802 - BaseHTTPRequestHandler API
        if self.path != "/v1/test-stream":
            self.send_error(404, "unknown test endpoint")
            return

        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        try:
            request = json.loads(body.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            self.send_error(400, "invalid request JSON")
            return

        scenario = request.get("scenario", "NORMAL")
        turn_id = request.get("turn_id", "unknown-turn")
        self.send_response(200)
        self.send_header("Content-Type", "application/x-ndjson; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Connection", "close")
        self.end_headers()

        try:
            self.wfile.write(frame("started", turn_id, request_id="test-request-1"))
            self.wfile.flush()

            if scenario == "FAIL_BEFORE_CONTENT":
                self.emit_failure(turn_id, "provider", "synthetic pre-content failure", False)
            elif scenario == "EMPTY":
                self.emit_completed(turn_id)
            elif scenario == "FAIL_AFTER_PARTIAL":
                self.emit_delta(turn_id, "partial ")
                self.emit_delta(turn_id, "content")
                self.emit_failure(turn_id, "provider", "synthetic partial failure", False)
            elif scenario == "DISCONNECT":
                self.emit_delta(turn_id, "partial")
                return
            elif scenario == "MALFORMED":
                self.wfile.write(b"not-json\n")
                self.wfile.flush()
            elif scenario == "CANCEL":
                for index in range(100):
                    self.emit_delta(turn_id, "slow-%d " % index)
                    time.sleep(0.05)
                self.emit_completed(turn_id)
            else:
                if scenario == "SLOW":
                    time.sleep(0.15)
                self.emit_delta(turn_id, "first ")
                if scenario == "SLOW":
                    time.sleep(0.15)
                self.emit_delta(turn_id, "second")
                self.emit_completed(turn_id)
                if scenario == "DUPLICATE_TERMINAL":
                    self.emit_completed(turn_id)
                elif scenario == "DELTA_AFTER_TERMINAL":
                    self.emit_delta(turn_id, " late")
                elif scenario == "FAIL_AFTER_TERMINAL":
                    self.emit_failure(turn_id, "provider", "late failure", False)
        except (BrokenPipeError, ConnectionResetError):
            # Expected for the cancellation scenario when the Android client
            # disconnects the stream instead of merely changing UI state.
            return

    def emit_delta(self, turn_id, text):
        self.wfile.write(frame("delta", turn_id, text=text))
        self.wfile.flush()

    def emit_completed(self, turn_id):
        self.wfile.write(frame("completed", turn_id))
        self.wfile.flush()

    def emit_failure(self, turn_id, category, message, retryable):
        self.wfile.write(frame("failed", turn_id, category=category,
                               message=message, retryable=retryable))
        self.wfile.flush()

    def log_message(self, format_string, *args):
        if not getattr(self.server, "quiet", False):
            sys.stderr.write("[phase5-backend] " + (format_string % args) + "\n")


class TestServer(ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    server = TestServer((args.host, args.port), StreamHandler)
    server.quiet = args.quiet
    print("Phase 5 test backend listening on http://%s:%d/" % server.server_address,
          flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.shutdown()
        server.server_close()


if __name__ == "__main__":
    main()
