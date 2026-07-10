# Writing Documentation

This tutorial teaches how to write documentation for Terracotta following the Diátaxis framework.

## What is Diátaxis?

Diátaxis is a documentation system that organizes content into four distinct types based on user intent:

| Type | Question it Answers | Purpose |
|------|---------------------|---------|
| **Tutorial** | "I want to learn" | Learning by doing |
| **How-to Guide** | "I want to do X" | Solving specific tasks |
| **Reference** | "I need to know" | Information lookup |
| **Explanation** | "I want to understand" | Conceptual understanding |

## Diátaxis Rules

1. **Each page belongs to exactly one type**
2. **Pages must match their type's purpose**
3. **Don't mix types in a single page**

## Choosing the Right Type

### Use a Tutorial When

- The user is a beginner
- You're teaching by doing
- There's a single clear outcome
- No prior knowledge assumed

**Tutorial Title Format**: "Learn how to X by doing Y"

Example: "Navigating the Codebase"

### Use a How-to Guide When

- The user wants to accomplish a specific task
- The task is real-world, not idealized
- Basic familiarity is assumed
- There's one main goal

**How-to Title Format**: "How to X"

Example: "Building the Project"

### Use Reference When

- The user needs precise, complete information
- The content is for lookup, not learning
- Information is structured and exhaustive
- No narrative flow

**Reference Title Format**: "X Reference" or "X Guide"

Example: "Tech Stack Reference"

### Use Explanation When

- The user wants to understand "why" and "how"
- You're explaining concepts and relationships
- The focus is on reasoning, not steps
- Multiple perspectives are valuable

**Explanation Title Format**: "Why X" or "How X Works"

Example: "Project Management"

## Writing Each Type

### Tutorials

**Structure**:

1. Title: "Learn how to X by doing Y"
2. Introduction: What you'll build, prerequisites
3. Steps: Sequential, small and executable
4. Final result: What now works
5. Next steps: Links to related content

**Rules**:

- One outcome only
- No branching paths
- No theory-heavy content
- Immediate feedback at each step

### How-to Guides

**Structure**:

1. Title: "How to X"
2. Problem statement: What you're trying to achieve
3. Preconditions: Required tools, system state
4. Steps: Actionable instructions
5. Outcome: How to confirm success
6. Variants: Alternative approaches (optional)

**Rules**:

- One specific task
- Real-world assumptions
- Focus on completion, not teaching
- Minimal necessary context

### Reference

**Structure**:

1. Title: "X Reference"
2. Definitions: Exact descriptions
3. Parameters: Tables with type, required, default, description
4. Signatures: Function signatures
5. Enumerations: All possible values
6. Errors: Error catalog

**Rules**:

- Exhaustive within scope
- Precision over readability
- No procedural flow
- No conceptual explanation

### Explanation

**Structure** (flexible):

1. Concept introduction: What it is, where it fits
2. Underlying model: How it works internally
3. Rationale: Why it exists
4. Trade-offs: Alternatives, limitations
5. Conceptual connections: Related ideas

**Rules**:

- Focus on understanding
- Explain "why" and "how"
- Use reasoning and relationships
- Selective detail, not exhaustive
- No procedural instructions

## Documentation Checklist

Before publishing:

- [ ] Page belongs to exactly one Diátaxis type
- [ ] Title reflects the type (command for how-to, concept for explanation)
- [ ] Content matches the purpose of that type
- [ ] No mixing of types in a single page
- [ ] Code examples are tested and working
- [ ] Links point to correct targets
- [ ] No tutorial drift in reference/explanation
- [ ] No reference drift in how-to/explanation

## Examples

### Correct: How-to Guide
```
# Building the Project

## Prerequisites
- JDK 21

## Build Commands
### Compile All Modules
```bash
./gradlew build
```
```

### Incorrect: How-to with Tutorial Drift
```
# Building the Project

## Prerequisites
- JDK 21

## What is Gradle?
Gradle is a build tool...

## Build Commands
### Compile All Modules
```bash
./gradlew build
```
```

The "What is Gradle?" section is tutorial drift - it teaches fundamentals rather than solving a task.

### Correct: Reference
```
# Tech Stack

## Kotlin / JVM

| Concern | Tool | Version |
|---------|------|---------|
| Language | Kotlin | 2.3.21 |
| JVM Target | JVM 17 | - |
| Compilation | JDK | 21 |
```

### Incorrect: Reference with Tutorial Drift
```
# Tech Stack

## Kotlin / JVM

Kotlin is a programming language...

| Concern | Tool | Version |
|---------|------|---------|
| Language | Kotlin | 2.3.21 |
```

The explanation of Kotlin is tutorial drift - reference should be concise and information-focused.

## Common Mistakes

### 1. Tutorial Drift in How-to

**Bad**: Explaining concepts in a how-to guide

**Good**: Focus on steps to accomplish the task

### 2. Reference Drift in Explanation

**Bad**: Exhaustive API listing in an explanation

**Good**: Selective detail focused on understanding

### 3. Mixing Types

**Bad**: A page that teaches AND lists API

**Good**: Separate tutorial + reference pages

## Getting Help

- Check existing pages for examples of each type
- Ask in the Discord server if unsure
