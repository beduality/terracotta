package io.github.beduality.terracotta.gradle

abstract class TerracottaExtension {
    abstract var projectId: String
    abstract var name: String
    abstract var summary: String
    abstract var description: String
    abstract var tags: List<String>
    abstract var license: String
    abstract var gameVersions: List<String>
    abstract var loaders: List<String>
    abstract var environment: String
    abstract var provider: String
    abstract var token: String?
    abstract var artifactFile: Any?
}
