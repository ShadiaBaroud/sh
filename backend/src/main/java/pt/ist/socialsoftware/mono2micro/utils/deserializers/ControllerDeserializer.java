package pt.ist.socialsoftware.mono2micro.utils.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import pt.ist.socialsoftware.mono2micro.domain.Controller;
import pt.ist.socialsoftware.mono2micro.domain.FunctionalityRedesign;
import pt.ist.socialsoftware.mono2micro.utils.Constants;
import pt.ist.socialsoftware.mono2micro.utils.ControllerType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ControllerDeserializer extends StdDeserializer<Controller> {

	public ControllerDeserializer() {
		this(null);
	}

	public ControllerDeserializer(Class<Controller> t) {
		super(t);
	}

	@Override
	public Controller deserialize(
		JsonParser jsonParser,
		DeserializationContext ctxt
	) throws IOException {
		JsonToken jsonToken = jsonParser.currentToken();

		Set<String> deserializableFields = null;

		try {
			deserializableFields = (Set<String>) ctxt.findInjectableValue(
				"controllerDeserializableFields",
				null,
				null
			);

		} catch (Exception ignored) {}

		if (jsonToken == JsonToken.START_OBJECT) {

			Controller controller = new Controller();
			while (jsonParser.nextValue() != JsonToken.END_OBJECT) {
				if (deserializableFields == null || deserializableFields.contains(jsonParser.getCurrentName())) {
					switch (jsonParser.getCurrentName()) {
						case "name":
							controller.setName(jsonParser.getValueAsString());
							break;
						case "complexity":
							controller.setComplexity(jsonParser.getFloatValue());
							break;
						case "performance":
							controller.setPerformance(jsonParser.getFloatValue());
							break;
						case "entities":
							controller.setEntities(
								jsonParser.readValueAs(
									new TypeReference<HashMap<Short, Byte>>() {}
								)
							);
							break;
						case "functionalityRedesigns":
							controller.setFunctionalityRedesigns(
									jsonParser.readValueAs(
											new TypeReference<List<FunctionalityRedesign>>() {}
									));
							break;
						case "type":
							controller.setType(ControllerType.valueOf(jsonParser.getValueAsString()));
							break;
						case "entitiesPerCluster":
							controller.setEntitiesPerCluster(
									jsonParser.readValueAs(
											new TypeReference<HashMap<Short, Set<Short>>>() {}
									)
							);
							break;

						default:
							throw new IOException("Attribute " + jsonParser.getCurrentName() + " does not exist on Controller object");
					}
				}
				else {
					jsonParser.skipChildren();
				}
			}

			return controller;
		}

		throw new IOException("Error deserializing Controller");
	}
}