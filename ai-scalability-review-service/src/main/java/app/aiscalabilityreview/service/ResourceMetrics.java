package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricsCollector.Run;

import static app.aiscalabilityreview.service.DatadogMetricsCollector.DAY;
import static app.aiscalabilityreview.service.MetricsDocument.num;

/** Section 4: CPU, memory and replica count of the service's pods. */
final class ResourceMetrics {
    private static final double NANOCORES = 1_000_000_000.0;
    private static final double MIB = 1024.0 * 1024;

    private final Run run;
    private final MetricsDocument doc;

    ResourceMetrics(Run run) {
        this.run = run;
        this.doc = run.doc;
    }

    void collect(String tags, String service, String env) {
        doc.heading("4. Resource Saturation");
        String cpuAvg = "avg:kubernetes.cpu.usage.total{" + tags + "}.rollup(avg, " + DAY + ")";
        String cpuMax = "max:kubernetes.cpu.usage.total{" + tags + "}.rollup(max, " + DAY + ")";
        MetricSeries avg = run.single("CPU Usage - Average", cpuAvg);
        if (avg != null) {
            doc.bullet("CPU Usage - Average", num(avg.mean() / NANOCORES, 4) + " cores (" + Math.round(avg.mean()) + " nanocores, per pod)", cpuAvg);
            doc.put("cpu_avg_cores", avg.mean() / NANOCORES, 4);
        }
        MetricSeries peak = run.single("CPU Usage - Peak", cpuMax);
        if (peak != null) {
            doc.bullet("CPU Usage - Peak", num(peak.peak() / NANOCORES, 4) + " cores (" + Math.round(peak.peak()) + " nanocores, per pod) on " + peak.dayAt(peak.peakIndex()), cpuMax);
            doc.put("cpu_peak_cores", peak.peak() / NANOCORES, 4);
        }
        String memAvg = "avg:kubernetes.memory.working_set{" + tags + "}.rollup(avg, " + DAY + ")";
        String memMax = "max:kubernetes.memory.working_set{" + tags + "}.rollup(max, " + DAY + ")";
        MetricSeries memoryAvg = run.single("Memory Working Set - Average", memAvg);
        if (memoryAvg != null) {
            doc.bullet("Memory Working Set - Average", num(memoryAvg.mean() / MIB, 2) + " MiB (" + Math.round(memoryAvg.mean()) + " bytes)", memAvg);
            doc.put("mem_avg_mib", memoryAvg.mean() / MIB, 2);
        }
        MetricSeries memoryPeak = run.single("Memory Working Set - Peak", memMax);
        if (memoryPeak != null) {
            doc.bullet("Memory Working Set - Peak", num(memoryPeak.peak() / MIB, 2) + " MiB (" + Math.round(memoryPeak.peak()) + " bytes) on " + memoryPeak.dayAt(memoryPeak.peakIndex()), memMax);
            doc.put("mem_peak_mib", memoryPeak.peak() / MIB, 2);
        }
        replicas(service, env);
    }

    private void replicas(String service, String env) {
        String tags = "env:" + env + ",kube_deployment:" + service;
        String replicasAvg = "avg:kubernetes_state.deployment.replicas{" + tags + "}.rollup(avg, " + DAY + ")";
        String replicasMax = "max:kubernetes_state.deployment.replicas{" + tags + "}.rollup(max, " + DAY + ")";
        MetricSeries avg = run.single("Replicas - Average", replicasAvg);
        if (avg != null) {
            doc.bullet("Replicas - Average", num(avg.mean(), 2) + " replicas", replicasAvg);
            doc.put("replicas_avg", avg.mean(), 2);
        }
        MetricSeries max = run.single("Replicas - Max", replicasMax);
        if (max != null) {
            doc.bullet("Replicas - Max", Math.round(max.peak()) + " replicas (on " + max.dayAt(max.peakIndex()) + ")", replicasMax);
            doc.put("replicas_max", max.peak(), 0);
        }
    }
}
