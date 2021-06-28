plugins {
  id("otel.javaagent-instrumentation")
}

dependencies {
  api(project(":instrumentation:servlet:servlet-common:library"))
  implementation(project(":instrumentation:servlet:servlet-common:javaagent"))
  compileOnly("org.apache.tomcat.embed:tomcat-embed-core:7.0.4")
}
