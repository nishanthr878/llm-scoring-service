# LLM Scoring Service

Evaluation and observability platform for AI agents and bots.

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)

## What it does

LLM Scoring Service evaluates AI agent conversations in real time — catching hallucinations, policy violations, PII leaks, and inconsistencies before they reach your users.

- **Score conversations** — faithfulness, relevance, hallucination, consistency
- **Scenario testing** — define policy rules and expected behaviors, evaluate bots against them
- **PII detection** — detect and classify user-sent, bot-echoed, and bot-originated PII
- **Real-time monitoring** — Kafka-powered live evaluation with SSE push to dashboard
- **Turn-level flagging** — pinpoint exactly which turn a bot started hallucinating
- **Pluggable scorers** — add custom scorers with zero framework changes

## Architecture
## Quick Start

**Prerequisites:** Docker, Docker Compose

```bash
git clone https://github.com/nishanthr878/llm-scoring-service
cd llm-scoring-service

# Add your Groq API key
echo "GROQ_API_KEY=your_key_here" > .env

# Start everything
docker compose up -d

# Verify
curl http://localhost:8080/actuator/health
```

Get a free Groq API key at [console.groq.com](https://console.groq.com)

## Usage

### Evaluate a single Q&A

```bash
curl -X POST http://localhost:8080/api/v1/scoring/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the return policy?",
    "retrievedChunks": ["Returns accepted within 30 days of purchase."],
    "answer": "You can return items within 30 days.",
    "sessionId": "session-001",
    "modelName": "gpt-4"
  }'
```

### Create a scenario

```bash
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -d '{
    "name": "return-agent",
    "policy": [
      "Always collect order ID before processing return",
      "Refund timeline is 5-7 business days",
      "Never promise same-day refunds"
    ],
    "expectedBehaviors": [
      "Bot collected order ID from user",
      "Bot mentioned refund timeline"
    ],
    "scorerNames": ["policyCompliance", "expectedBehavior", "turnHallucination", "piiDetection"],
    "alertThreshold": 0.7
  }'
```

### Evaluate against a scenario

```bash
curl -X POST http://localhost:8080/api/v1/scoring/evaluate/scenario/return-agent \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "I want to return my order"},
      {"role": "assistant", "content": "Sure, can you provide your order ID?"}
    ],
    "sessionId": "session-001",
    "modelName": "gpt-4"
  }'
```

### Real-time monitoring via ingest

```bash
curl -X POST http://localhost:8080/api/v1/events/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-001",
    "scenarioName": "return-agent",
    "modelName": "gpt-4",
    "format": "openai",
    "messages": [...]
  }'
```

## Supported Message Formats

| Format | Description |
|--------|-------------|
| `openai` | `{"role": "user", "content": "..."}` — default |
| `human-ai` | `{"speaker": "human", "text": "..."}` |
| `field-mapping` | Any format via custom field mapping config |

## Available Scorers

| Scorer | What it checks | Type |
|--------|---------------|------|
| `faithfulness` | Is answer grounded in context? | Single turn |
| `answerRelevance` | Does answer address the question? | Single turn |
| `hallucination` | Did bot invent facts? | Single turn |
| `consistency` | Did bot contradict itself? | Conversation |
| `contextRetention` | Did bot remember earlier turns? | Conversation |
| `conversationFaithfulness` | Grounded across all turns? | Conversation |
| `policyCompliance` | Did bot follow policy rules? | Scenario |
| `expectedBehavior` | Did bot exhibit expected behaviors? | Scenario |
| `turnHallucination` | Which specific turn hallucinated? | Scenario |
| `piiDetection` | PII detection with scenario classification | All |
| `customPattern` | User-defined regex/keyword matching | All |

## PII Detection

Detects and classifies PII into three scenarios:

| Scenario | Example | Severity |
|----------|---------|----------|
| `USER_SENT` | User sends their email | MEDIUM |
| `BOT_ECHOED` | Bot repeats user's email back | HIGH |
| `BOT_ORIGINATED` | Bot produces a credit card number | CRITICAL |

Supports: EMAIL, PHONE, CREDIT_CARD, SSN, API_KEY, PASSWORD

## SDKs

**Java SDK**
```xml
<dependency>
  <groupId>com.llmscoring</groupId>
  <artifactId>llm-scoring-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
LLMScoring scoring = LLMScoring.create(
    LLMScoringConfig.builder()
        .scoringUrl("http://localhost:8080")
        .defaultScenario("return-agent")
        .mode(TrackingMode.SLIDING_WINDOW)
        .build()
);

ConversationTracker tracker = scoring.session(sessionId);
tracker.track(userMessage, botResponse); // non-blocking
```

**Python SDK**
```bash
pip install llm-scoring-sdk
```

```python
from llm_scoring import LLMScoring, LLMScoringConfig

scoring = LLMScoring.create(LLMScoringConfig(
    scoring_url="http://localhost:8080",
    default_scenario="return-agent"
))

tracker = scoring.session(session_id)
tracker.track(user_message, bot_response)
```

**LangChain Integration**
```python
from llm_scoring.langchain_callback import LLMScoringCallback

callback = LLMScoringCallback(
    scoring_url="http://localhost:8080",
    scenario_name="return-agent"
)

llm = ChatOpenAI(callbacks=[callback])
# Every call auto-tracked — no other code change
```

## API Reference

### Scoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/scoring/evaluate` | Single turn evaluation |
| POST | `/api/v1/scoring/evaluate/conversation` | Conversation evaluation |
| POST | `/api/v1/scoring/evaluate/scenario/{name}` | Scenario evaluation |
| GET | `/api/v1/scoring/results` | All results |
| GET | `/api/v1/scoring/results/failed` | Failed results only |

### Scenarios
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/scenarios` | Create scenario |
| GET | `/api/v1/scenarios` | List all scenarios |
| GET | `/api/v1/scenarios/scorers/available` | Available scorers |

### Ingest
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/events/ingest` | Ingest conversation (async) |
| GET | `/api/v1/events/formats` | Available message formats |

### Stats
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/stats/overview` | Pass rate, total counts |
| GET | `/api/v1/stats/by-model` | Stats grouped by model |
| GET | `/api/v1/stats/trends` | Daily pass rate trends |
| GET | `/api/v1/stats/failures` | Top failure reasons |

### Monitoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/monitor/stream` | SSE stream (real-time events) |
| GET | `/api/v1/monitor/alerts` | Active alerts |
| POST | `/api/v1/monitor/alerts/{id}/dismiss` | Dismiss alert |
| GET | `/api/v1/monitor/flags` | Active turn flags |

### Conversations
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/conversations` | All conversations |
| GET | `/api/v1/conversations/{id}/detail` | Full detail with flags |

## Adding a Custom Scorer

```java
@Component
public class ToxicityScorer extends BaseScorer implements Scorer {

    private final ChatClient chatClient;

    @Override
    public String name() { return "toxicity"; }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        // your scoring logic
    }
}
```

That's it. Spring auto-discovers it. Add `"toxicity"` to any scenario's `scorerNames`.

## Stack

- Java 21 + Spring Boot 3.5
- PostgreSQL + JSONB
- Apache Kafka (KRaft — no Zookeeper)
- Spring AI (Groq / OpenAI compatible)
- React + Vite (UI)

## UI

```bash
git clone https://github.com/nishanthr878/llm-scoring-service-ui
cd llm-scoring-service-ui
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

## Related Repos

- [UI](https://github.com/nishanthr878/llm-scoring-service-ui)
- [Java SDK](https://github.com/nishanthr878/llm-scoring-service-java-sdk)
- [Python SDK](https://github.com/nishanthr878/llm-scoring-service-python-sdk)


