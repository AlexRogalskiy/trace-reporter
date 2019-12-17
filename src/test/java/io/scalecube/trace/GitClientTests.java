package io.scalecube.trace;

import io.scalecube.trace.git.GitClient;
import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

class GitClientTests {

  @Test
  void testFlow() throws InvalidRemoteException, TransportException, IOException, GitAPIException {
    String slug = System.getProperty("slug", "");
    if (slug.trim().isEmpty()) {
      throw new TestAbortedException("missing slug configuration in the test");
    }
    GitClient git = GitClient.cloneRepo("git@github.com:" + slug + ".git");
    try (TraceReporter reporter = new TraceReporter()) {
      reporter.addY("ABC","A", 72);
      reporter.addY("ABC","A", 63);
      reporter.addY("ABC","A", 45);
      reporter.addY("ABC","A", 52);
      reporter.addY("XYZ","A", 161);
      reporter.addY("XYZ","A", 151);
      reporter.addY("XYZ","A", 143);

      reporter.createChart(git, "template.json", "test-with-data").block();
    }
  }
}
