import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class GunStats {
    static final Pattern TYPE = Pattern.compile("\"type\"\s*:\s*\"([^\"]+)\"");
    static final Pattern ITEM_TYPE = Pattern.compile("\"item_type\"\s*:\s*\"([^\"]+)\"");

    public static void main(String[] args) throws Exception {
        Path taczDir = Paths.get(args[0]);
        Set<String> failed = new HashSet<>();
        Path failedFile = Paths.get(args[1]);
        if (Files.exists(failedFile)) {
            for (String line : Files.readAllLines(failedFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) failed.add(line.trim());
            }
        }

        Map<String, String> idToPack = new TreeMap<>();
        Map<String, String> idToType = new TreeMap<>();
        Map<String, int[]> packCounts = new TreeMap<>(); // [total, failed]

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(taczDir, "*.zip")) {
            for (Path zipPath : ds) {
                String pack = zipPath.getFileName().toString();
                try (ZipFile zf = new ZipFile(zipPath.toFile())) {
                    Enumeration<? extends ZipEntry> en = zf.entries();
                    while (en.hasMoreElements()) {
                        ZipEntry e = en.nextElement();
                        String n = e.getName();
                        if (!n.startsWith("data/") || !n.contains("/index/guns/") || !n.endsWith(".json")) continue;
                        String[] p = n.split("/");
                        if (p.length < 5 || !p[2].equals("index") || !p[3].equals("guns")) continue;
                        String id = p[1] + ":" + p[4].substring(0, p[4].length() - 5);
                        String json = new String(zf.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8);
                        add(id, pack, json, failed, idToPack, idToType, packCounts);
                    }
                }
            }
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(taczDir)) {
            for (Path d : ds) {
                if (!Files.isDirectory(d)) continue;
                Path data = d.resolve("data");
                if (!Files.exists(data)) continue;
                String pack = d.getFileName().toString();
                try (var stream = Files.walk(data)) {
                    for (Path f : (Iterable<Path>) stream::iterator) {
                        String n = data.relativize(f).toString().replace(File.separatorChar, '/');
                        String[] p = n.split("/");
                        if (p.length < 4 || !p[1].equals("index") || !p[2].equals("guns") || !n.endsWith(".json")) continue;
                        String id = p[0] + ":" + p[3].substring(0, p[3].length() - 5);
                        add(id, pack, Files.readString(f, StandardCharsets.UTF_8), failed, idToPack, idToType, packCounts);
                    }
                }
            }
        }

        int total = idToPack.size();
        int loaded = 0;
        for (String id : idToPack.keySet()) if (!failed.contains(id)) loaded++;
        System.out.println("有效枪械索引(唯一): " + total);
        System.out.println("加载失败(缺 data): " + failed.stream().filter(idToPack::containsKey).count());
        System.out.println("实际可用枪械: " + loaded);
        System.out.println();
        System.out.println("==== 按枪包 ====");
        packCounts.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
                .forEach(e -> System.out.printf("%-55s 定义 %3d  可用 %3d%n", e.getKey(), e.getValue()[0], e.getValue()[0] - e.getValue()[1]));
        System.out.println();
        System.out.println("==== 按类型 ====");
        Map<String, Integer> typeCounts = new TreeMap<>();
        Map<String, Integer> typeLoaded = new TreeMap<>();
        for (String id : idToPack.keySet()) {
            String t = idToType.getOrDefault(id, "unknown");
            typeCounts.merge(t, 1, Integer::sum);
            if (!failed.contains(id)) typeLoaded.merge(t, 1, Integer::sum);
        }
        typeCounts.forEach((t, c) -> System.out.printf("%-16s %3d (可用 %3d)%n", t, c, typeLoaded.getOrDefault(t, 0)));
    }

    static void add(String id, String pack, String json, Set<String> failed, Map<String, String> idToPack,
                    Map<String, String> idToType, Map<String, int[]> packCounts) {
        idToPack.put(id, pack);
        Matcher m = TYPE.matcher(json);
        idToType.put(id, m.find() ? m.group(1) : "unknown");
        int[] c = packCounts.computeIfAbsent(pack, k -> new int[2]);
        c[0]++;
        if (failed.contains(id)) c[1]++;
    }
}
