package pt.ist.socialsoftware.mono2micro.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ist.socialsoftware.mono2micro.dto.AccessDto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Utils {

    public static Integer lineno() { return new Throwable().getStackTrace()[1].getLineNumber(); }

    public static void print(String message, Integer lineNumber) { System.out.println("[" + lineNumber + "] " + message); }

    public static Set<String> getJsonFileKeys(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonfactory = mapper.getFactory();

        JsonParser jsonParser = jsonfactory.createParser(is);
        JsonToken jsonToken = jsonParser.nextValue(); // JsonToken.START_OBJECT

        if (jsonToken != JsonToken.START_OBJECT) {
            System.err.println("Json must start with a left curly brace");
            System.exit(-1);
        }

        Set<String> keys = new HashSet<>();

        jsonParser.nextValue();

        while (jsonToken != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentName() != null) {
                String keyName = jsonParser.getCurrentName();
                System.out.println("Key name: " + keyName);
                keys.add(keyName);
                jsonParser.skipChildren();
            }

            jsonToken = jsonParser.nextValue();
        }

        is.close();

        return keys;
    }

    // FIXME better name for this function pls
    public static void fillEntityDataStructures(
        Map<Short, List<Pair<String, Byte>>> entityControllers,
        Map<String, Integer> e1e2PairCount,
        List<AccessDto> accessesList,
        String controllerName
    ) {

        for (int i = 0; i < accessesList.size(); i++) {
            AccessDto access = accessesList.get(i);
            short entityID = access.getEntityID();
            byte mode = access.getMode();

            if (entityControllers.containsKey(entityID)) {
                boolean containsController = false;

                for (Pair<String, Byte> controllerPair : entityControllers.get(entityID)) {
                    if (controllerPair.getFirst().equals(controllerName)) {
                        containsController = true;

                        if (controllerPair.getSecond() != 3 && controllerPair.getSecond() != mode)
                            controllerPair.setSecond((byte) 3); // "RW" -> 3

                        break;
                    }
                }

                if (!containsController) {
                    entityControllers.get(entityID).add(
                        new Pair<>(
                            controllerName,
                            mode
                        )
                    );
                }

            } else {
                List<Pair<String, Byte>> controllersPairs = new ArrayList<>();
                controllersPairs.add(
                    new Pair<>(
                        controllerName,
                        mode
                    )
                );


                entityControllers.put(entityID, controllersPairs);
            }

            if (i < accessesList.size() - 1) {
                AccessDto nextAccess = accessesList.get(i + 1);
                short nextEntityID = nextAccess.getEntityID();

                if (entityID != nextEntityID) {
                    String e1e2 = entityID + "->" + nextEntityID;
                    String e2e1 = nextEntityID + "->" + entityID;

                    int count = e1e2PairCount.getOrDefault(e1e2, 0);
                    e1e2PairCount.put(e1e2, count + 1);

                    count = e1e2PairCount.getOrDefault(e2e1, 0);
                    e1e2PairCount.put(e2e1, count + 1);
                }
            }
        }
    }

    public static int getMaxNumberOfPairs(Map<String,Integer> e1e2PairCount) {
        if (!e1e2PairCount.values().isEmpty())
            return Collections.max(e1e2PairCount.values());
        else
            return 0;
    }

    public static float[] calculateSimilarityMatrixMetrics(
        Map<Short,List<Pair<String, Byte>>> entityControllers, // entityID -> [<controllerName, accessMode>, ...]
        Map<String,Integer> e1e2PairCount,
        short e1ID,
        short e2ID,
        int maxNumberOfPairs
    ) {

        float inCommon = 0;
        float inCommonW = 0;
        float inCommonR = 0;
        float e1ControllersW = 0;
        float e1ControllersR = 0;

        for (Pair<String, Byte> e1Controller : entityControllers.get(e1ID)) {
            for (Pair<String, Byte> e2Controller : entityControllers.get(e2ID)) {
                if (e1Controller.getFirst().equals(e2Controller.getFirst())) {
                    inCommon++;
                    // != 1 == contains("W") -> "W" or "RW"
                    if (e1Controller.getSecond() != 1 && e2Controller.getSecond() != 1)
                        inCommonW++;

                    // != 2 == contains("R") -> "R" or "RW"
                    if (e1Controller.getSecond() != 2 && e2Controller.getSecond() != 2)
                        inCommonR++;
                }
            }

            // != 1 == contains("W") -> "W" or "RW"
            if (e1Controller.getSecond() != 1)
                e1ControllersW++;

            // != 2 == contains("R") -> "R" or "RW"
            if (e1Controller.getSecond() != 2)
                e1ControllersR++;
        }

        float accessMetric = inCommon / entityControllers.get(e1ID).size();
        float writeMetric = e1ControllersW == 0 ? 0 : inCommonW / e1ControllersW;
        float readMetric = e1ControllersR == 0 ? 0 : inCommonR / e1ControllersR;

        String e1e2 = e1ID + "->" + e2ID;
        float e1e2Count = e1e2PairCount.getOrDefault(e1e2, 0);

        float sequenceMetric;

        if (maxNumberOfPairs != 0)
            sequenceMetric = e1e2Count / maxNumberOfPairs;
        else // nao ha controladores a aceder a mais do que uma entidade
            sequenceMetric = 0;

        return new float[] {
            accessMetric,
            writeMetric,
            readMetric,
            sequenceMetric
        };
    }
}
