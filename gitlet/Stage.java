package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import static gitlet.Utils.*;

public class Stage implements Serializable {
    static final File STAGE_PATH = join(Repository.GITLET_DIR, "stage");
    private HashMap<String, String> toBeAdded;
    private LinkedList<String> toBeRemoved;

    public HashMap<String, String> getToBeAdded() {
        return toBeAdded;
    }

    public Stage() {
        this.toBeAdded = new HashMap<>();
        this.toBeRemoved = new LinkedList<>();
    }

    public LinkedList<String> getToBeRemoved() {
        return toBeRemoved;
    }

    public void persistStage() {
        writeObject(STAGE_PATH, this);
    }

    public static Stage getStage() {
        return readObject(STAGE_PATH, Stage.class);
    }

    public String toString() {
        String str = "toBeAdded: \n";
        for (String key : toBeAdded.keySet()) {
            str = str + "key: " + key + "; value: " + toBeAdded.get(key) + "\n";
        }
        str += "toBeRemoved: \n";
        for (String key : toBeRemoved) {
            str = str + "key: " + key + "\n";
        }
        return str;
    }

    public static void main(String[] args) {
        System.out.println(getStage());
    }
}
