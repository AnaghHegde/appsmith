export * from "ce/entities/DataTree/utils";
import type {
  ActionEntity,
  JSActionEntity,
  ModuleInputsEntity,
  WidgetEntity,
} from "@appsmith/entities/DataTree/types";
import { ENTITY_TYPE_VALUE } from "@appsmith/entities/DataTree/types";
import { isWidgetActionOrJsObject as CE_isWidgetActionOrJsObject } from "ce/entities/DataTree/utils";
import { isModuleInput } from "@appsmith/workers/Evaluation/evaluationUtils";
import type { DataTreeEntity } from "entities/DataTree/dataTreeTypes";
import { EvaluationSubstitutionType } from "entities/DataTree/dataTreeFactory";
import { isDynamicValue } from "utils/DynamicBindingUtils";
import type { ModuleInput } from "@appsmith/constants/ModuleConstants";
//overriding this entire funtion
export const generateDataTreeModuleInputs = (
  moduleInputs: Record<string, ModuleInput>,
) => {
  const unEvalEntity: Record<string, string> = {};
  const bindingPaths: Record<string, EvaluationSubstitutionType> = {};
  const dynamicBindingPathList = [];

  for (const [key, value] of Object.entries(moduleInputs)) {
    unEvalEntity[key] = value.defaultValue;
    bindingPaths[key] = EvaluationSubstitutionType.TEMPLATE;
    if (isDynamicValue(value.defaultValue)) {
      dynamicBindingPathList.push({ key: key });
    }
  }
  return {
    unEvalEntity: {
      ...unEvalEntity,
      ENTITY_TYPE: ENTITY_TYPE_VALUE.MODULE_INPUT,
    },
    configEntity: {
      ENTITY_TYPE: ENTITY_TYPE_VALUE.MODULE_INPUT,
      bindingPaths: bindingPaths, // As all js object function referred to as action is user javascript code, we add them as binding paths.
      reactivePaths: { ...bindingPaths },
      dynamicBindingPathList: dynamicBindingPathList,
      name: "inputs",
    },
  };
};

export function isWidgetActionOrJsObject(
  entity: DataTreeEntity,
): entity is ActionEntity | WidgetEntity | JSActionEntity | ModuleInputsEntity {
  return CE_isWidgetActionOrJsObject(entity) || isModuleInput(entity);
}
