# Diátaxis Documentation Framework — Complete Guide (All Four Types)

Diátaxis organizes documentation into four distinct types based on user intent:

* **Tutorials** → learning-oriented (“I want to learn”)
* **How-to guides** → task-oriented (“I want to do”)
* **Reference** → information-oriented (“I need to know”)
* **Explanation** → understanding-oriented (“I want to understand”)

Each type has a strict purpose and should not mix with the others.

---

# 1. Tutorials

## Purpose

Tutorials are designed to **teach by doing**. They guide a beginner through a complete, safe, and successful first experience.

They answer:

> “I want to learn how this works by building something.”

---

## Characteristics

* Learning-focused
* Step-by-step execution
* Controlled, ideal environment
* Single clear outcome
* No decisions or alternatives
* No prior knowledge assumed

---

## Structure

### 1. Title

Must describe the outcome:

* “Build a minimal REST API in Node.js”

---

### 2. Introduction

* What will be built
* Minimal prerequisites

No theory.

---

### 3. Steps

* Sequential
* Small and executable
* Each step produces visible progress

Example pattern:

* Step 1: do X
* Step 2: do Y
* Step 3: do Z

---

### 4. Final result

* What now works
* How to verify success

---

### 5. Next steps (optional)

* Links to how-to guides or reference docs

---

## Writing principles

* Learning by doing
* One outcome only
* No branching paths
* No conceptual explanations
* Immediate feedback at each step

---

## Anti-patterns

* Mixing in reference material
* Multiple goals in one tutorial
* Real-world complexity
* Optional steps everywhere
* Theory-heavy content

---

## Mental model

> “Follow these steps and succeed once in a controlled environment.”

---

# 2. How-to Guides

## Purpose

How-to guides solve **real-world tasks**.

They answer:

> “I want to do X.”

---

## Characteristics

* Problem-oriented
* Real environment assumptions
* Assumes basic familiarity
* Flexible solutions
* Single task focus
* Outcome-driven

---

## Structure

### 1. Title

Must express task:

* “Configure HTTPS for a reverse proxy”

---

### 2. Problem statement

* What the user is trying to achieve

---

### 3. Preconditions

* Required tools
* System state
* Permissions

Declarative only.

---

### 4. Steps

* Actionable instructions
* May include conditional branches
* Focus on completion, not teaching

---

### 5. Outcome / verification

* How to confirm success

---

### 6. Variants (optional)

* Alternative approaches
* Different environments

---

## Writing principles

* Solve a specific problem
* One default path
* Minimal necessary context
* Real-world assumptions
* Action-focused language

---

## Anti-patterns

* Teaching fundamentals (tutorial drift)
* Exhaustive API listing (reference drift)
* Deep conceptual explanation (explanation drift)
* Over-branching
* Missing success criteria

---

## Mental model

> “A recipe for solving a specific problem in a real system.”

---

# 3. Reference

## Purpose

Reference material provides **precise, complete, and structured information** for lookup.

It answers:

> “What are the exact details of this system?”

---

## Characteristics

* Exhaustive within scope
* Highly structured
* Information-dense
* Neutral and objective
* No narrative flow
* Designed for quick lookup

---

## Structure

### 1. Definitions

Exact descriptions of entities:

* functions
* APIs
* commands
* components

---

### 2. Parameter tables

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |

---

### 3. Signatures

Example:

* `createUser(input: UserInput): User`

---

### 4. Enumerations

* active
* pending
* suspended

---

### 5. Error catalog

| Code | Meaning | Condition |
| ---- | ------- | --------- |

---

## Writing principles

* Precision over readability
* Exhaustive coverage
* No procedural flow
* No conceptual explanation
* Canonical naming only

---

## Anti-patterns

* Instructional steps
* Conceptual essays
* Partial coverage
* Opinionated guidance
* Ambiguous naming

---

## Mental model

> “A precise map of a system you already know you need to use.”

---

# 4. Explanation

## Purpose

Explanation builds **understanding of concepts and systems**.

It answers:

> “Why does this exist and how does it work?”

---

## Characteristics

* Concept-focused
* Reasoning-driven
* Contextual and interpretive
* Not exhaustive
* Open-ended
* Multi-perspective

---

## Structure (flexible)

### 1. Concept introduction

* What the concept is
* Where it fits

---

### 2. Underlying model

* How it works internally
* Relationships and structure

---

### 3. Rationale

* Why it exists
* What problem it solves

---

### 4. Trade-offs

* Alternatives
* Limitations
* Design constraints

---

### 5. Conceptual connections

* Related ideas
* Broader systems

---

## Writing principles

* Focus on understanding
* Explain “why” and “how”
* Use reasoning and relationships
* Selective detail (not exhaustive)
* No procedural instructions

---

## Anti-patterns

* Step-by-step instructions (tutorial drift)
* Exhaustive listings (reference drift)
* Surface-level definitions
* Pure opinion or advice
* Over-simplification

---

## Mental model

> “A map of ideas and relationships that explains why the system is the way it is.”

---

# 5. Comparison Overview

| Type        | Question it answers   | Focus              | Structure            | Completeness              |
| ----------- | --------------------- | ------------------ | -------------------- | ------------------------- |
| Tutorial    | How do I learn this?  | Learning by doing  | Strict step-by-step  | Low scope, high guidance  |
| How-to      | How do I do X?        | Task completion    | Flexible steps       | Medium, task-focused      |
| Reference   | What are the details? | Information lookup | Structured data      | Exhaustive within scope   |
| Explanation | Why/how does it work? | Understanding      | Conceptual narrative | Selective, not exhaustive |

---

# 6. Key Separation Rules

* Tutorials teach, but do not solve real-world problems
* How-to guides solve problems, but do not teach fundamentals
* Reference documents data, but does not instruct or explain
* Explanation clarifies understanding, but does not instruct or enumerate
