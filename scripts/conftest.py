def pytest_addoption(parser):
    parser.addoption(
        "--release-version",
        default="0.0.0",
        help="Version to smoke-test, e.g. 0.1.2 (default: 0.0.0, tests will skip if not a real release)",
    )
    parser.addoption("--build-from-tag", action="store_true", help="Build the project from the release tag")
    parser.addoption("--gradle-e2e", action="store_true", help="Run Gradle plugin end-to-end test")
    parser.addoption("--sdk-e2e", action="store_true", help="Run SDK end-to-end test")
