/**
 * @AUTHOR: BowYard
 */
package yourpackage;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommentedConfiguration {

    private final HashMap<String, List<String>> comments;
    private final HashMap<String, Object> singleKeyMap;
    private final List<String> attach;
    
    private FileConfiguration config;
    private File file;

    private CommentedConfiguration() {
        this.comments = new HashMap<>();
        this.singleKeyMap = new HashMap<>();
        this.attach = new ArrayList<>();
    }

    public static CommentedConfiguration loadFromConfig(String name, File file) {
        CommentedConfiguration newWrapper = new CommentedConfiguration();
        newWrapper.file = file;
        newWrapper.config = new YamlConfiguration();
        if (!newWrapper.load()) { return null; }
        newWrapper.applyModifications(loadFromJar(name));
        if (!newWrapper.save()) { return null; }
        return newWrapper;
    }

    private static CommentedConfiguration loadFromJar(String name) {
        CommentedConfiguration newWrapper = new CommentedConfiguration();
        newWrapper.config = new YamlConfiguration();
        try { newWrapper.loadFromReader(name);
        } catch (IOException e) {
            e.printStackTrace();
            return newWrapper;
        }
        return newWrapper;
    }

    private void applyModifications(CommentedConfiguration fromJar) {
        HashMap<String, Object> newSingleKeyMap = new HashMap<>();

        for (String str : fromJar.attach) {
            if (this.attach.contains(str)) continue;
            this.attach.add(str);
        }

        for (String keyComments: fromJar.comments.keySet()) {
            if (this.comments.containsKey(keyComments)) {
                this.comments.get(keyComments).clear();
                this.comments.get(keyComments).addAll(fromJar.comments.get(keyComments));
                continue;
            }
            this.comments.put(keyComments, fromJar.comments.get(keyComments));
        }

        for (String map : fromJar.singleKeyMap.keySet()) {
            if (this.singleKeyMap.containsKey(map) && this.singleKeyMap.get(map) != null) {
                newSingleKeyMap.put(map, this.singleKeyMap.get(map));
            } else {
                newSingleKeyMap.put(map, fromJar.singleKeyMap.get(map));
            }
        }
        this.singleKeyMap.putAll(newSingleKeyMap);
    }

    private List<String> getUltimateValue(String key) {
        ArrayList<String> values = new ArrayList<>();
        if (this.config.get(key) instanceof MemorySection) {
            for (String innerkey : this.config.getConfigurationSection(key).getKeys(false)) {
                values.addAll(getUltimateValue(key + "." + innerkey));
            }
        } else { values.add(key); }
        return values;
    }

    private HashMap<String, List<String>> formalizer(String key) {
        HashMap<String, List<String>> finalMap = new HashMap<>();
        ArrayList<String> possible = new ArrayList<>();
        for (String str : this.singleKeyMap.keySet()) {
            if (str.startsWith(key)) { possible.add(str);}
        }
        String[] splittone;
        if (key.contains(".")) {
            splittone = key.split("\\.");
        } else {
            splittone = new String[] {key};
        }
        finalMap.put(splittone[splittone.length-1], new ArrayList<>());
        for (String poss : possible) {
            String[] innerSplit = poss.split("\\."); // 0 -> key, 1 -> nextkey
            if (!finalMap.get(splittone[splittone.length-1]).contains(innerSplit[splittone.length]))
                finalMap.get(splittone[splittone.length-1]).add(innerSplit[splittone.length]);

            if (!this.singleKeyMap.containsKey(key+ "." + innerSplit[splittone.length])) {
                finalMap.putAll(formalizer(key + "." + innerSplit[splittone.length]));
            }
        }
        return finalMap;
    }

    private String formatKey(String key) {
        StringBuilder builder = new StringBuilder();
        if (this.singleKeyMap.containsKey(key)) {
            builder.append(key).append(": ").append(this.singleKeyMap.get(key)).append("\n");
            return builder.append("\n").toString();
        }
        HashMap<String, List<String>> formalizedSection = formalizer(key);
        builder.append(shifter(formalizedSection, key, 0, key));
        return builder.append("\n").toString();
    }


    private String shifter(HashMap<String, List<String>> formalized, String key, int shift, String complete) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < shift; i++) { builder.append("  "); }
        builder.append(key).append(":\n");
        for (String inner : formalized.get(key)) {
            if (this.singleKeyMap.containsKey(complete + "." + inner)) {
                for (int i = 0; i < shift+1; i++) { builder.append("  "); }
                builder.append(inner).append(": ").append(this.singleKeyMap.get(complete + "." + inner)).append("\n");
            } else {
                if (formalized.containsKey(inner)) {
                    builder.append(shifter(formalized, inner, shift+1, complete + "." + inner));
                }
            }
        }
        return builder.toString();
    }

    private void loadFromReader(String name) throws IOException {
        this.comments.clear(); this.singleKeyMap.clear(); this.attach.clear();

        InputStream iStream = this.getClass().getResourceAsStream("/" + name);
        InputStream iStreams = this.getClass().getResourceAsStream("/" + name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
        BufferedReader readers = new BufferedReader(new InputStreamReader(iStreams));
        try {
            this.config.load(readers);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace(); return;
        }

        String line;
        ArrayList<String> comments = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.matches("^(#(\\s*).+)")) { // is it a comment?
                comments.add(line);
            } else if (line.matches("^(\\w+:)") || line.matches("^(\\w+:\\s.+)")) { // is it a key?
                String sc = line.replaceAll(":((\\s.+)?)", "");

                List<String> endpoints = getUltimateValue(sc);
                if (comments.size() > 0) {
                    this.comments.put(sc, new ArrayList<>(comments));
                }
                for (String str : endpoints) {
                    singleKeyMap.put(str, config.get(str));
                }
                comments.clear();
                this.attach.add(sc);
            }
        }
    }

    public boolean load() {
        try {
            this.config.load(file);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace(); return false;
        }
        this.comments.clear(); this.singleKeyMap.clear(); this.attach.clear();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace(); return false;
        }
        String line;
        ArrayList<String> comments = new ArrayList<>();
        while (true) {
            try { if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                e.printStackTrace(); return false;
            }
            if (line.matches("^(#(\\s*).+)")) { // is it a comment?
                comments.add(line);
            } else if (line.matches("^(\\w+:)") || line.matches("^(\\w+:\\s.+)")) { // is it a key?
                String sc = line.replaceAll(":((\\s.+)?)", "");

                List<String> endpoints = getUltimateValue(sc);
                if (comments.size() > 0) {
                    this.comments.put(sc, new ArrayList<>(comments));
                }
                for (String str : endpoints) {
                    singleKeyMap.put(str, config.get(str));
                }
                comments.clear();
                this.attach.add(sc);
            }
        }
        return true;
    }

    public boolean save() {
        BufferedWriter writer;
        try { writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace(); return false;
        }
        StringBuilder configBuilder = new StringBuilder();
        for (String attach : this.attach) { // loop all points node
            if (this.comments.containsKey(attach)) {
                for (String comment : comments.get(attach)) {
                    configBuilder.append(comment).append("\n");
                }
            }
            configBuilder.append("\n");
            configBuilder.append(formatKey(attach));
        }
        try {
            writer.write(configBuilder.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
        return true;
    }

    public Object get(String path) {
        return this.config.get(path);
    }

    public void set(String path, Object value) {
        this.singleKeyMap.put(path, value);
    }
    
    public FileConfiguration getConfig() {
        return config; 
    }
}
