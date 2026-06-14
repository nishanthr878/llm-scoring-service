SSE code is spread across three layers. Here's exactly where each part lives:

---

**Backend — 3 files:**

**1. `SseEmitterService.java`**
`src/main/java/com/llmscoring/service/SseEmitterService.java`

This is the core SSE engine:
- Maintains a list of all connected clients (`CopyOnWriteArrayList<SseEmitter>`)
- `createEmitter()` — creates a new SSE connection, sends initial heartbeat
- `push()` — sends events to all connected clients
- `pushScoringResult()`, `pushAlert()`, `pushTurnFlag()` — typed push methods
- `heartbeat()` — scheduled every 15s to keep connections alive

**2. `LiveMonitorController.java`**
`src/main/java/com/llmscoring/controller/LiveMonitorController.java`

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream() {
    return sseEmitterService.createEmitter();
}
```
This is the single endpoint the browser connects to. `TEXT_EVENT_STREAM_VALUE` is what tells Spring to treat it as SSE.

**3. `ScoringService.java`**
`src/main/java/com/llmscoring/service/ScoringService.java`

In `processFlags()` — this is where SSE events get triggered after every evaluation:
```java
sseEmitterService.pushScoringResult(result, flags);
sseEmitterService.pushTurnFlag(flag);
```

---

**Frontend — 1 file:**

**`LiveMonitor.jsx`**
`src/pages/LiveMonitor.jsx`

The SSE client lives entirely in the `useEffect` hook:
```javascript
const es = new EventSource('/api/v1/monitor/stream')
es.onopen = () => setConnected(true)
es.onerror = () => { reconnect after 3s }
es.addEventListener('scoring-result', ...)
es.addEventListener('turn-flag', ...)
es.addEventListener('alert', ...)
es.addEventListener('connected', ...)
es.addEventListener('heartbeat', ...)
```

`EventSource` is the browser's built-in SSE client. No library needed.

---

**How it all connects:**

```
Browser opens EventSource('/api/v1/monitor/stream')
              ↓
LiveMonitorController.stream()
              ↓
SseEmitterService.createEmitter() → added to emitters list
              ↓
Bot sends conversation → ScoringService evaluates
              ↓
ScoringService.processFlags()
              ↓
SseEmitterService.pushScoringResult()
              ↓
Loops through all emitters → emitter.send(event)
              ↓
Browser EventSource fires 'scoring-result' listener
              ↓
React state updates → UI re-renders
```


No, SSE is not polling. Let me make the distinction precise.

---

**Polling vs SSE vs WebSocket:**

**Polling — client asks repeatedly:**
```
Client: "anything new?" → Server: "no"
Client: "anything new?" → Server: "no"
Client: "anything new?" → Server: "yes, here it is"
Client: "anything new?" → Server: "no"
```
Every 2-5 seconds, client makes a new HTTP request. Wasteful. Server gets hammered even when nothing is happening.

**SSE — server pushes when ready:**
```
Client: "I'm listening, keep this connection open"
Server: "......"  (silence, connection stays open)
Server: "EVENT: new scoring result"
Server: "......"  (silence)
Server: "EVENT: alert fired"
```
One HTTP connection stays open permanently. Server pushes only when something actually happens. Client just listens.

**WebSocket — bidirectional:**
```
Client ↔ Server (both can send at any time)
```
Full duplex. Overkill when you only need server → client push.

---

**Where SSE is used in real products:**

**GitHub** — the most visible example. When you push code and GitHub Actions starts running, you watch the build logs stream in real time in your browser. That's SSE. GitHub isn't polling every second — the server is pushing log lines as they're written.

**ChatGPT / Claude** — when you see the response streaming word by word as it's generated, that's SSE. The LLM generates tokens and the server pushes each token to your browser as it comes out. Without SSE you'd wait for the full response before seeing anything.

**Vercel deployment logs** — same pattern. You push to Git, Vercel builds, you watch the build output stream live in the dashboard. SSE.

**Twitter/X live feed** — when new tweets appear at the top without you refreshing. SSE (or long polling depending on implementation).

**Stock tickers** — live price updates on trading platforms. Server pushes price changes as they happen.

**Notion** — when someone else is editing a document you have open and you see their changes appear. SSE for presence and updates.

**Grafana** — dashboards that auto-refresh with new metrics without page reload.

---

**Why SSE and not WebSocket for these cases:**

ChatGPT doesn't need you to send data mid-stream. GitHub Actions logs only flow one way — server to browser. Our Live Monitor only needs server to push scoring results to the browser. The browser never needs to send data back on that connection.

WebSocket would be correct if the browser needed to send data back on the same connection — like a collaborative editor where both sides are typing simultaneously.

---

**The technical reason SSE works over regular HTTP:**

SSE uses `Content-Type: text/event-stream` which tells the browser "this response never ends — keep reading." The browser's `EventSource` API handles reconnection, event parsing, and buffering automatically. It's built into every browser, no library needed.

This is why Spring Boot can support it with just:
```java
produces = MediaType.TEXT_EVENT_STREAM_VALUE
```

No special protocol, no upgrade handshake like WebSocket needs. Just HTTP with a very long response.

Good question. Let me go deep on this properly.

---

**Start from the bottom — what is HTTP?**

HTTP is a request-response protocol. Client sends a request, server sends a response, connection closes. That's the basic contract.

```
Client → "GET /api/data HTTP/1.1"
Server → "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{...}"
         connection closes
```

Every piece of HTTP is text. The `\r\n` is a carriage return + newline. The blank line (`\r\n\r\n`) separates headers from body. That's literally all HTTP/1.1 is — text over a TCP socket.

---

**What is TCP?**

TCP is the layer below HTTP. It's a reliable byte stream between two machines. HTTP sits on top of TCP.

```
Your app → TCP → IP → Network → IP → TCP → Server app
```

TCP guarantees:
- Bytes arrive in order
- Lost packets are retransmitted
- Both sides know if the connection drops

HTTP uses TCP but adds structure — the request/response format, headers, status codes.

---

**Normal HTTP response anatomy:**

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 27
Connection: close

{"message": "hello world"}
```

Breaking this down:

`HTTP/1.1 200 OK` — protocol version, status code, status text

`Content-Type: application/json` — tells client how to interpret the body

`Content-Length: 27` — how many bytes to read. Client reads exactly 27 bytes then stops. Connection closes.

`Connection: close` — after this response, close the TCP connection.

The blank line separates headers from body. Everything after it is the body.

---

**How JSON API responses work in Spring Boot:**

When you write this:

```java
@GetMapping("/results")
public ResponseEntity<List<ScoringResult>> getAll() {
    return ResponseEntity.ok(scoringService.getAll());
}
```

Spring does this internally:
1. Calls `scoringService.getAll()` → gets a Java `List<ScoringResult>`
2. Jackson serializes it to a JSON string
3. Spring calculates `Content-Length`
4. Writes HTTP headers + body to the TCP socket
5. Closes connection

The client (browser/curl) reads until `Content-Length` bytes are consumed, then stops.

---

**The problem with polling:**

```javascript
// Polling — the naive approach
setInterval(async () => {
    const result = await fetch('/api/v1/scoring/results')
    const data = await result.json()
    setResults(data)
}, 2000) // every 2 seconds
```

What happens on the network:

```
T=0:    Client opens TCP connection
        Client sends GET /api/v1/scoring/results
        Server sends 200 OK + JSON body
        TCP connection closes

T=2000: Client opens NEW TCP connection
        Client sends GET /api/v1/scoring/results
        Server sends 200 OK + JSON body (same data, nothing changed)
        TCP connection closes

T=4000: Same thing again
        ...repeat forever
```

Problems:
- Opening TCP connection costs time (3-way handshake)
- Server processes every request even when nothing changed
- If something happens at T=1999, client won't know until T=2000
- 1000 users polling every 2 seconds = 500 requests/second just for "nothing changed"

---

**Long polling — the first improvement:**

```
Client: GET /api/v1/wait-for-event
Server: (holds the connection open, doesn't respond yet)
        (30 seconds pass...)
        (event happens)
Server: 200 OK + event data
        connection closes
Client: immediately opens new long poll connection
```

Better than polling — no wasted requests when nothing happens. But still opens a new connection after every event. Used by older real-time systems.

---

**SSE — how it actually works at the protocol level:**

SSE exploits a feature of HTTP that most people don't think about: **responses don't have to have a `Content-Length`.**

If you omit `Content-Length`, the client reads until the connection closes. This is called **chunked transfer encoding** or just an open-ended response.

The SSE response looks like this on the wire:

```http
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
X-Accel-Buffering: no

: connected\n\n

data: {"id":1,"sessionId":"test"}\n\n

data: {"id":2,"sessionId":"test2"}\n\n

event: turn-flag\n
data: {"turnIndex":7,"flagType":"HALLUCINATION"}\n\n
```

Key things:

`Content-Type: text/event-stream` — this single header is what makes it SSE. Browser's `EventSource` API looks for exactly this content type.

`Connection: keep-alive` — don't close the TCP connection.

`Cache-Control: no-cache` — don't cache this — it's a live stream.

`X-Accel-Buffering: no` — tells nginx not to buffer. Critical for real-time — without this nginx holds data until its buffer fills, which defeats the purpose.

The body never ends. Server keeps writing event chunks whenever it has data. Client keeps reading.

---

**SSE event format:**

Each event is plain text with a specific format:

```
data: your data here\n\n
```

Or with an event name:

```
event: scoring-result\n
data: {"id":1,"score":0.9}\n\n
```

The double `\n\n` marks the end of one event. Single `\n` continues the same event. This is the entire SSE "protocol" — it's just text formatting.

`id:` sets the last event ID (for reconnection)
`retry:` sets reconnection delay in milliseconds
`:` is a comment (used for heartbeat keep-alives)

---

**What Spring Boot does internally for SSE:**

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitters.add(emitter);
    return emitter;
}
```

When this method returns, Spring does NOT close the connection. It keeps the HTTP response open as a writable stream. The `SseEmitter` object is a handle to that open connection.

When you call:
```java
emitter.send(SseEmitter.event().name("scoring-result").data(json))
```

Spring writes this to the TCP socket immediately:
```
event: scoring-result\n
data: {"id":1,...}\n\n
```

The browser's `EventSource` receives it, parses it, and fires the matching event listener.

`Long.MAX_VALUE` as the timeout means the connection stays open essentially forever (292 years). Without this, Spring would close it after a default timeout.

---

**What `EventSource` does in the browser:**

```javascript
const es = new EventSource('/api/v1/monitor/stream')
```

The browser:
1. Opens a TCP connection to your server
2. Sends `GET /api/v1/monitor/stream` with `Accept: text/event-stream`
3. Receives the open-ended HTTP response
4. Reads the stream byte by byte
5. Parses SSE events as they arrive
6. Fires the corresponding event listeners
7. **Auto-reconnects** if connection drops — waits `retry` ms (default 3000) then reconnects

This is all built into the browser. No library needed.

---

**WebSocket comparison at protocol level:**

WebSocket is a completely different protocol. It starts as HTTP but then upgrades:

```
Client: GET /ws HTTP/1.1
        Upgrade: websocket
        Connection: Upgrade
        Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==

Server: HTTP/1.1 101 Switching Protocols
        Upgrade: websocket
        Connection: Upgrade
        Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

After `101 Switching Protocols`, the TCP connection is no longer HTTP. It becomes a custom binary framing protocol where both sides can send frames at any time. Completely bidirectional.

The overhead: custom protocol, handshake, frame parsing, masking (browser→server frames must be masked for security). More complex to implement, more to configure in nginx.

For our use case — server pushes to browser, browser never sends back on this connection — SSE is the right choice. WebSocket is solving a problem we don't have.

---

**HTTP/2 and SSE:**

HTTP/2 multiplexes multiple streams over one TCP connection. SSE works differently here — instead of one long HTTP/1.1 response, HTTP/2 uses server push streams. Spring Boot handles this transparently when HTTP/2 is configured.

HTTP/3 (QUIC) replaces TCP with UDP + reliability layer. SSE still works the same way from application code — Spring abstracts the transport.

---

**Why `CopyOnWriteArrayList` for emitters:**

```java
private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
```

Multiple threads call `push()` simultaneously — one per Kafka event, one per HTTP request. Normal `ArrayList` would throw `ConcurrentModificationException` if one thread is iterating the list while another is adding/removing.

`CopyOnWriteArrayList` solves this by making a fresh copy of the array on every write. Reads are lock-free and see a consistent snapshot. Slightly expensive on writes but our write frequency (connections/disconnections) is very low compared to read frequency (iterating to push events).

---

**The full flow one more time, at every layer:**

```
1. Browser calls new EventSource('/api/v1/monitor/stream')

2. Browser DNS resolves localhost → 127.0.0.1

3. Browser opens TCP socket to 127.0.0.1:8080
   (TCP 3-way handshake: SYN → SYN-ACK → ACK)

4. Browser sends over TCP:
   GET /api/v1/monitor/stream HTTP/1.1\r\n
   Host: localhost:8080\r\n
   Accept: text/event-stream\r\n
   \r\n

5. Spring receives request, routes to LiveMonitorController.stream()

6. Spring creates SseEmitter, adds to emitters list, returns it
   Spring writes response headers to TCP socket:
   HTTP/1.1 200 OK\r\n
   Content-Type: text/event-stream\r\n
   Cache-Control: no-cache\r\n
   \r\n
   (connection stays open — no Content-Length)

7. SseEmitterService sends heartbeat:
   : connected\r\n\r\n
   Browser fires 'connected' event listener

8. ...time passes...

9. Bot sends conversation → ScoringService.evaluate()
   → FlagEngine runs scorers concurrently
   → ScoringResult saved to PostgreSQL
   → processFlags() called
   → TurnFlags created
   → sseEmitterService.pushScoringResult(result, flags)

10. SseEmitterService iterates emitters list
    For each emitter calls:
    emitter.send(SseEmitter.event()
        .name("scoring-result")
        .data(json))

11. Spring writes to TCP socket:
    event: scoring-result\r\n
    data: {"id":21,"sessionId":"...",...}\r\n
    \r\n

12. Browser reads bytes from TCP socket
    EventSource parser sees double \n → complete event
    Fires: es.addEventListener('scoring-result', handler)

13. React handler called:
    setEvents(prev => [{ type: 'result', data, ts: new Date() }, ...prev])

14. React re-renders LiveMonitor component
    New result card appears in the feed
```

That's the complete journey from bot sending a message to pixels changing on screen.