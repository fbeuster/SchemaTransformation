package schemaTransformation.logs;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Felix Beuster on 19.07.2016.
 */
public class RelationCollisions {

    private LinkedHashMap<String, ArrayList<String>> collisions;

    public RelationCollisions() {
        collisions = new LinkedHashMap<>();
    }

    public void add(String original, String collision) {
        ArrayList<String> collisionList = collisions.get(original);

        if (collisionList == null) {
            collisionList = new ArrayList<>();
        }

        collisionList.add(collision);
        collisions.put(original, collisionList);
    }

    public int size() {
        return collisions.size();
    }

    @Override
    public String toString() {
        String ret = "";

        for (String name : collisions.keySet()) {
            ret += name + " collides with: ";

            for (String collision : collisions.get(name)) {
                ret += collision + "; ";
            }

            ret += "\n";
        }

        return ret;
    }
}
