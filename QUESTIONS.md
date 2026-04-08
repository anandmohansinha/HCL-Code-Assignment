# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
OpenAPI-first and hand-coded endpoints both have a place; the right choice depends on how much contract stability and cross-team coordination you need.

For the Warehouse API, generating the interface from OpenAPI is a good fit because it makes the contract explicit, keeps documentation and code aligned, and reduces drift between what clients expect and what the service exposes. It is especially helpful when multiple teams consume the API, when backward compatibility matters, or when you want client/server generation and review of API changes to happen at the spec level. The tradeoff is that it adds build complexity, generated code can feel less natural to work with, and simple endpoint changes may require touching the spec, regenerating code, and understanding the generator's conventions.

For Product and Store, hand-coded endpoints are faster to iterate on and usually easier to read when the API surface is small and mostly internal. They give the team freedom to shape the resource methods directly around the framework and the domain model. The downside is that documentation can drift, contract consistency is easier to lose over time, and different developers may make different choices around naming, status codes, validation, and error shape.

My default choice would be:
- OpenAPI-first for externally consumed or business-critical APIs where contract clarity, reviewability, and consumer trust matter.
- Hand-coded resources for small internal APIs or early-stage features while the shape is still changing quickly.

In this codebase, I would keep Warehouse spec-first because it already behaves like a more formal contract. For Product and Store, I would either keep them hand-coded for now or move them to OpenAPI only if they are becoming shared/public interfaces and the team wants one consistent API governance model across the service.
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would prioritize tests by risk and by the kinds of bugs this system is most likely to suffer from: business-rule regressions, transaction-boundary mistakes, and concurrent update issues.

First, I would keep a strong set of fast domain/use-case tests around warehouse creation, replacement, archiving, and validation rules. Those are the cheapest tests to run and they protect the core business logic directly. Parameterized tests are especially valuable here because they let us cover many invalid input combinations without a lot of duplicated test code.

Second, I would invest in a smaller number of targeted integration tests for the high-risk seams:
- persistence behavior such as unique constraints, rollbacks, and optimistic locking
- transaction-sensitive workflows such as "notify the legacy system only after commit"
- endpoint-level happy paths and key error paths

Third, I would keep concurrency tests, but only for the flows where race conditions would cause real production damage. In this project that clearly includes warehouse updates and duplicate warehouse creation, because optimistic locking and uniqueness are part of the correctness model.

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
```
