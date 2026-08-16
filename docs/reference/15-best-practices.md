# Chapter 15 — Best Practices

## Why Google Chose This Architecture

| Goal | Solution |
|------|----------|
| Follow official guidance | Three-layer architecture (UI, Domain, Data) |
| Multi-developer collaboration | Feature modularization with clear ownership |
| Fast builds | Convention plugins + granular modules |
| Testability | Interface-based design with fake implementations |
| Offline support | Offline-first with Room as source of truth |
| Scalability | Feature `api/impl` split prevents coupling |
| Consistency | Convention plugins standardize configuration |
| Maintainability | Unidirectional data flow, immutable state |

---

## Architectural Principles Summarized

### 1. Single Source of Truth
Every piece of data has one owner. Room is the source of truth for content, DataStore for preferences. No duplicated state.

### 2. Unidirectional Data Flow
Events flow down (User taps button to ViewModel method call to Repository). Data flows up (Room query to Repository Flow to ViewModel StateFlow to Composable).

### 3. Dependency Inversion
Upper layers depend on abstractions (interfaces), not implementations. `ForYouViewModel` depends on `UserNewsResourceRepository` (interface), not `CompositeUserNewsResourceRepository` (implementation).

### 4. Offline-First
Network is a sync mechanism, not a data source. Users see content immediately from local cache. Background sync keeps data fresh.

### 5. Feature Isolation
Features communicate through navigation keys only. No feature has access to another feature's ViewModel or internal state.

---

## How to Reuse This Architecture

### For a New Project

1. **Copy `build-logic/`** — Convention plugins work for any Android project
2. **Set up `core:model`** — Define your domain models as a JVM library
3. **Set up `core:data`** — Define repository interfaces and implementations
4. **Set up `core:database`** and `core:network` — Separate data sources
5. **Create your first feature** — Follow the `api/impl` pattern from Chapter 9
6. **Wire in the app module** — Add navigation, scaffold, and top-level destinations

### Adapting for Your Team

| Team Size | Recommendation |
|-----------|---------------|
| 1-3 devs | Can simplify — skip `api/impl` split if only a few features |
| 4-10 devs | Use the full `api/impl` pattern — module boundaries prevent conflicts |
| 10+ devs | Consider even more granular modules (split `core:data` by domain) |

---

## Common Mistakes and How to Fix Them

### Architecture Mistakes

| Mistake | Problem | Fix |
|---------|---------|-----|
| Business logic in Composables | Untestable, violates UDF | Move to ViewModel or Use Case |
| Network calls in ViewModel | No offline support, tightly coupled | Call Repository instead |
| Mutable UI state | Race conditions, unpredictable UI | Use immutable data classes |
| ViewModel knows about navigation | Tight coupling | Pass navigation as lambdas |
| Feature A imports Feature B's impl | Compilation coupling | Depend on Feature B's api module |

### Module Mistakes

| Mistake | Problem | Fix |
|---------|---------|-----|
| Shared code in feature module | Other features can't access it | Move to appropriate core module |
| Using `api` for all dependencies | Leaks transitive deps, slower builds | Default to `implementation` |
| Circular module dependencies | Build fails | Restructure — extract shared code to core |
| Manual Gradle config | Inconsistent, error-prone | Use convention plugins |

### Testing Mistakes

| Mistake | Problem | Fix |
|---------|---------|-----|
| Using Mockito for repositories | Doesn't catch interface changes | Use fake implementations |
| Testing stateful composable | ViewModel coupling in tests | Test stateless variant |
| Forgetting TestDispatcherRule | viewModelScope doesn't work | Add `@get:Rule` |
| Testing implementation details | Brittle tests | Test behavior, not implementation |

---

## Scaling Advice

### When Your App Grows

1. **Split `core:data` by domain** — `core:data-users`, `core:data-products` when you have 10+ repositories
2. **Add more feature modules** — One per user journey
3. **Consider dynamic feature modules** — For on-demand delivery of rarely used features
4. **Add a `core:common-ui`** — For UI utilities that don't depend on domain models
5. **Automate module creation** — Scripts that generate the `api/impl` scaffold

### Performance Considerations

| Area | Approach |
|------|----------|
| Build speed | More modules (parallel builds) + convention plugins |
| App startup | SplashScreen API + lazy initialization |
| Sync | Change-list based sync (incremental) |
| UI performance | Compose stability, `@Stable`, `@Immutable` annotations |
| Memory | Unscoped repositories, `WhileSubscribed` for Flows |

---

## Checklist for Code Reviews

- [ ] Is the UI state modeled as a sealed hierarchy?
- [ ] Does the ViewModel expose `StateFlow` (not `MutableState`)?
- [ ] Are events passed as lambdas (not stored in ViewModel)?
- [ ] Does the feature use `api/impl` split?
- [ ] Are new dependencies added to version catalog?
- [ ] Is the module using a convention plugin?
- [ ] Are tests using fakes (not mocks)?
- [ ] Is the NavKey `@Serializable`?
- [ ] Does the entry provider use `navigator` parameter for navigation?
- [ ] Is the repository reading from local storage (not network) for display?

---

## Summary

NiA's architecture works because it follows clear principles: single source of truth, unidirectional data flow, dependency inversion, offline-first, and feature isolation. These principles scale from solo developers to large teams.

## Key Takeaways

- Architecture is about principles, not just patterns
- Module boundaries enforce discipline that code reviews alone cannot
- Offline-first is not just for offline scenarios — it makes the app faster for everyone
- Convention plugins are essential infrastructure, not optional tooling
- Testing with fakes is superior to mocking for this architecture

## Final Thought

> This architecture is not the only way to build Android apps. It is **one well-proven approach** that Google uses to demonstrate its own guidance. Adapt it to your needs, but understand the reasoning behind each decision before changing it.
