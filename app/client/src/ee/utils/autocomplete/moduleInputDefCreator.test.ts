import { dataTreeTypeDefCreator } from "utils/autocomplete/dataTreeTypeDefCreator";
import {
  ENTITY_TYPE_VALUE,
  EvaluationSubstitutionType,
} from "entities/DataTree/dataTreeFactory";

describe("dataTreeTypeDefCreator for module input", () => {
  it("creates the correct def for module input entity", () => {
    const evalEntity = {
      username: "Appsmith",
      email: "123@appsmith.com",
      ENTITY_TYPE: ENTITY_TYPE_VALUE.MODULE_INPUT,
    };

    const entityConfig = {
      name: "inputs",
      ENTITY_TYPE: ENTITY_TYPE_VALUE.MODULE_INPUT,
      bindingPaths: {
        username: EvaluationSubstitutionType.TEMPLATE,
        email: EvaluationSubstitutionType.TEMPLATE,
      },
      reactivePaths: {
        username: EvaluationSubstitutionType.TEMPLATE,
        email: EvaluationSubstitutionType.TEMPLATE,
      },
      dynamicBindingPathList: [{ key: "username" }, { key: "email" }],
    };
    const dataTree = {
      inputs: evalEntity,
    };
    const configTree = {
      inputs: entityConfig,
    };
    const { def, entityInfo } = dataTreeTypeDefCreator(
      dataTree,
      {},
      configTree,
    );

    expect(def).toHaveProperty("inputs.username");
    expect(def).toHaveProperty("inputs.email");
    expect(def.inputs).toEqual({ username: "string", email: "string" });
    expect(entityInfo.get("inputs")).toStrictEqual({
      type: ENTITY_TYPE_VALUE.MODULE_INPUT,
      subType: ENTITY_TYPE_VALUE.MODULE_INPUT,
    });
  });
});
