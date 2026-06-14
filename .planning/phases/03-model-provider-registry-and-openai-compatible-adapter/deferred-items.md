## Deferred Items

- 2026-06-14 — While executing 03-02 Task 1, `mvn -q -pl pi-agent-domain -Dtest=ModelStreamingContractsTest,DomainDependencyArchTest test` also attempted to compile the untracked `ModelProviderRegistryContractsTest`, which belongs to Phase 03 Plan 01 provider registry work and references registry classes outside this plan's scope. This is tracked as a parallel-execution workspace contamination/blocking issue rather than a 03-02 code failure.
