package io.scalecube.trace.service.reporter.latency;

import io.scalecube.trace.service.reporter.Reporter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.agrona.CloseHelper;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class LatencyReporter extends Reporter {

  private final Recorder histogram;

  private Histogram accumulatedHistogram;

  private final LatencyListener listener;

  /**
   * Launch this test reporter.
   *
   * @param listeners throughput listeners
   * @return a reporter
   */
  public static LatencyReporter create(LatencyListener... listeners) {
    return new LatencyReporter(new CompositeReportingLatencyListener(listeners));
  }

  /** start latency reporter. */
  @SuppressWarnings("unchecked")
  public LatencyReporter start() {
    reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    Duration reportInterval = Duration.ofSeconds(Long.getLong("benchmark.report.interval", 1));
    this.disposable =
        Flux.interval(reportDelay, reportInterval, Schedulers.single())
            .doOnCancel(this::onTerminate)
            .subscribe(i -> this.run(), Throwable::printStackTrace);
    return this;
  }

  private LatencyReporter(LatencyListener listener) {
    this.listener = listener;
    this.histogram = new Recorder(TimeUnit.SECONDS.toNanos(10), 3);
  }

  private void run() {
    if (warmupFinished) {
      Histogram intervalHistogram = histogram.getIntervalHistogram();
      if (accumulatedHistogram != null) {
        accumulatedHistogram.add(intervalHistogram);
      } else {
        accumulatedHistogram = intervalHistogram;
      }

      listener.onReport(intervalHistogram);
    } else {
      warmupFinished = true;
      histogram.reset();
    }
  }

  private void onTerminate() {
    listener.onTerminate(accumulatedHistogram);
  }

  public void onDiff(long diff) {
    histogram.recordValue(diff);
  }

  @Override
  public void close() {
    disposable.dispose();
    histogram.reset();
    CloseHelper.quietClose(listener);
  }
}
