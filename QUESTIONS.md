# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly.
What are your thoughts on the pros and cons of each approach? Which would you choose and why?

## Answer:
    OpenAPI approach means we write the API contract first, then generate code from it.
    Hand-coded approach means we write the endpoint code directly ourselves.
OpenAPI pros
============
    clear API contract
    good documentation
    good when many teams use the API
    less chance of API mismatch
    
OpenAPI cons
============
    more setup
    slower for small changes
    generated code can feel harder to work with

Hand-coded pros
==============
    faster to build
    easier for small/simple APIs
    more flexible

Hand-coded cons
===============
    docs can go out of date
    different APIs may become inconsistent

Which one would I choose?
========================
    For important/shared APIs like Warehouse, I would prefer OpenAPI
    For simple/internal APIs like Product and Store, hand-coded is fine
Use OpenAPI when contract and consistency matter most, use hand-coded when speed and simplicity matter most.


## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**

I would prioritize tests by risk and by the kinds of bugs this system is most likely to suffer from: 
business-rule regressions, transaction-boundary mistakes, and concurrent update issues.

First, I would keep a strong set of fast domain/use-case tests around warehouse creation, replacement, archiving, and validation rules. 
Those are the cheapest tests to run and they protect the core business logic directly.
Parameterized tests are especially valuable here because they let us cover many invalid input combinations without a lot of duplicated test code.

Second, I would invest in a smaller number of targeted integration tests for the high-risk seams:
- persistence behavior such as unique constraints, rollbacks, and optimistic locking
- transaction-sensitive workflows such as "notify the legacy system only after commit"
- endpoint-level happy paths and key error paths

Third, I would keep concurrency tests, but only for the flows where race conditions would cause real production damage.
In this project that clearly includes warehouse updates and duplicate warehouse creation,
because optimistic locking and uniqueness are part of the correctness model.

Over time, I would aim for a test pyramid like this:
- many fast unit/use-case tests for business rules
- a focused middle layer of repository/resource integration tests
- a very small number of expensive concurrency or end-to-end tests for the highest-risk scenarios

To keep coverage effective, I would:
- add tests whenever we fix a bug or discover a missing edge case
- keep integration tests focused on important invariants rather than broad duplication of unit tests
- run the fast suite on every change and the heavier concurrency/integration checks in CI
- periodically prune low-signal tests so the suite stays fast and trustworthy

That balance gives good protection without turning the test suite into something so slow or brittle that the team stops relying on it.

