# Deferred Items

## Out-of-scope untracked Plan 02 artifacts during Plan 01-03

- **Found during:** Plan 01-03 Task 1/Task 3 verification
- **Issue:** The working tree contained untracked Plan 02 domain/test files such as `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/agent/AgentDefinitionTest.java` and untracked runtime/event/error/tool/message main packages. These are outside Plan 01-03 ownership and caused `mvn -q -pl pi-agent-domain test` to fail at test compilation before Plan 01-03 tests could run.
- **Action:** Did not modify or stage those files to avoid cross-agent/parallel execution conflicts. Plan 01-03 changes were verified with domain main compilation; full Maven test verification remains blocked until Plan 02 files are completed or removed by their owning executor.
