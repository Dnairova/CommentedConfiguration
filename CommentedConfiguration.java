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

    private HashMap<String, List<String>> comments;
    private HashMap<String, String> singleKeyMap;
    private List<String> attach;
    private FileConfiguration config;
    public FileConfiguration getConfig() { return config; }
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

        try { newWrapper.load();
        } catch (IOException e) {
            e.printStackTrace();
            return newWrapper;
        }

        newWrapper.applyModifications(loadFromJar(name));
        try { newWrapper.save();
        } catch (IOException e) { e.printStackTrace(); }
        return newWrapper;
    }

    private static CommentedConfiguration loadFromJar(String name) {
        CommentedConfiguration newWrapper = new CommentedConfiguration();
        InputStream iStream = newWrapper.getClass().getResourceAsStream("/" + name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
        newWrapper.config = new YamlConfiguration();
        try { newWrapper.loadFromReader(name);
        } catch (IOException e) {
            e.printStackTrace();
            return newWrapper;
        }
        return newWrapper;
    }

    private void applyModifications(CommentedConfiguration fromJar) {
        HashMap<String, String> newSingleKeyMap = new HashMap<>();
        this.attach.clear();
        this.attach.addAll(fromJar.attach);
        this.comments.clear();
        this.comments.putAll(fromJar.comments);
        for (String map : fromJar.singleKeyMap.keySet()) {
            if (this.singleKeyMap.containsKey(map) && !this.singleKeyMap.get(map).equalsIgnoreCase("")) {
                newSingleKeyMap.put(map, this.singleKeyMap.get(map));
            } else {
                newSingleKeyMap.put(map, fromJar.singleKeyMap.get(map));
            }
        }
        this.singleKeyMap.clear();
        this.singleKeyMap.putAll(newSingleKeyMap);
    }

    private List<String> getUltimateValue(String key) {
        ArrayList<String> values = new ArrayList<>();
        if (this.config.get(key) instanceof MemorySection) {
            for (String innerkey : this.config.getConfigurationSection(key).getKeys(false)) {
                for (String endpoint : getUltimateValue(key + "." + innerkey)) {
                    values.add(endpoint);
                }
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
        StringBuilder builder = new StringBuilder("");
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
                    singleKeyMap.put(str, String.valueOf(config.get(str)));
                }
                comments.clear();
                this.attach.add(sc);
            }
        }
    }

    /** |
     *  | =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
     *  |  */

    public void load() throws IOException {
        try {
            this.config.load(file);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace(); return;
        }
        this.comments.clear(); this.singleKeyMap.clear(); this.attach.clear();
        BufferedReader reader;
        reader = new BufferedReader(new FileReader(file));
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
                    singleKeyMap.put(str, String.valueOf(config.get(str)));
                }
                comments.clear();
                this.attach.add(sc);
            }
        }
    }

    public void save() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        StringBuilder configBuilder = new StringBuilder("");
        for (String attach : this.attach) { // loop all points node
            if (this.comments.containsKey(attach)) {
                for (String comment : comments.get(attach)) {
                    configBuilder.append(comment).append("\n");
                }
            }
            configBuilder.append("\n");
            configBuilder.append(formatKey(attach));
        }
        writer.write(configBuilder.toString());
        writer.flush();
        writer.close();
        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    public Object get(String path) {
        return this.config.get(path);
    }

    public void set(String path, Object value) {
        this.singleKeyMap.replace(path, String.valueOf(value));
    }
}
