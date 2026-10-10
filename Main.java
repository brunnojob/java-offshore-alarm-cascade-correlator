import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Main {
    static double numeric(String value) {
        double v = Double.parseDouble(value);
        if (!Double.isFinite(v)) throw new IllegalArgumentException("finite number required");
        return v;
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    static List<String[]> input(int columns) throws IOException {
        var rows = new ArrayList<String[]>();
        var reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            require(rows.size() < 100000, "input limit exceeded");
            var row = line.split(",", -1);
            require(row.length == columns && Arrays.stream(row).noneMatch(String::isBlank), "invalid CSV row");
            rows.add(row);
        }
        require(!rows.isEmpty(), "missing input");
        return rows;
    }
    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--self-test")) { test(); System.out.println("tests passed"); }
            else run();
        } catch (Exception e) { System.err.println(e.getMessage()); System.exit(2); }
    }

    record Alarm(double time, String tag, String parent) {}
    record Cluster(Alarm root, List<Alarm> members) {}
    static List<Cluster> correlate(List<Alarm> alarms, double window) {
        var clusters = new ArrayList<Cluster>(); double last = -1;
        for (var a : alarms) {
            require(a.time >= 0 && a.time >= last, "alarms must be chronological"); last = a.time;
            Cluster match = null;
            for (int i = clusters.size() - 1; i >= 0; i--) {
                var c = clusters.get(i); if (a.time - c.root.time > window) continue;
                if (c.members.stream().anyMatch(p -> p.tag.equals(a.parent))) { match = c; break; }
            }
            if (match == null) { var members = new ArrayList<Alarm>(); members.add(a); clusters.add(new Cluster(a, members)); }
            else match.members.add(a);
        }
        return clusters;
    }
    static void test() {
        var clusters = correlate(List.of(new Alarm(0, "power", "-"), new Alarm(1, "pump", "power"), new Alarm(2, "flow", "pump"), new Alarm(20, "flow", "pump")), 5);
        require(clusters.size() == 2 && clusters.get(0).members.size() == 3, "cascade mismatch");
        require(correlate(List.of(new Alarm(0, "x", "unknown")), 5).size() == 1, "orphan dropped");
    }
    static void run() throws IOException {
        var alarms = new ArrayList<Alarm>(); for (var r : input(3)) alarms.add(new Alarm(numeric(r[0]), r[1], r[2]));
        for (var c : correlate(alarms, 5)) System.out.printf(Locale.ROOT, "root=%s,time=%.3f,count=%d,members=%s%n", c.root.tag, c.root.time, c.members.size(), c.members.stream().map(Alarm::tag).collect(Collectors.joining("|")));
    }

}
